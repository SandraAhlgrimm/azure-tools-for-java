/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.intellij.connector.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.microsoft.azure.toolkit.intellij.connector.ResourceConnectionExplorer;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;

public class OpenResourceConnectionExplorerAction extends AnAction {
    @Override
    @AzureOperation(name = "user/connector.open_resource_connection_explorer")
    public void actionPerformed(@Nonnull AnActionEvent anActionEvent) {
        // open azure resource connection explorer
        final Project project = anActionEvent.getProject();
        final ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(ResourceConnectionExplorer.ToolWindowFactory.ID);
        assert toolWindow != null;
        toolWindow.setAvailable(true);
        AzureTaskManager.getInstance().runLater(() -> toolWindow.activate(null));
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}
