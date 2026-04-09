/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.azuremcp;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.registry.Registry;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

import static com.microsoft.azure.toolkit.intellij.azuremcp.AzureMcpUtils.logErrorTelemetryEvent;
import static com.microsoft.azure.toolkit.intellij.azuremcp.AzureMcpUtils.logTelemetryEvent;

/**
 * Post-startup activity that installs or updates Azure Skills via {@code npx}.
 *
 * <ul>
 *   <li>If skills have never been installed ({@code ~/.agents/skills/} is absent or empty),
 *       runs {@code npx -y skills add ... -g} to perform a fresh install.</li>
 *   <li>If skills are already installed and the last install/update was more than 24 hours ago,
 *       runs {@code npx -y skills update -g} to update them.</li>
 *   <li>Otherwise, skips entirely.</li>
 * </ul>
 * <p>
 * Can be disabled via the {@code azure.skills.autoconfigure.disabled} registry key.
 */
@Slf4j
public class AzureSkillsInitializer implements ProjectActivity, DumbAware {
    private static final String COPILOT_PLUGIN_ID = "com.github.copilot";
    private static final String[] NPX_ADD_ARGS = {
            "-y", "skills", "add",
            "https://github.com/microsoft/azure-skills/tree/main/.github/plugins/azure-skills/skills",
            "--all", "github-copilot", "-g"
    };
    private static final String[] NPX_UPDATE_ARGS = {
            "-y", "skills", "update", "-g"
    };
    private static final Duration UPDATE_INTERVAL = Duration.ofHours(24);
    private static final String TIMESTAMP_FILE_NAME = "azure-skills-last-update";

    // This timeout should account for time required to clone the repo and install all the skills.
    private static final long TIMEOUT_IN_MINUTES = 5;

    @Override
    public Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
        if (Registry.is("azure.skills.autoconfigure.disabled", false)) {
            logTelemetryEvent("azure-skills-initialization-disabled");
            return null;
        }

