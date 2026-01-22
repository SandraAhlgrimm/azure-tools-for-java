/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.projectexplorer;

import com.intellij.icons.AllIcons;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import com.intellij.ui.tree.LeafState;
import com.microsoft.azure.toolkit.intellij.appmod.AppModPanelHelper;
import com.microsoft.azure.toolkit.intellij.appmod.Constants;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.AzureModule;
import com.microsoft.azure.toolkit.intellij.appmod.IMigrateOptionProvider;
import com.microsoft.azure.toolkit.intellij.appmod.MigrateNodeData;
import com.microsoft.azure.toolkit.intellij.appmod.MigratePluginInstaller;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Project Explorer facet node for "Migrate to Azure" functionality.
 * Uses the same extension point as MigrateToAzureNode and MigrateToAzureAction for consistency.
 */
public class MigrateToAzureFacetNode extends AbstractAzureFacetNode<AzureModule> {
    private static final ExtensionPointName<IMigrateOptionProvider> migrationProviders =
        ExtensionPointName.create("com.microsoft.tooling.msservices.intellij.azure.migrateOptionProvider");
    
    public MigrateToAzureFacetNode(Project project, AzureModule module) {
        super(project, module);
        initializeNode();
    }

    public void initializeNode() {
        updateChildren();
    }

    @Override
    public Collection<? extends AbstractAzureFacetNode<?>> buildChildren() {
        final ArrayList<AbstractAzureFacetNode<?>> nodes = new ArrayList<>();
        
        if (MigratePluginInstaller.isAppModPluginInstalled()) {
            // Load migration options from extension points
            final List<MigrateNodeData> migrationNodes = loadMigrationNodes();
            
            if (migrationNodes.isEmpty()) {
                // No migration options - add prompt to open App Modernization Panel
                nodes.add(new OpenPanelNode(getProject()));
            } else {
                // Convert MigrateNodeData to FacetNode
                for (MigrateNodeData nodeData : migrationNodes) {
                    if (nodeData.isVisible()) {
                        nodes.add(new MigrationNodeWrapper(getProject(), nodeData));
                    }
                }
            }
        }
        // When plugin not installed, return empty list - user must double-click to trigger install
        
        return nodes;
    }

    /**
     * Loads migration nodes from extension point providers.
     */
    private List<MigrateNodeData> loadMigrationNodes() {
        return migrationProviders.getExtensionList().stream()
            .filter(provider -> provider.isApplicable(getProject()))
            .sorted(Comparator.comparingInt(IMigrateOptionProvider::getPriority))
            .flatMap(provider -> provider.createNodeData(getProject()).stream())
            .collect(Collectors.toList());
    }

    @Override
    protected void buildView(@Nonnull PresentationData presentation) {
        if (MigratePluginInstaller.isAppModPluginInstalled()) {
            presentation.addText("Migrate to Azure", com.intellij.ui.SimpleTextAttributes.REGULAR_ATTRIBUTES);
            presentation.setIcon(IntelliJAzureIcons.getIcon(Constants.ICON_APPMOD_PATH));
        } else {
            final boolean copilotInstalled = MigratePluginInstaller.isCopilotInstalled();
            final String text = copilotInstalled 
                ? "Migrate to Azure (Install App modernization)"
                : "Migrate to Azure (Install GitHub Copilot and app modernization)";
            presentation.addText(text, com.intellij.ui.SimpleTextAttributes.REGULAR_ATTRIBUTES);
            presentation.setIcon(IntelliJAzureIcons.getIcon(Constants.ICON_APPMOD_PATH));
        }
    }

    @Override
    public void navigate(boolean requestFocus) {
        // When plugin not installed, trigger install on double-click
        if (!MigratePluginInstaller.isAppModPluginInstalled()) {
            MigratePluginInstaller.showInstallConfirmation(getProject(), 
                () -> MigratePluginInstaller.installPlugin(getProject()));
        }
    }

    @Override
    public boolean canNavigate() {
        // Enable navigation (double-click) when plugin is not installed
        return !MigratePluginInstaller.isAppModPluginInstalled();
    }

    @Override
    public @Nonnull LeafState getLeafState() {
        // When plugin not installed, show as leaf node (no expand arrow, double-click triggers navigate)
        // When installed, show expand arrow to reveal children
        return MigratePluginInstaller.isAppModPluginInstalled() ? LeafState.NEVER : LeafState.ALWAYS;
    }

    /**
     * Node that opens the App Modernization Panel when no migration options are available.
     */
    private static class OpenPanelNode extends AbstractAzureFacetNode<String> {
        protected OpenPanelNode(Project project) {
            super(project, "Get Started with App Modernization");
        }

        @Override
        public Collection<? extends AbstractAzureFacetNode<?>> buildChildren() {
            return List.of();
        }

        @Override
        protected void buildView(@Nonnull PresentationData presentation) {
            presentation.addText("Get Started with App Modernization", com.intellij.ui.SimpleTextAttributes.REGULAR_ATTRIBUTES);
            presentation.setIcon(AllIcons.Toolwindows.ToolWindowProject);
        }

        @Override
        public void navigate(boolean requestFocus) {
            AppModPanelHelper.openAppModPanel(getProject());
        }

        @Override
        public boolean canNavigate() {
            return true;
        }

        @Override
        public @Nonnull LeafState getLeafState() {
            return LeafState.ALWAYS;
        }
    }

    /**
     * Wrapper class that converts MigrateNodeData to AbstractAzureFacetNode for Project Explorer display.
     */
    private static class MigrationNodeWrapper extends AbstractAzureFacetNode<MigrateNodeData> {
        private final MigrateNodeData nodeData;

        protected MigrationNodeWrapper(Project project, MigrateNodeData nodeData) {
            super(project, nodeData);
            this.nodeData = nodeData;
        }

        @Override
        public Collection<? extends AbstractAzureFacetNode<?>> buildChildren() {
            final ArrayList<AbstractAzureFacetNode<?>> children = new ArrayList<>();
            
            // Get children - lazy or static (Project Explorer's buildChildren is already lazy)
            final List<MigrateNodeData> childDataList = nodeData.isLazyLoading()
                ? nodeData.getChildrenLoader().get()
                : nodeData.getChildren();
            
            for (MigrateNodeData child : childDataList) {
                if (child.isVisible()) {
                    children.add(new MigrationNodeWrapper(getProject(), child));
                }
            }
            
            return children;
        }

        @Override
        protected void buildView(@Nonnull PresentationData presentation) {
            presentation.addText(nodeData.getLabel(), com.intellij.ui.SimpleTextAttributes.REGULAR_ATTRIBUTES);
            
            // Set tooltip if available
            if (nodeData.getTooltip() != null) {
                presentation.setTooltip(nodeData.getTooltip());
            }
            
            // Use node's icon if available, otherwise use default app_mod icon
            presentation.setIcon(AllIcons.Vcs.Changelist);
        }

        @Override
        public void navigate(boolean requestFocus) {
            // Trigger click handler
            nodeData.doubleClick(null);
        }

        @Override
        public boolean canNavigate() {
            // Enable navigation for leaf nodes OR nodes with click handlers
            return !nodeData.hasChildren() || nodeData.hasClickHandler();
        }

        @Override
        public boolean canNavigateToSource() {
            // Enable source navigation only for leaf nodes
            return !nodeData.hasChildren();
        }

        @Override
        public @Nonnull LeafState getLeafState() {
            // ALWAYS = leaf node (no expand arrow, double-click triggers navigate)
            // NEVER = always show expand arrow
            return nodeData.hasChildren() ? LeafState.NEVER : LeafState.ALWAYS;
        }
    }
}
