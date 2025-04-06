/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.common.subscription;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;

@Slf4j
public class SelectSubscriptionsAction extends AnAction implements DumbAware {

    @Override
    @AzureOperation(name = "user/account.select_subscription")
    public void actionPerformed(@Nonnull AnActionEvent e) {
        selectSubscriptions(e.getProject());
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void update(AnActionEvent e) {
        try {
            final boolean isSignIn = Azure.az(AzureAccount.class).isLoggedIn();
            e.getPresentation().setEnabled(isSignIn);
        } catch (final Exception ex) {
            ex.printStackTrace();
            log.error("update", ex);
        }
    }

    public static void selectSubscriptions(Project project) {
        if (!Azure.az(AzureAccount.class).isLoggedIn()) {
            return;
        }
        final AzureTaskManager manager = AzureTaskManager.getInstance();
        manager.runLater(() -> {
            final SubscriptionsDialog dialog = new SubscriptionsDialog(project);
            dialog.select(selected -> manager.runOnPooledThread(() -> Azure.az(AzureAccount.class).account().setSelectedSubscriptions(selected)));
        });
    }
}
