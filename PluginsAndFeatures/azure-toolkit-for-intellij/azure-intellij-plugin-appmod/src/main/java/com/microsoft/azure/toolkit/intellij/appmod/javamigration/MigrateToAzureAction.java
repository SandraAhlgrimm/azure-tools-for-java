/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.appmod.javamigration;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.microsoft.azure.toolkit.intellij.appmod.utils.AppModPanelHelper;
import com.microsoft.azure.toolkit.intellij.appmod.utils.AppModUtils;
import com.microsoft.azure.toolkit.intellij.appmod.common.AppModPluginInstaller;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Unified ActionGroup for "Migrate to Azure" functionality.
 * Handles all three states:
 * 1. Plugin NOT installed - direct click triggers installation
 * 2. Plugin installed but no migration options - shows "Open App Mod Panel" action
 * 3. Plugin installed with migration options - sub-menu shows migration options
 * 
 * Data is loaded once on first access and cached in Project.getUserData.
 */
public class MigrateToAzureAction extends ActionGroup {
    private static final ExtensionPointName<IMigrateOptionProvider> MIGRATION_PROVIDERS =
        ExtensionPointName.create("com.microsoft.tooling.msservices.intellij.azure.migrateOptionProvider");
    private static final Key<MigrationState> STATE_KEY = Key.create("azure.migrate.action.state");
    
    private enum State { NOT_INSTALLED, NO_OPTIONS, HAS_OPTIONS }
    
    private static class MigrationState {
        final State state;
        final List<MigrateNodeData> nodes;
        
        MigrationState(State state, List<MigrateNodeData> nodes) {
            this.state = state;
            this.nodes = nodes;
        }
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
    
    /**
     * Gets or computes migration state for the project.
     * State is cached in Project.getUserData and loaded once on first access.
     */
    private MigrationState getOrComputeState(Project project) {
        MigrationState state = project.getUserData(STATE_KEY);
        if (state == null) {
            state = computeState(project);
            project.putUserData(STATE_KEY, state);
        }
        return state;
    }
    
    /**
     * Computes migration state by calling providers.
     */
    private MigrationState computeState(Project project) {
        if (!AppModPluginInstaller.isAppModPluginInstalled()) {
            return new MigrationState(State.NOT_INSTALLED, List.of());
        }
        
        final List<MigrateNodeData> nodes = MIGRATION_PROVIDERS.getExtensionList().stream()
            .filter(provider -> provider.isApplicable(project))
            .sorted(Comparator.comparingInt(IMigrateOptionProvider::getPriority))
            .flatMap(provider -> provider.createNodeData(project).stream())
            .filter(MigrateNodeData::isVisible)
            .collect(Collectors.toList());
        
        if (nodes.isEmpty()) {
            AppModUtils.logTelemetryEvent("action.no-options");
        }
        
        return new MigrationState(
            nodes.isEmpty() ? State.NO_OPTIONS : State.HAS_OPTIONS,
            nodes
        );
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        final Project project = e.getProject();
        if (project == null) {
            e.getPresentation().setEnabledAndVisible(false);
            return;
        }
        
        final MigrationState migrationState = getOrComputeState(project);
        
        // Common settings for all states
        e.getPresentation().setPopupGroup(true);
        e.getPresentation().putClientProperty(ActionUtil.ALWAYS_VISIBLE_GROUP, true);
        
        switch (migrationState.state) {
            case NOT_INSTALLED:
                final boolean copilotInstalled = AppModPluginInstaller.isCopilotInstalled();
                e.getPresentation().setText(copilotInstalled 
                    ? "Migrate to Azure (Install Github Copilot app modernization)"
                    : "Migrate to Azure (Install GitHub Copilot and app modernization)");
                e.getPresentation().setPerformGroup(true);
                e.getPresentation().putClientProperty(ActionUtil.SUPPRESS_SUBMENU, true);
                break;
            case NO_OPTIONS:
                e.getPresentation().setText("Migrate to Azure (Open GitHub Copilot app modernization)");
                e.getPresentation().setPerformGroup(true);
                e.getPresentation().putClientProperty(ActionUtil.SUPPRESS_SUBMENU, true);
                break;
            case HAS_OPTIONS:
                e.getPresentation().setText("Migrate to Azure");
                e.getPresentation().setPerformGroup(false);
                e.getPresentation().putClientProperty(ActionUtil.SUPPRESS_SUBMENU, false);
                break;
        }
        e.getPresentation().setEnabledAndVisible(true);
    }
    
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        final Project project = e.getProject();
        if (project == null) {
            return;
        }
        
        final MigrationState migrationState = getOrComputeState(project);
        
        switch (migrationState.state) {
            case NOT_INSTALLED:
                AppModUtils.logTelemetryEvent("action.click-install");
                AppModPluginInstaller.showInstallConfirmation(project,
                    () -> AppModPluginInstaller.installPlugin(project));
                break;
            case NO_OPTIONS:
                AppModPanelHelper.openAppModPanel(project, "action");
                break;
            case HAS_OPTIONS:
                // Handled by popup menu
                break;
        }
    }

    @Override
    public AnAction @NotNull [] getChildren(@Nullable AnActionEvent e) {
        if (e == null) {
            return AnAction.EMPTY_ARRAY;
        }
        
        final Project project = e.getProject();
        if (project == null) {
            return AnAction.EMPTY_ARRAY;
        }
        
        final MigrationState migrationState = getOrComputeState(project);
        
        if (migrationState.state == State.HAS_OPTIONS) {
            return migrationState.nodes.stream()
                .map(this::convertNodeToAction)
                .toArray(AnAction[]::new);
        }
        
        return AnAction.EMPTY_ARRAY;
    }
    
    private AnAction convertNodeToAction(MigrateNodeData nodeData) {
        if (nodeData.hasChildren()) {
            final DefaultActionGroup subgroup = new DefaultActionGroup(nodeData.getLabel(), true);
            subgroup.getTemplatePresentation().setIcon(AllIcons.Vcs.Changelist);

            final List<MigrateNodeData> children = nodeData.isLazyLoading() 
                ? nodeData.getChildrenLoader().get() 
                : nodeData.getChildren();
            
            for (MigrateNodeData child : children) {
                if (child.isVisible()) {
                    subgroup.add(convertNodeToAction(child));
                }
            }
            return subgroup;
        } else {
            return new AnAction(nodeData.getLabel(), nodeData.getDescription(), AllIcons.Vcs.Changelist) {
                @Override
                public void update(@NotNull AnActionEvent e) {
                    e.getPresentation().setEnabled(nodeData.isEnabled());
                }
                
                @Override
                public void actionPerformed(@NotNull AnActionEvent e) {
                    AppModUtils.logTelemetryEvent("action.click-option", Map.of("label", nodeData.getLabel()));
                    nodeData.click(e);
                }
            };
        }
    }
}