        logTelemetryEvent("azure-skills-initialization-started");
        log.info("Running Azure Skills initializer");
        try {
            if (!isCopilotPluginInstalled()) {
                log.info("GitHub Copilot plugin is not installed, skipping Azure Skills initialization");
                logTelemetryEvent("azure-skills-copilot-not-installed");
                return null;
            }

            final String npxPath = findNpxExecutable();
            if (npxPath == null) {
                log.warn("npx is not installed or not found on PATH");
                logTelemetryEvent("azure-skills-npx-not-found");
                return null;
            }

            if (isSkillsInstalled()) {
                updateSkills(npxPath);
            } else {
                installAzureSkills(npxPath);
            }
        } catch (final Exception ex) {
            log.error("Error initializing Azure Skills: " + ex.getMessage(), ex);
            logErrorTelemetryEvent("azure-skills-initialization-failed", ex);
        }
        return null;
    }

    private void installAzureSkills(String npxPath) {
        log.info("Azure Skills not found, running fresh install");
        final boolean success = runNpxCommand(npxPath, NPX_ADD_ARGS);
        if (success) {
            writeTimestamp();
            log.info("Azure Skills installed successfully.");
            logTelemetryEvent("azure-skills-install-success");
        } else {
            log.warn("Azure Skills npx add command failed");
            logTelemetryEvent("azure-skills-install-failed");
        }
    }

    private void updateSkills(String npxPath) {
        if (!isUpdateDue()) {
            log.info("Azure Skills is up to date, skipping update");
            return;
        }

        log.info("Azure Skills update is due, running update");
        final boolean success = runNpxCommand(npxPath, NPX_UPDATE_ARGS);
        if (success) {
            writeTimestamp();
            log.info("Azure Skills updated successfully.");
            logTelemetryEvent("azure-skills-update-success");
        } else {
            log.warn("Azure Skills npx update command failed");
            logTelemetryEvent("azure-skills-update-failed");
        }
    }

    /**
     * Checks whether the GitHub Copilot plugin is installed and enabled.
     */
    private boolean isCopilotPluginInstalled() {
        final IdeaPluginDescriptor copilotPlugin = PluginManagerCore.getPlugin(PluginId.getId(COPILOT_PLUGIN_ID));
        return copilotPlugin != null && copilotPlugin.isEnabled();
    }

    /**
     * Checks whether Azure Skills are already installed by looking for
     * skill subdirectories under {@code ~/.agents/skills/}.
     */
    private boolean isSkillsInstalled() {
        final Path skillsDir = getSkillsDir();
        if (!Files.isDirectory(skillsDir)) {
            return false;
        }
        try (final var entries = Files.list(skillsDir)) {
            return entries.anyMatch(Files::isDirectory);
        } catch (final IOException ex) {
            log.warn("Failed to check skills directory: " + ex.getMessage());
            return false;
        }
    }

    /**
     * Determines whether an update should be attempted based on the last
     * install/update timestamp stored in the IntelliJ config directory.
     */
    private boolean isUpdateDue() {
        final Path timestampFile = getTimestampFile();
        if (!Files.exists(timestampFile)) {
            return true;
        }
        try {
            final String content = Files.readString(timestampFile).trim();
            final Instant lastUpdate = Instant.parse(content);
            return Duration.between(lastUpdate, Instant.now()).compareTo(UPDATE_INTERVAL) > 0;
        } catch (final Exception ex) {
            log.warn("Failed to read skills timestamp file, treating as update due: " + ex.getMessage());
            return true;
        }
    }

    /**
     * Writes the current UTC timestamp to the marker file so subsequent
     * startups can decide whether an update is needed.
     */
    private void writeTimestamp() {
        try {
            final Path timestampFile = getTimestampFile();
            Files.createDirectories(timestampFile.getParent());
            Files.writeString(timestampFile, Instant.now().toString());
        } catch (final IOException ex) {
            log.warn("Failed to write skills timestamp file: " + ex.getMessage());
        }
    }

    private Path getSkillsDir() {
        return Paths.get(System.getProperty("user.home"), ".agents", "skills");
    }

    private Path getTimestampFile() {
        return Paths.get(PathManager.getConfigPath(), TIMESTAMP_FILE_NAME);
    }

    /**
     * Finds the npx executable on the system PATH.
     * On Windows, tries npx.cmd first (npm shim), then npx.
     *
     * @return the npx command string if found, null otherwise
     */
    @Nullable
    private String findNpxExecutable() {
        final String[] candidates = SystemInfo.isWindows
                ? new String[]{"npx.cmd", "npx"}
                : new String[]{"npx"};

        for (final String candidate : candidates) {
            final ProcessBuilder pb = new ProcessBuilder(candidate, "--version");
            pb.redirectErrorStream(true);
            final int exitCode = handleProcessExecution(pb);
            if (exitCode == 0) {
                return candidate;
            }
        }
        return null;
    }

    private int handleProcessExecution(ProcessBuilder pb) {
        try {
            final Process process = pb.start();
            try (final BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.debug(line);
                }
            }
            final boolean completed = process.waitFor(TIMEOUT_IN_MINUTES, TimeUnit.MINUTES);
            if (completed) {
                return process.exitValue();
            } else {
                process.destroyForcibly();
                log.warn("Process timed out and was killed: " + pb.command());
                return -1;
            }
        } catch (final IOException | InterruptedException ex) {
            log.info("Error executing process command " + pb.command() + ": " + ex.getMessage());
            return -1;
        }
    }

    /**
     * Runs an npx command with the given arguments.
     *
     * @param npxPath the path/name of the npx executable
     * @param args    the arguments to pass after npx
     * @return true if the command completed successfully
     */
    private boolean runNpxCommand(final String npxPath, final String[] args) {
        final String[] command = new String[args.length + 1];
        command[0] = npxPath;
        System.arraycopy(args, 0, command, 1, args.length);

        final ProcessBuilder pb = new ProcessBuilder(command);
        final int exitCode = handleProcessExecution(pb);
        log.info("npx skills command exited with code: " + exitCode);
        return exitCode == 0;
    }

}
