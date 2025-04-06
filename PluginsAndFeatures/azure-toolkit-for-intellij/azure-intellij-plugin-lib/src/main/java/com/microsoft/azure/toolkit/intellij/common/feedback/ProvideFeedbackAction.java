/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.common.feedback;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.util.Key;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.OperationContext;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.Objects;

import static com.microsoft.azure.toolkit.lib.common.action.Action.EMPTY_PLACE;
import static com.microsoft.azure.toolkit.lib.common.action.Action.PLACE;

public class ProvideFeedbackAction extends AnAction implements DumbAware {
    public static final Key<String> ID = new Key<>("ProvideFeedbackAction");

    @Override
    @AzureOperation(name = "user/feedback.open_monkey_survey")
    public void actionPerformed(@Nonnull AnActionEvent event) {
        OperationContext.current().setTelemetryProperty(PLACE, StringUtils.firstNonBlank(event.getPlace(), EMPTY_PLACE));
        MonkeySurvey.openInIDE(Objects.requireNonNull(event.getProject()));
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}
