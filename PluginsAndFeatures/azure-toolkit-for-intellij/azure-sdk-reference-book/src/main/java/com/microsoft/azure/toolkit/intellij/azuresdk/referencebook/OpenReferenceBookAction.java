/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.azuresdk.referencebook;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.lib.common.messager.ExceptionNotification;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.OperationContext;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;

import static com.microsoft.azure.toolkit.lib.common.action.Action.EMPTY_PLACE;
import static com.microsoft.azure.toolkit.lib.common.action.Action.PLACE;

public class OpenReferenceBookAction extends AnAction implements DumbAware {
    public static final String ID = "user/sdk.OpenSdkReferenceBook";

    @Override
    @ExceptionNotification
    @AzureOperation(name = "user/sdk.open_reference_book")
    public void actionPerformed(@Nonnull final AnActionEvent event) {
        OperationContext.current().setTelemetryProperty(PLACE, StringUtils.firstNonBlank(event.getPlace(), EMPTY_PLACE));
        final Module module = event.getData(LangDataKeys.MODULE);
        openSdkReferenceBook(event.getProject(), null);
    }

    public static void openSdkReferenceBook(final @Nullable Project project, @Nullable final String feature) {
        AzureTaskManager.getInstance().runLater(() -> {
            final AzureSdkReferenceBookDialog dialog = new AzureSdkReferenceBookDialog(project);
            if (StringUtils.isNotEmpty(feature)) {
                dialog.selectFeature(feature);
            }
            dialog.show();
        });
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}
