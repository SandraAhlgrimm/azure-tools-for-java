/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.appmod;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ActionGroup for "Migrate to Azure" functionality.
 * Only shown when App Modernization plugin IS installed.
 * Shows migration options as sub-menu from extension providers.
 * 
 * Mutually exclusive with MigrateToAzureInstallAction (AnAction) which is shown when plugin is NOT installed.
 */
public class MigrateToAzureAction extends ActionGroup {
    private static final ExtensionPointName<IMigrateOptionProvider> migrationProviders =
        ExtensionPointName.create("com.microsoft.tooling.msservices.intellij.azure.migrateOptionProvider");
    
    public MigrateToAzureAction() {
        super("Migrate to Azure", true);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        super.update(e);
        final Project project = e.getProject();
        
        // Only visible when plugin IS installed (MigrateToAzureInstallAction handles uninstalled case)
        if (!MigratePluginInstaller.isAppModPluginInstalled()) {
            e.getPresentation().setEnabledAndVisible(false);
            return;
        }
        
        e.getPresentation().setText("Migrate to Azure");
        e.getPresentation().setEnabledAndVisible(project != null);
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

        // Load migration options from extension points
        final List<MigrateNodeData> migrationNodes = loadMigrationNodes(project);
        
        if (migrationNodes.isEmpty()) {
            return new AnAction[]{ createNoOptionsAction() };
        }

        // Convert nodes to actions
        return convertNodesToActions(migrationNodes);
    }
    
    /**
     * Creates a disabled action shown when no migration options are available.
     */
    private AnAction createNoOptionsAction() {
        return new AnAction("No migration options available") {
            {
                getTemplatePresentation().setEnabled(false);
            }
            
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                // No-op
            }
        };
    }

    /**
     * Loads migration nodes from extension point providers.
     */
    private List<MigrateNodeData> loadMigrationNodes(@Nonnull Project project) {
        return migrationProviders.getExtensionList().stream()
            .filter(provider -> provider.isApplicable(project))
            .sorted(Comparator.comparingInt(IMigrateOptionProvider::getPriority))
            .flatMap(provider -> provider.createNodeData(project).stream())
            .filter(MigrateNodeData::isVisible)
            .collect(Collectors.toList());
    }

    /**
     * Converts node tree to action tree for sub-menu display.
     */
    private AnAction[] convertNodesToActions(List<MigrateNodeData> nodes) {
        final List<AnAction> actions = new ArrayList<>();
        for (MigrateNodeData node : nodes) {
            actions.add(convertNodeToAction(node));
        }
        return actions.toArray(new AnAction[0]);
    }

    /**
     * Converts a single node (and its children) to an action.
     */
    private AnAction convertNodeToAction(MigrateNodeData nodeData) {
        if (nodeData.hasChildren()) {
            // Node with children -> create sub-menu
            final DefaultActionGroup subgroup = new DefaultActionGroup();
            subgroup.getTemplatePresentation().setText(nodeData.getLabel(), false);
            subgroup.setPopup(true);
            subgroup.getTemplatePresentation().setIcon(AllIcons.Vcs.Changelist);

            // Handle lazy loading or static children
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
            // Leaf node -> create clickable action
            return new AnAction(nodeData.getLabel()) {
                {
                    getTemplatePresentation().setIcon(AllIcons.Vcs.Changelist);
                    if (nodeData.getDescription() != null) {
                        getTemplatePresentation().setDescription(nodeData.getDescription());
                    }
                }
                
                @Override
                public void update(@NotNull AnActionEvent e) {
                    e.getPresentation().setEnabled(nodeData.isEnabled());
                }
                
                @Override
                @AzureOperation(name = "user/appmod.trigger_migrate_option")
                public void actionPerformed(@NotNull AnActionEvent e) {
                    nodeData.click(e);
                }
            };
        }
    }
}
