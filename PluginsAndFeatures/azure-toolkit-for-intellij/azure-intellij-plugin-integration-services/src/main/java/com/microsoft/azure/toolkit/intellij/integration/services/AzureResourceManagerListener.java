/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.integration.services;

import com.intellij.execution.services.ServiceEventListener;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.util.messages.MessageBus;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;

import java.util.List;

public class AzureResourceManagerListener implements AzureResourceManager.Listener {
    @Override
    public void resourceAdded(final AbstractAzResource<?, ?, ?> resource) {
        resourceChanged();
    }

    public void resourcesAdded(List<AbstractAzResource<?, ?, ?>> resources) {
        resourceChanged();
    }

    @Override
    public void resourceRemoved(final AbstractAzResource<?, ?, ?> resource) {
        resourceChanged();
    }

    static void resourceChanged() {
//        final AzureServiceViewContributor ext = ServiceViewContributor.CONTRIBUTOR_EP_NAME.findExtension(AzureServiceViewContributor.class);
//        final Optional<NodeViewContributor> optRoot = Optional.ofNullable(ext).stream()
//            .map(r -> r.getServices(null)).flatMap(Collection::stream).findFirst();
//        final ServiceEventListener.ServiceEvent event = optRoot
//            .map(root -> ServiceEventListener.ServiceEvent.createEvent(ServiceEventListener.EventType.SERVICE_CHILDREN_CHANGED, root, AzureServiceViewContributor.class))
//            .orElseGet(() -> ServiceEventListener.ServiceEvent.createResetEvent(AzureServiceViewContributor.class));
        final MessageBus bus = ApplicationManager.getApplication().getMessageBus();
        bus.syncPublisher(ServiceEventListener.TOPIC).handle(ServiceEventListener.ServiceEvent.createResetEvent(AzureServiceViewContributor.class));
    }
}
