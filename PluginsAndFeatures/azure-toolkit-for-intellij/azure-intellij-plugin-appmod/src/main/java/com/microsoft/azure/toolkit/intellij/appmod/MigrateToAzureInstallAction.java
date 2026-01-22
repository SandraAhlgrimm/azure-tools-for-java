/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.appmod;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Action for "Migrate to Azure" when:
 * 1. App Modernization plugin is NOT installed - click triggers installation
 * 2. Plugin IS installed but no migration options available - click opens App Mod Panel
 * 
 * Mutually exclusive with MigrateToAzureAction (ActionGroup) which is shown when plugin IS installed AND has migration options.
 */
public class MigrateToAzureInstallAction extends AnAction {
    private static final ExtensionPointName<IMigrateOptionProvider> migrationProviders =
        ExtensionPointName.create("com.microsoft.tooling.msservices.intellij.azure.migrateOptionProvider");
    
    private enum State {
        NOT_INSTALLED,
        NO_OPTIONS,
        HAS_OPTIONS
    }
    
    private State currentState = State.NOT_INSTALLED;

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        final Project project = e.getProject();
        if (project == null) {
            e.getPresentation().setEnabledAndVisible(false);
            return;
        }
        
        currentState = determineState(project);
        
        switch (currentState) {
            case NOT_INSTALLED:
                final boolean copilotInstalled = MigratePluginInstaller.isCopilotInstalled();
                final String text = copilotInstalled 
                    ? "Migrate to Azure (Install App modernizationn)" 
                    : "Migrate to Azure (Install GitHub Copilot and app modernization)";
                e.getPresentation().setText(text);
                e.getPresentation().setEnabledAndVisible(true);
                break;
            case NO_OPTIONS:
                e.getPresentation().setText("Migrate to Azure (Open GitHub Copilot app modernization)");
                e.getPresentation().setEnabledAndVisible(true);
                break;
            case HAS_OPTIONS:
                // Hide - MigrateToAzureAction (ActionGroup) will show instead
                e.getPresentation().setEnabledAndVisible(false);
                break;
        }
    }
    
    private State determineState(Project project) {
        if (!MigratePluginInstaller.isAppModPluginInstalled()) {
            return State.NOT_INSTALLED;
        }
        
        final boolean hasOptions = migrationProviders.getExtensionList().stream()
            .filter(provider -> provider.isApplicable(project))
            .flatMap(provider -> provider.createNodeData(project).stream())
            .anyMatch(MigrateNodeData::isVisible);
        
        return hasOptions ? State.HAS_OPTIONS : State.NO_OPTIONS;
    }

    @Override
    @AzureOperation(name = "user/appmod.migrate_action")
    public void actionPerformed(@NotNull AnActionEvent e) {
        final Project project = e.getProject();
        if (project == null) {
            return;
        }
        
        switch (currentState) {
            case NOT_INSTALLED:
                MigratePluginInstaller.showInstallConfirmation(project, 
                    () -> MigratePluginInstaller.installPlugin(project));
                break;
            case NO_OPTIONS:
                AppModPanelHelper.openAppModPanel(project);
                break;
            case HAS_OPTIONS:
                // Should not happen - action is hidden in this state
                break;
        }
    }
}
