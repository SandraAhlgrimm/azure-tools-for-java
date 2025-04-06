/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.explorer.action;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindowManager;
import com.microsoft.azure.toolkit.intellij.explorer.AzureExplorer;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.Optional;

public class AzureExplorerOpenAction extends AnAction {
    @Override
    @AzureOperation(name = "user/common.open_explorer")
    public void actionPerformed(@Nonnull AnActionEvent event) {
        final Project project = CommonDataKeys.PROJECT.getData(event.getDataContext());
        Optional.ofNullable(project)
            .map(ToolWindowManager::getInstance)
            .map(twm -> twm.getToolWindow(AzureExplorer.TOOLWINDOW_ID))
            .ifPresent(w -> w.activate(null));
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}
