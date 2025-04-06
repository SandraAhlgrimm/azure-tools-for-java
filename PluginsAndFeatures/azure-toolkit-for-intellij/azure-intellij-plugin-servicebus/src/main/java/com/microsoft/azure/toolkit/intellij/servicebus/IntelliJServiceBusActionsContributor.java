/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *  Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.servicebus;

import com.azure.resourcemanager.servicebus.models.EntityStatus;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.ide.servicebus.ServiceBusActionsContributor;
import com.microsoft.azure.toolkit.intellij.common.AzureBundle;
import com.microsoft.azure.toolkit.intellij.servicebus.view.ServiceBusToolWindowManager;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.account.IAccount;
import com.microsoft.azure.toolkit.lib.account.IAzureAccount;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.servicebus.ServiceBusNamespace;
import com.microsoft.azure.toolkit.lib.servicebus.model.ServiceBusInstance;

import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

public class IntelliJServiceBusActionsContributor implements IActionsContributor {
    @Override
    public void registerHandlers(AzureActionManager am) {
        registerActiveActionHandler(am);
        registerDisabledActionHandler(am);
        registerSendDisabledActionHandler(am);
        registerReceiveDisabledActionHandler(am);
        registerCopyConnectionStringActionHandler(am);
        registerSendMessageActionHandler(am);
        registerStartReceivingActionHandler(am);
        registerStopReceivingActionHandler(am);
        registerGroupCreateActionHandler(am);
        registerCopyNamespaceConnectionStringActionHandler(am);
    }

    private void registerActiveActionHandler(AzureActionManager am) {
        final BiPredicate<ServiceBusInstance<?, ?, ?>, AnActionEvent> condition = (r, e) -> true;
        final BiConsumer<ServiceBusInstance<?, ?, ?>, AnActionEvent> handler = (c, e) -> c.updateStatus(EntityStatus.ACTIVE);
        am.registerHandler(ServiceBusActionsContributor.ACTIVE_INSTANCE, condition, handler);
    }

    private void registerDisabledActionHandler(AzureActionManager am) {
        final BiPredicate<ServiceBusInstance<?, ?, ?>, AnActionEvent> condition = (r, e) -> true;
        final BiConsumer<ServiceBusInstance<?, ?, ?>, AnActionEvent> handler = (c, e) -> c.updateStatus(EntityStatus.DISABLED);
        am.registerHandler(ServiceBusActionsContributor.DISABLE_INSTANCE, condition, handler);
    }

    private void registerSendDisabledActionHandler(AzureActionManager am) {
        final BiPredicate<ServiceBusInstance<?, ?, ?>, AnActionEvent> condition = (r, e) -> true;
        final BiConsumer<ServiceBusInstance<?, ?, ?>, AnActionEvent> handler = (c, e) -> c.updateStatus(EntityStatus.SEND_DISABLED);
        am.registerHandler(ServiceBusActionsContributor.SEND_DISABLE_INSTANCE, condition, handler);
    }

    private void registerReceiveDisabledActionHandler(AzureActionManager am) {
        final BiPredicate<ServiceBusInstance<?, ?, ?>, AnActionEvent> condition = (r, e) -> true;
        final BiConsumer<ServiceBusInstance<?, ?, ?>, AnActionEvent> handler = (c, e) -> c.updateStatus(EntityStatus.RECEIVE_DISABLED);
        am.registerHandler(ServiceBusActionsContributor.RECEIVE_DISABLE_INSTANCE, condition, handler);
    }

    private void registerCopyConnectionStringActionHandler(AzureActionManager am) {
        final BiPredicate<ServiceBusInstance<?, ?, ?>, AnActionEvent> condition = (r, e) -> true;
        final BiConsumer<ServiceBusInstance<?, ?, ?>, AnActionEvent> handler = (c, e) -> {
            final String connectionString = c.getOrCreateListenConnectionString();
            am.getAction(ResourceCommonActionsContributor.COPY_STRING).handle(connectionString, e);
            AzureMessager.getMessager().info(AzureBundle.message("azure.servicebus.info.copyConnectionString"), null, generateConfigAction(c));
        };
        am.registerHandler(ServiceBusActionsContributor.COPY_CONNECTION_STRING, condition, handler);
    }

