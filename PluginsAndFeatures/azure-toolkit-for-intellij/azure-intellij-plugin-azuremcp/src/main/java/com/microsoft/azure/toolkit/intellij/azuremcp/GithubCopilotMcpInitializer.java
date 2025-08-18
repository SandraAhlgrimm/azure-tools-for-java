package com.microsoft.azure.toolkit.intellij.azuremcp;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManagerListener;
import com.intellij.openapi.startup.ProjectActivity;
import com.intellij.openapi.util.SystemInfo;
import com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemeter;
import com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemetry;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class GithubCopilotMcpInitializer implements ProjectActivity, DumbAware, ProjectManagerListener {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .configure(JsonParser.Feature.ALLOW_COMMENTS, true)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .enable(SerializationFeature.INDENT_OUTPUT);

    private static final ComparableVersion LOWEST_SUPPORTED_COPILOT_VERSION = new ComparableVersion("1.5.50");
    private static final String GHCP_MCP_INITIALIZER = "GitHubCopilotMcpInitializer";
    private final AzureMcpPackageManager azureMcpPackageManager;

    public GithubCopilotMcpInitializer() {
        this.azureMcpPackageManager = new AzureMcpPackageManager();
    }

    @Override
    public Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
        log.info("Running GitHub Copilot MCP initializer");
        try {
            if (isGitHubCopilotPluginInstalled()) {
                initializeAzureMcpServer();
            }
        } catch (final Exception ex) {
            log.error("Error initializing Azure MCP Server: " + ex.getMessage(), ex);
            logErrorTelemetryEvent("azmcp-server-initialization-failed", ex);
        }
        return null;
    }

    private boolean isGitHubCopilotPluginInstalled() {
        // Get all installed plugins
        final IdeaPluginDescriptor[] installedPlugins = PluginManagerCore.getPlugins();
        return Arrays.stream(installedPlugins)
                .anyMatch(plugin -> {
                    final boolean copilotPluginInstalled = "com.github.copilot".equals(plugin.getPluginId().getIdString());
                    log.info("GitHub Copilot plugin installed: " + copilotPluginInstalled);
                    return copilotPluginInstalled && isMcpSupported(plugin.getVersion());
                });
    }

    private void initializeAzureMcpServer() throws Exception {
        log.info("Getting Azure MCP executable");
        final File azMcpExe = azureMcpPackageManager.getAzureMcpExecutable();
        if (azMcpExe != null) {
            configureMcpServer(azMcpExe);
            azureMcpPackageManager.cleanup();
        }
    }

    private void configureMcpServer(File azMcpExe) throws Exception {
        log.info("Initializing Azure MCP Server");
        final String mcpConfigLocation = getConfigPath().getAbsolutePath() + "/mcp.json";
        final Path mcpPath = Path.of(mcpConfigLocation);
        final McpConfig mcpConfig;
        if (mcpPath.toFile().exists()) {
            log.info("MCP configuration file already exists at: " + mcpConfigLocation);
            final String mcpContents = new String(Files.readAllBytes(mcpPath));
            mcpConfig = OBJECT_MAPPER.readValue(mcpContents, McpConfig.class);
        } else {
            Files.createDirectories(mcpPath.getParent());
            mcpConfig = new McpConfig();
        }

        Map<String, McpServer> servers = mcpConfig.getServers();
        if (servers == null) {
            servers = new HashMap<>();
            mcpConfig.setServers(servers);
        }

        final McpServer azureMcpServer = new McpServer();
        azureMcpServer.setCommand(azMcpExe.getAbsolutePath());
        azureMcpServer.setArgs(Arrays.asList("server", "start"));
        servers.put("Azure MCP Server", azureMcpServer);
        Files.writeString(mcpPath, OBJECT_MAPPER.writeValueAsString(mcpConfig));
        logTelemetryEvent("azmcp-server-initialization-success");
    }

    public boolean isMcpSupported(String version) {
        log.info("GitHub Copilot plugin version is: " + version);
        if (version == null) {
            return false;
        }
        final ComparableVersion installedVersion = new ComparableVersion(version);
        return LOWEST_SUPPORTED_COPILOT_VERSION.compareTo(installedVersion) <= 0;
    }

    /**
     * This is the same logic GitHub Copilot uses to determine the config path.
     *
     * @return the directory where the config file should be stored
     */
    private File getConfigPath() {
        final String xdgConfigHome = System.getenv("XDG_CONFIG_HOME");
        final String userHome = System.getProperty("user.home");
        final File configPath;
        if (xdgConfigHome != null && (new File(xdgConfigHome)).isAbsolute()) {
            configPath = new File(xdgConfigHome, "github-copilot");
        } else if (SystemInfo.isWindows) {
            final String localAppData = System.getenv("LOCALAPPDATA");
            if (localAppData != null) {
                configPath = new File(localAppData, "github-copilot/intellij");
            } else {
                String userProfileLocation = System.getenv("USERPROFILE");
                if (userProfileLocation == null) {
                    userProfileLocation = userHome;
                }
                configPath = new File(userProfileLocation, "AppData/Local/github-copilot/intellij");
            }
        } else {
            configPath = new File(userHome, ".config/github-copilot/intellij");
        }
        return configPath;
    }

    public static void logTelemetryEvent(String eventName) {
        Map<String, String> properties = Map.of(
                AzureTelemeter.OP_NAME, eventName,
                AzureTelemeter.OP_PARENT_ID, GHCP_MCP_INITIALIZER,
                AzureTelemeter.OPERATION_NAME, eventName, // what's the difference between OP_NAME and OPERATION_NAME?
                AzureTelemeter.SERVICE_NAME, GHCP_MCP_INITIALIZER
        );
        AzureTelemeter.log(AzureTelemetry.Type.INFO, properties);
    }

    public static void logErrorTelemetryEvent(String eventName, Exception ex) {
        Map<String, String> properties = Map.of(
                AzureTelemeter.OP_NAME, eventName,
                AzureTelemeter.OP_PARENT_ID, GHCP_MCP_INITIALIZER,
                AzureTelemeter.OPERATION_NAME, eventName, // what's the difference between OP_NAME and OPERATION_NAME?
                AzureTelemeter.SERVICE_NAME, GHCP_MCP_INITIALIZER,
                AzureTelemeter.ERROR_STACKTRACE, ExceptionUtils.getStackTrace(ex)
        );
        AzureTelemeter.log(AzureTelemetry.Type.ERROR, properties);
    }
}
