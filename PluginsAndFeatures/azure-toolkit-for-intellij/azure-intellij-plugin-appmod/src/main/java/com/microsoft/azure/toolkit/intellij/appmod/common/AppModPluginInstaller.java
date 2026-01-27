/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.appmod.common;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.updateSettings.impl.pluginsAdvertisement.PluginsAdvertiser;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.intellij.appmod.utils.AppModUtils;

import javax.annotation.Nonnull;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Utility class for managing App Modernization plugin installation.
 * This centralizes all plugin detection and installation logic to avoid code duplication
 * between MigrateToAzureNode and MigrateToAzureAction.
 */
public class AppModPluginInstaller {
    private static final String PLUGIN_ID = "com.github.copilot.appmod";
    private static final String COPILOT_PLUGIN_ID = "com.github.copilot";
    public static final String TO_INSTALL_APP_MODE_PLUGIN = " (Install Github Copilot app modernization)";
    private AppModPluginInstaller() {
        // Utility class - prevent instantiation
    }
    
    /**
     * Checks if the App Modernization plugin is installed and enabled.
     */
    public static boolean isAppModPluginInstalled() {
        try {
            final PluginId pluginId = PluginId.getId(PLUGIN_ID);
            final IdeaPluginDescriptor plugin = PluginManagerCore.getPlugin(pluginId);
            return plugin != null && plugin.isEnabled();
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Checks if GitHub Copilot plugin is installed and enabled.
     */
    public static boolean isCopilotInstalled() {
        try {
            final PluginId pluginId = PluginId.getId(COPILOT_PLUGIN_ID);
            final IdeaPluginDescriptor plugin = PluginManagerCore.getPlugin(pluginId);
            return plugin != null && plugin.isEnabled();
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Detects if running in development mode (runIde task).
     * This helps determine whether to show restart options or dev-mode instructions.
     */
    public static boolean isRunningInDevMode() {
        try {
            final PluginId azureToolkitId = PluginId.getId("com.microsoft.tooling.msservices.intellij.azure");
            final IdeaPluginDescriptor descriptor = PluginManagerCore.getPlugin(azureToolkitId);
            if (descriptor != null) {
                final String path = descriptor.getPluginPath().toString();
                return path.contains("build") || path.contains("sandbox") || path.contains("out");
            }
        } catch (Exception ex) {
            // If we can't determine, assume production mode (safer)
        }
        return false;
    }
    
    /**
     * Shows a confirmation dialog for plugin installation.
     * 
     * @param project The current project
     * @param onConfirm Callback to execute when user confirms installation
     */
    public static void showInstallConfirmation(@Nonnull Project project, @Nonnull Runnable onConfirm) {
        final boolean copilotInstalled = isCopilotInstalled();
        
        final String title = copilotInstalled 
            ? "Install Github Copilot app modernization"
            : "Install GitHub Copilot and app modernization";
        
        final String message = copilotInstalled
            ? "Install this plugin to automate migrating your apps to Azure with Copilot."
            : "To migrate to Azure, you'll need two plugins: GitHub Copilot and app modernization.";
        
        if (Messages.showOkCancelDialog(project, message, title, "Install", "Cancel", Messages.getQuestionIcon()) == Messages.OK) {
            onConfirm.run();
        }
    }
    
    /**
     * Installs the App Modernization plugin.
     * IntelliJ platform will automatically install Copilot as a dependency if AppMod declares it.
     * 
     * @param project The current project
     */
    public static void installPlugin(@Nonnull Project project) {
        final boolean appModInstalled = isAppModPluginInstalled();
        
        // If already installed, nothing to do
        if (appModInstalled) {
            AppModUtils.logTelemetryEvent("plugin.install-skipped", Map.of("reason", "already-installed"));
            return;
        }
        
        // Only pass AppMod ID - IntelliJ will automatically install Copilot as dependency
        // (AppMod's plugin.xml should declare <depends>com.github.copilot</depends>)
        final Set<PluginId> pluginsToInstall = new LinkedHashSet<>();
        pluginsToInstall.add(PluginId.getId(PLUGIN_ID));
        
        // Use PluginsAdvertiser.installAndEnable - IntelliJ handles the rest
        // The platform will show plugin selection dialog, download, install, and prompt for restart
        AzureTaskManager.getInstance().runAndWait(() -> {
            PluginsAdvertiser.installAndEnable(
                project,
                pluginsToInstall,
                true,   // showDialog
                true,   // selectAllInDialog - pre-select all plugins
                null,   // modalityState
                () -> {
                    AppModUtils.logTelemetryEvent("plugin.install-complete");
                }
            );
        });
    }
}
