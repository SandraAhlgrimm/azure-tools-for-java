package com.microsoft.azure.toolkit.intellij.eventhubs;

import com.azure.resourcemanager.eventhubs.models.EntityStatus;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.ide.eventhubs.EventHubsActionsContributor;
import com.microsoft.azure.toolkit.intellij.common.AzureBundle;
import com.microsoft.azure.toolkit.intellij.eventhubs.view.EventHubsToolWindowManager;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.account.IAccount;
import com.microsoft.azure.toolkit.lib.account.IAzureAccount;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.eventhubs.EventHubsInstance;
import com.microsoft.azure.toolkit.lib.eventhubs.EventHubsNamespace;

import java.util.function.BiConsumer;
import java.util.function.BiPredicate;


public class IntellijEventHubsActionsContributor implements IActionsContributor {
    @Override
    public void registerHandlers(AzureActionManager am) {
        registerActiveActionHandler(am);
        registerDisabledActionHandler(am);
        registerSendDisabledActionHandler(am);
        registerSendMessageActionHandler(am);
        registerStartListeningActionHandler(am);
        registerStopListeningActionHandler(am);
        registerCopyConnectionStringActionHandler(am);
        registerGroupCreateNamespaceActionHandler(am);
        registerCopyNamespaceConnectionStringActionHandler(am);
    }

    private void registerActiveActionHandler(AzureActionManager am) {
        final BiPredicate<EventHubsInstance, AnActionEvent> condition = (r, e) -> true;
        final BiConsumer<EventHubsInstance, AnActionEvent> handler = (c, e) -> c.updateStatus(EntityStatus.ACTIVE);
        am.registerHandler(EventHubsActionsContributor.ACTIVE_INSTANCE, condition, handler);
    }

    private void registerDisabledActionHandler(AzureActionManager am) {
        final BiPredicate<EventHubsInstance, AnActionEvent> condition = (r, e) -> true;
        final BiConsumer<EventHubsInstance, AnActionEvent> handler = (c, e) -> c.updateStatus(EntityStatus.DISABLED);
        am.registerHandler(EventHubsActionsContributor.DISABLE_INSTANCE, condition, handler);
    }

    private void registerSendDisabledActionHandler(AzureActionManager am) {
        final BiPredicate<EventHubsInstance, AnActionEvent> condition = (r, e) -> true;
        final BiConsumer<EventHubsInstance, AnActionEvent> handler = (c, e) -> c.updateStatus(EntityStatus.SEND_DISABLED);
        am.registerHandler(EventHubsActionsContributor.SEND_DISABLE_INSTANCE, condition, handler);
    }

    private void registerSendMessageActionHandler(AzureActionManager am) {
        final BiPredicate<EventHubsInstance, AnActionEvent> condition = (r, e) -> true;
        final BiConsumer<EventHubsInstance, AnActionEvent> handler = (c, e) -> AzureTaskManager.getInstance()
                .runLater(() -> EventHubsToolWindowManager.getInstance().showEventHubsPanel(e.getProject(), c, false));
        am.registerHandler(EventHubsActionsContributor.SEND_MESSAGE_INSTANCE, condition, handler);
    }

    private void registerStartListeningActionHandler(AzureActionManager am) {
        final BiPredicate<EventHubsInstance, AnActionEvent> condition = (r, e) -> true;
        final BiConsumer<EventHubsInstance, AnActionEvent> handler = (c, e) -> AzureTaskManager.getInstance()
                .runLater(() -> EventHubsToolWindowManager.getInstance().showEventHubsPanel(e.getProject(), c, true));
        am.registerHandler(EventHubsActionsContributor.START_LISTENING_INSTANCE, condition, handler);
    }

    private void registerStopListeningActionHandler(AzureActionManager am) {
        final BiPredicate<EventHubsInstance, AnActionEvent> condition = (r, e) -> true;
        final BiConsumer<EventHubsInstance, AnActionEvent> handler = (c, e) -> EventHubsToolWindowManager.getInstance().stopListening(e.getProject(), c);
        am.registerHandler(EventHubsActionsContributor.STOP_LISTENING_INSTANCE, condition, handler);
    }

    private void registerCopyConnectionStringActionHandler(AzureActionManager am) {
        final BiPredicate<EventHubsInstance, AnActionEvent> condition = (r, e) -> true;
        final BiConsumer<EventHubsInstance, AnActionEvent> handler = (c, e) -> {
            final String connectionString = c.getOrCreateListenConnectionString();
            am.getAction(ResourceCommonActionsContributor.COPY_STRING).handle(connectionString, e);
            AzureMessager.getMessager().info(AzureBundle.message("azure.eventhubs.info.copyConnectionString"), null, generateConfigAction(c));
        };
        am.registerHandler(EventHubsActionsContributor.COPY_CONNECTION_STRING, condition, handler);
    }

    private void registerGroupCreateNamespaceActionHandler(AzureActionManager am) {
        final BiConsumer<Object, AnActionEvent> handler = (c, e) -> {
            final IAccount account = Azure.az(IAzureAccount.class).account();
            final String url = String.format("%s/#create/Microsoft.EventHub", account.getPortalUrl());
            am.getAction(ResourceCommonActionsContributor.OPEN_URL).handle(url, e);
        };
        am.registerHandler(EventHubsActionsContributor.GROUP_CREATE_EVENT_HUBS, (r, e) -> true, (r, e) -> handler.accept(r, (AnActionEvent) e));
    }

    private void registerCopyNamespaceConnectionStringActionHandler(AzureActionManager am) {
        final BiPredicate<EventHubsNamespace, AnActionEvent> condition = (r, e) -> true;
        final BiConsumer<EventHubsNamespace, AnActionEvent> handler = (c, e) -> {
            final String connectionString = c.getOrCreateListenConnectionString();
            am.getAction(ResourceCommonActionsContributor.COPY_STRING).handle(connectionString, e);
            AzureMessager.getMessager().info(AzureBundle.message("azure.eventhubs.info.copyConnectionString"), null, generateConfigAction(c));
        };
        am.registerHandler(EventHubsActionsContributor.COPY_CONNECTION_STRING_NAMESPACE, condition, handler);
    }

    private static Action<?> generateConfigAction(AzResource resource) {
        final String sasKeyUrl = String.format("%s/saskey", resource.getPortalUrl());
        return new Action<>(Action.Id.of("user/eventhubs.config_shared_access_key"))
                .withLabel("Configure in Azure Portal")
                .withHandler((s, e) -> AzureActionManager.getInstance().getAction(ResourceCommonActionsContributor.OPEN_URL).handle(sasKeyUrl, e));
    }

    @Override
    public int getOrder() {
        return EventHubsActionsContributor.INITIALIZE_ORDER + 1;
    }
}
