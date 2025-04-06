/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.aad;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.auth.AzureToolkitAuthenticationException;
import com.microsoft.azure.toolkit.lib.common.messager.ExceptionNotification;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.OperationContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;

import static com.microsoft.azure.toolkit.lib.Azure.az;
import static com.microsoft.azure.toolkit.lib.common.action.Action.EMPTY_PLACE;
import static com.microsoft.azure.toolkit.lib.common.action.Action.PLACE;

/**
 * Displays UI to display the code templates for the registered Azure AD applications.
 * <p>
 * ComponentNotRegistered is suppressed, because IntelliJ isn't finding the reference in resources/META-INF.
 */
@Slf4j
@SuppressWarnings("ComponentNotRegistered")
public class ShowApplicationTemplatesAction extends AnAction {
    public ShowApplicationTemplatesAction() {
        super(MessageBundle.message("action.AzureToolkit.AD.AzureAppTemplates.text"));
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
        try {
            // throws an exception if user is not signed in
            az(AzureAccount.class).account();
        } catch (final AzureToolkitAuthenticationException ex) {
            log.debug("user is not signed in", ex);
            e.getPresentation().setEnabled(false);
        }
    }

    @Override
    @ExceptionNotification
    @AzureOperation(name = "user/aad.show_application_templates")
    public void actionPerformed(@Nonnull AnActionEvent e) {
        OperationContext.current().setTelemetryProperty(PLACE, StringUtils.firstNonBlank(e.getPlace(), EMPTY_PLACE));
        final var project = e.getProject();
        assert project != null;

        new AzureApplicationTemplateDialog(project, null).show();
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}