    private void registerSendMessageActionHandler(AzureActionManager am) {
        final BiPredicate<ServiceBusInstance<?,?,?>, AnActionEvent> condition = (r, e) -> true;
        final BiConsumer<ServiceBusInstance<?,?,?>, AnActionEvent> handler = (c, e) -> AzureTaskManager.getInstance()
                .runLater(() -> ServiceBusToolWindowManager.getInstance().showServiceBusPanel(e.getProject(), c, false));
        am.registerHandler(ServiceBusActionsContributor.SEND_MESSAGE_INSTANCE, condition, handler);
    }

    private void registerStartReceivingActionHandler(AzureActionManager am) {
        final BiPredicate<ServiceBusInstance<?,?,?>, AnActionEvent> condition = (r, e) -> true;
        final BiConsumer<ServiceBusInstance<?,?,?>, AnActionEvent> handler = (c, e) -> AzureTaskManager.getInstance()
                .runLater(() -> ServiceBusToolWindowManager.getInstance().showServiceBusPanel(e.getProject(), c, true));
        am.registerHandler(ServiceBusActionsContributor.START_RECEIVING_INSTANCE, condition, handler);
    }

    private void registerStopReceivingActionHandler(AzureActionManager am) {
        final BiPredicate<ServiceBusInstance<?,?,?>, AnActionEvent> condition = (r, e) -> true;
        final BiConsumer<ServiceBusInstance<?,?,?>, AnActionEvent> handler = (c, e) -> ServiceBusToolWindowManager.getInstance().stopListening(e.getProject(), c);
        am.registerHandler(ServiceBusActionsContributor.STOP_RECEIVING_INSTANCE, condition, handler);
    }

    private void registerGroupCreateActionHandler(AzureActionManager am) {
        final BiConsumer<Object, AnActionEvent> handler = (c, e) -> {
            final IAccount account = Azure.az(IAzureAccount.class).account();
            final String url = String.format("%s/#create/Microsoft.ServiceBus", account.getPortalUrl());
            am.getAction(ResourceCommonActionsContributor.OPEN_URL).handle(url, e);
        };
        am.registerHandler(ServiceBusActionsContributor.GROUP_CREATE_SERVICE_BUS, (r, e) -> true, (r, e) -> handler.accept(r, (AnActionEvent) e));
    }

    private void registerCopyNamespaceConnectionStringActionHandler(AzureActionManager am) {
        final BiPredicate<ServiceBusNamespace, AnActionEvent> condition = (r, e) -> true;
        final BiConsumer<ServiceBusNamespace, AnActionEvent> handler = (c, e) -> {
            final String connectionString = c.getOrCreateConnectionString();
            am.getAction(ResourceCommonActionsContributor.COPY_STRING).handle(connectionString, e);
            AzureMessager.getMessager().info(AzureBundle.message("azure.servicebus.info.copyConnectionString"), null, generateConfigAction(c));
        };
        am.registerHandler(ServiceBusActionsContributor.COPY_NAMESPACE_CONNECTION_STRING, condition, handler);
    }

    private static Action<?> generateConfigAction(AzResource resource) {
        final String sasKeyUrl = String.format("%s/saskey", resource.getPortalUrl());
        return new Action<>(Action.Id.of("user/servicebus.config_shared_access_key"))
                .withLabel("Configure in Azure Portal")
                .withHandler((s, e) -> AzureActionManager.getInstance().getAction(ResourceCommonActionsContributor.OPEN_URL).handle(sasKeyUrl, e));
    }

    @Override
    public int getOrder() {
        return ServiceBusActionsContributor.INITIALIZE_ORDER;
    }
}
