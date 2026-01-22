/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.appmod;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import org.jetbrains.annotations.NotNull;

/**
 * Action shown when App Modernization plugin is NOT installed.
 * Click directly triggers plugin installation (no sub-menu).
 * 
 * Mutually exclusive with MigrateToAzureAction (ActionGroup) which is shown when plugin IS installed.
 */
public class MigrateToAzureInstallAction extends AnAction {

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        // Only visible when plugin is NOT installed
        if (MigratePluginInstaller.isAppModPluginInstalled()) {
            e.getPresentation().setEnabledAndVisible(false);
            return;
        }
        
        final boolean copilotInstalled = MigratePluginInstaller.isCopilotInstalled();
        final String text = copilotInstalled 
            ? "Migrate to Azure (Install App Modernization)" 
            : "Migrate to Azure (Install Copilot and App Modernization)";
        e.getPresentation().setText(text);
        e.getPresentation().setEnabledAndVisible(true);
    }

    @Override
    @AzureOperation(name = "user/appmod.install_plugin")
    public void actionPerformed(@NotNull AnActionEvent e) {
        final Project project = e.getProject();
        if (project == null) {
            return;
        }
        
        MigratePluginInstaller.showInstallConfirmation(project, 
            () -> MigratePluginInstaller.installPlugin(project));
    }
}
