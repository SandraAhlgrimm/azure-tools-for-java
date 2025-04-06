/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.lib.common.messager.ExceptionNotification;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.OperationContext;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

import static com.microsoft.azure.toolkit.lib.common.action.Action.EMPTY_PLACE;
import static com.microsoft.azure.toolkit.lib.common.action.Action.PLACE;

public class ModuleConnectorAction extends AnAction {
    @Override
    @ExceptionNotification
    @AzureOperation(name = "user/connector.connect_from_project_open_dialog")
    public void actionPerformed(@NotNull final AnActionEvent event) {
        OperationContext.current().setTelemetryProperty(PLACE, StringUtils.firstNonBlank(event.getPlace(), EMPTY_PLACE));
        final Module module = LangDataKeys.MODULE.getData(event.getDataContext());
        if (module != null) {
            connectModuleToAzureResource(module);
        }
    }

    public static void connectModuleToAzureResource(@NotNull final Module module) {
        final Project project = module.getProject();
        final ConnectorDialog dialog = new ConnectorDialog(project);
        dialog.setConsumer(new ModuleResource(module.getName()));
        dialog.show();
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        final boolean onModule = Objects.nonNull(LangDataKeys.MODULE.getData(e.getDataContext()));
        e.getPresentation().setEnabledAndVisible(onModule);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}
