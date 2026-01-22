/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.appmod;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.ide.common.component.Node;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcon;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service Explorer node for "Migrate to Azure" functionality.
 * This node extends the azure-toolkit-ide-common-lib Node class to integrate with the Service Explorer tree.
 */
public final class MigrateToAzureNode extends Node<String> {
    private static final ExtensionPointName<IMigrateOptionProvider> childProviders =
        ExtensionPointName.create("com.microsoft.tooling.msservices.intellij.azure.migrateOptionProvider");
    
    private final Project project;

    private static final AzureIcon APP_MOD_ICON = AzureIcon.builder().iconPath(Constants.ICON_APPMOD_PATH).build();
    private static final AzureIcon CHANGELIST_ICON = AzureIcon.builder().iconPath("/icons/changelist").build();
    private static final AzureIcon TOOLWINDOW_ICON = AzureIcon.builder().iconPath("/icons/toolWindowProject").build();

    public MigrateToAzureNode(Project project) {
        super("Migrate to Azure");
        this.project = project;
        withIcon(APP_MOD_ICON);
        initializeNode();
    }

    public void initializeNode() {
        if (MigratePluginInstaller.isAppModPluginInstalled()) {
            showMigrationOptions();
        } else {
            showNotInstalled();
        }
    }

    private void showNotInstalled() {
        final boolean copilotInstalled = MigratePluginInstaller.isCopilotInstalled();
        
        // Dynamic description based on what needs to be installed
        final String description = copilotInstalled 
            ? "Install App modernizationn"
            : "Install GitHub Copilot and app modernization";
        withDescription(description);
        
        onClicked(e -> {
            MigratePluginInstaller.showInstallConfirmation(project, () -> MigratePluginInstaller.installPlugin(project));
        });
    }


    public void showMigrationOptions() {
        clearClickHandlers();
        withDescription("");
        
        // Load migration options from extension points and convert to Node
        final List<MigrateNodeData> nodeDataList = childProviders.getExtensionList().stream()
            .filter(provider -> provider.isApplicable(project))
            .sorted(Comparator.comparingInt(IMigrateOptionProvider::getPriority))
            .flatMap(provider -> provider.createNodeData(project).stream())
            .collect(Collectors.toList());
        
        if (nodeDataList.isEmpty()) {
            // No migration options - add prompt to open App Modernization Panel
            addChild(createOpenPanelNode());
        } else {
            // Convert MigrateNodeData to Node and add as children
            nodeDataList.stream()
                .map(this::convertToNode)
                .forEach(this::addChild);
        }
    }
    
    /**
     * Creates a node that opens the App Modernization Panel.
     */
    private Node<?> createOpenPanelNode() {
        Node<String> node = new Node<>("Get Started with App Modernization");
        node.withIcon(TOOLWINDOW_ICON);
        node.onClicked(data -> AppModPanelHelper.openAppModPanel(project));
        return node;
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
