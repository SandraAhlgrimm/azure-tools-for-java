/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.appmod;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.ide.common.component.Node;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcon;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.ActionGroup;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service Explorer node for "Migrate to Azure" functionality.
 * This node extends the azure-toolkit-ide-common-lib Node class to integrate with the Service Explorer tree.
 * 
 * State is computed on initialization and can be refreshed via refresh() method.
 */
public final class MigrateToAzureNode extends Node<String> {
    private static final ExtensionPointName<IMigrateOptionProvider> childProviders =
        ExtensionPointName.create("com.microsoft.tooling.msservices.intellij.azure.migrateOptionProvider");
    
    private final Project project;

    private static final AzureIcon APP_MOD_ICON = AzureIcon.builder().iconPath(Constants.ICON_APPMOD_PATH).build();
    private static final AzureIcon CHANGELIST_ICON = AzureIcon.builder().iconPath("/icons/changelist").build();

    public MigrateToAzureNode(Project project) {
        super("Migrate to Azure");
        this.project = project;
        withIcon(APP_MOD_ICON);
        
        // Add refresh action to context menu (only once in constructor)
        withActions(new ActionGroup(
            new Action<>(Action.Id.<String>of("user/appmod.refresh_migrate_node"))
                .withLabel("Refresh")
                .withIcon(AzureIcons.Action.REFRESH.getIconPath())
                .withHandler((v, e) -> this.refresh())
                .withAuthRequired(false)
        ));
        
        // Use addChildren with a function so it rebuilds on refresh
        addChildren(data -> buildChildNodes());
        
        initializeNode();
    }

    public void initializeNode() {
        // Clear previous state
        clearClickHandlers();
        withDescription("");
        
        if (!MigratePluginInstaller.isAppModPluginInstalled()) {
            showNotInstalled();
        }
        // Don't call showMigrationOptions() here - let buildChildNodes() handle it
        // This avoids double loading of extension point data
    }
    
    /**
     * Refreshes the node by re-computing migration options.
     * Called by RefreshMigrateToAzureAction from context menu.
     */
    public void refresh() {
        refreshChildren();  // This rebuilds children from addChildren function
    }

    public Project getProject() {
        return project;
    }

    private void showNotInstalled() {
        final boolean copilotInstalled = MigratePluginInstaller.isCopilotInstalled();
        
        // Dynamic description based on what needs to be installed
        final String description = copilotInstalled 
            ? "Install App modernization"
            : "Install GitHub Copilot and app modernization";
        withDescription(description);
        
        onClicked(e -> {
            MigratePluginInstaller.showInstallConfirmation(project, () -> MigratePluginInstaller.installPlugin(project));
        });
    }
    
    /**
     * Load migration options from extension points.
     */
    private List<MigrateNodeData> loadMigrationNodeData() {
        return childProviders.getExtensionList().stream()
            .filter(provider -> provider.isApplicable(project))
            .sorted(Comparator.comparingInt(IMigrateOptionProvider::getPriority))
            .flatMap(provider -> provider.createNodeData(project).stream())
            .filter(MigrateNodeData::isVisible)
            .collect(Collectors.toList());
    }
    
    /**
     * Build child nodes - called by Node framework on refresh.
     * Also updates description and click handler based on data.
     */
    private List<Node<?>> buildChildNodes() {
        if (!MigratePluginInstaller.isAppModPluginInstalled()) {
            return List.of();
        }
        
        final List<MigrateNodeData> nodeDataList = loadMigrationNodeData();
        
        // Update description and click handler based on data
        clearClickHandlers();
        if (nodeDataList.isEmpty()) {
            withDescription("Open GitHub Copilot app modernization");
            onClicked(e -> AppModPanelHelper.openAppModPanel(project));
        } else {
            withDescription("");
        }
        
        return nodeDataList.stream()
            .map(this::convertToNode)
            .collect(Collectors.toList());
    }
    
    /**
     * Converts MigrateNodeData to Node for Service Explorer compatibility.
     */
    private Node<?> convertToNode(MigrateNodeData data) {
        Node<MigrateNodeData> node = new Node<>(data);
        
        // Set basic properties
        node.withLabel(d -> d.getLabel());
        // Use Changelist icon for child nodes
        node.withIcon(CHANGELIST_ICON);
        if (data.getTooltip() != null) {
            node.withTips(d -> d.getTooltip());
        }
        
        // Set click handler
        if (data.hasClickHandler()) {
            node.onClicked(d -> data.click(null));
        }
        
        // Handle children - lazy or static
        if (data.isLazyLoading()) {
            // Lazy loading: use Node's native lazy loading mechanism
            node.withChildrenLoadLazily(true);
            node.addChildren(
                d -> d.getChildrenLoader().get(),
                (childData, parent) -> convertToNode(childData)
            );
        } else if (data.hasChildren()) {
            // Static children: add directly
            for (MigrateNodeData childData : data.getChildren()) {
                node.addChild(convertToNode(childData));
            }
        }
        
        return node;
    }

    @Override
    public synchronized void refreshView() {
        super.refreshView();
        refreshChildrenLater(false);
    }

    public static boolean isPluginInstalled() {
        return MigratePluginInstaller.isAppModPluginInstalled();
    }
}
