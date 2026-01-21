/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.appmod;

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
 * Single action group for "Migrate to Azure" functionality.
 * - When plugins not installed: sub-menu shows "Install Plugin" option
 * - When plugins installed: sub-menu shows migration options from extension providers
 */
public class MigrateToAzureAction extends ActionGroup {
    private static final ExtensionPointName<IMigrateOptionProvider> migrationProviders =
        ExtensionPointName.create("com.microsoft.tooling.msservices.intellij.azure.migrateOptionProvider");
    
    private static final String APP_MOD_ICON_PATH = "/icons/app_mod.svg";

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
        
        if (MigratePluginInstaller.isAppModPluginInstalled()) {
            e.getPresentation().setText("Migrate to Azure");
            e.getPresentation().setEnabledAndVisible(project != null && hasMigrationOptions(project));
        } else {
            // Plugin not installed - still show menu with install option
            e.getPresentation().setText("Migrate to Azure");
            e.getPresentation().setEnabledAndVisible(true);
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

        // If plugin not installed, show install action
        if (!MigratePluginInstaller.isAppModPluginInstalled()) {
            return new AnAction[]{ createInstallAction(project) };
        }

        // Load migration options from extension points
        final List<MigrateNodeData> migrationNodes = loadMigrationNodes(project);
        
        if (migrationNodes.isEmpty()) {
            return AnAction.EMPTY_ARRAY;
        }

        // Convert nodes to actions
        return convertNodesToActions(migrationNodes);
    }
    
    /**
     * Creates the install plugin action shown when required plugins are not installed.
     */
    private AnAction createInstallAction(@Nonnull Project project) {
        final boolean copilotInstalled = MigratePluginInstaller.isCopilotInstalled();
        final String text = copilotInstalled 
            ? "Install App Modernization Plugin..." 
            : "Install Copilot & App Modernization Plugins...";
        
        return new AnAction(text) {
            {
                getTemplatePresentation().setIcon(IntelliJAzureIcons.getIcon(APP_MOD_ICON_PATH));
            }
            
            @Override
            @AzureOperation(name = "user/appmod.install_plugin")
            public void actionPerformed(@NotNull AnActionEvent e) {
                MigratePluginInstaller.showInstallConfirmation(project, 
                    () -> MigratePluginInstaller.installPlugin(project));
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
     * Checks if there are any migration options available.
     */
    private boolean hasMigrationOptions(@Nonnull Project project) {
        return migrationProviders.getExtensionList().stream()
            .anyMatch(provider -> provider.isApplicable(project));
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
            // For lazy loading nodes, create a LazyActionGroup that loads children on demand
            if (nodeData.isLazyLoading()) {
                return new LazyActionGroup(nodeData);
            }
            
            // Node with static children -> create sub-menu with children added immediately
            final DefaultActionGroup subgroup = new DefaultActionGroup();
            subgroup.getTemplatePresentation().setText(nodeData.getLabel(), false);
            subgroup.setPopup(true);
            
            // Use node's icon if available, otherwise use default app_mod icon
            if (nodeData.getIconPath() != null) {
                subgroup.getTemplatePresentation().setIcon(IntelliJAzureIcons.getIcon(nodeData.getIconPath()));
            } else {
                subgroup.getTemplatePresentation().setIcon(IntelliJAzureIcons.getIcon(APP_MOD_ICON_PATH));
            }
            
            // Add static children
            for (MigrateNodeData child : nodeData.getChildren()) {
                if (child.isVisible()) {
                    subgroup.add(convertNodeToAction(child));
                }
            }
            
            return subgroup;
        } else {
            // Leaf node -> create clickable action
            return new AnAction(nodeData.getLabel()) {
                {
                    // Use node's icon if available, otherwise use default app_mod icon
                    if (nodeData.getIconPath() != null) {
                        getTemplatePresentation().setIcon(IntelliJAzureIcons.getIcon(nodeData.getIconPath()));
                    } else {
                        getTemplatePresentation().setIcon(IntelliJAzureIcons.getIcon(APP_MOD_ICON_PATH));
                    }
                    if (nodeData.getDescription() != null) {
                        getTemplatePresentation().setDescription(nodeData.getDescription());
                    }
                }
                
                @Override
                public void update(@NotNull AnActionEvent e) {
                    e.getPresentation().setEnabled(nodeData.isEnabled());
                }
                
                @Override
                @AzureOperation(name = "user/common.migrate_to_azure.trigger_option")
                public void actionPerformed(@NotNull AnActionEvent e) {
                    nodeData.click(e);
                }
            };
        }
    }
    
    /**
     * ActionGroup that loads children lazily when the submenu is expanded.
     * This avoids blocking the UI when the parent menu is shown.
     */
    private class LazyActionGroup extends ActionGroup {
        private final MigrateNodeData nodeData;
        private volatile AnAction[] cachedChildren;
        private volatile boolean isLoading = false;
        
        // Loading placeholder action
        private final AnAction loadingAction = new AnAction("Loading...") {
            {
                getTemplatePresentation().setEnabled(false);
            }
            
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                // No-op, this is just a placeholder
            }
        };
        
        LazyActionGroup(MigrateNodeData nodeData) {
            super(nodeData.getLabel(), true);
            this.nodeData = nodeData;
            
            // Set icon
            if (nodeData.getIconPath() != null) {
                getTemplatePresentation().setIcon(IntelliJAzureIcons.getIcon(nodeData.getIconPath()));
            } else {
                getTemplatePresentation().setIcon(IntelliJAzureIcons.getIcon(APP_MOD_ICON_PATH));
            }
        }
        
        @Override
        public @NotNull ActionUpdateThread getActionUpdateThread() {
            // Load children on background thread to avoid blocking EDT
            return ActionUpdateThread.BGT;
        }
        
        @Override
        public AnAction @NotNull [] getChildren(@Nullable AnActionEvent e) {
            if (cachedChildren != null) {
                return cachedChildren;
            }
            
            if (!isLoading) {
                isLoading = true;
                // Load children lazily
                final List<MigrateNodeData> children = nodeData.getChildrenLoader().get();
                final List<AnAction> actions = new ArrayList<>();
                for (MigrateNodeData child : children) {
                    if (child.isVisible()) {
                        actions.add(convertNodeToAction(child));
                    }
                }
                cachedChildren = actions.isEmpty() 
                    ? new AnAction[]{ createNoOptionsAction() }
                    : actions.toArray(new AnAction[0]);
                return cachedChildren;
            }
            
            // Still loading, show placeholder
            return new AnAction[]{ loadingAction };
        }
        
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
    }
}
