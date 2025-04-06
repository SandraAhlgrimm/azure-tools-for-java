/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.integration.services;

import com.intellij.execution.services.ServiceEventListener;
import com.intellij.execution.services.ServiceViewDescriptor;
import com.intellij.execution.services.ServiceViewLazyContributor;
import com.intellij.execution.services.ServiceViewProvidingContributor;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.util.messages.MessageBus;
import com.microsoft.azure.toolkit.ide.common.component.Node;
import com.microsoft.azure.toolkit.intellij.explorer.TypeGroupedServicesRootNode;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.stream.Collectors;

@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Getter
public class NodeViewContributor implements ServiceViewProvidingContributor<NodeViewContributor, Node<?>>, ServiceViewLazyContributor, Node.ViewRenderer, Node.ChildrenRenderer {
    @EqualsAndHashCode.Include
    private final Node<?> node;
    private final boolean service;

    public NodeViewContributor(@Nonnull Node<?> node) {
        this(node, false);
    }

    public NodeViewContributor(@Nonnull Node<?> node, boolean service) {
        this.node = node;
        this.service = service;
        this.node.setViewRenderer(this);
        this.node.setChildrenRenderer(this);
    }

    @Override
    public @Nonnull ServiceViewDescriptor getViewDescriptor(@Nonnull final Project project) {
        return new NodeViewDescriptor(project, this.node, this);
    }

    @Override
    public @Nonnull List<NodeViewContributor> getServices(@Nonnull final Project project) {
        return this.node.getChildren().stream()
            .map(n -> new NodeViewContributor(n, this.node instanceof TypeGroupedServicesRootNode))
            .collect(Collectors.toList());
    }

    @Override
    public @Nonnull ServiceViewDescriptor getServiceDescriptor(@Nonnull final Project project, @Nonnull final NodeViewContributor contributor) {
        return contributor.getViewDescriptor(project);
    }

    @Override
    public @Nonnull Node<?> asService() {
        return this.node;
    }

    @Override
    public void updateView() {
        final MessageBus bus = ApplicationManager.getApplication().getMessageBus();
        final ServiceEventListener.EventType type = ServiceEventListener.EventType.SERVICE_CHANGED;
        final ServiceEventListener.ServiceEvent event = ServiceEventListener.ServiceEvent.createEvent(type, this, AzureServiceViewContributor.class);
        bus.syncPublisher(ServiceEventListener.TOPIC).handle(event);
    }

    @Override
    public void updateChildren(final boolean... booleans) {
        final MessageBus bus = ApplicationManager.getApplication().getMessageBus();
        final ServiceEventListener.EventType type = ServiceEventListener.EventType.SERVICE_CHILDREN_CHANGED;
        final ServiceEventListener.ServiceEvent event = ServiceEventListener.ServiceEvent.createEvent(type, this, AzureServiceViewContributor.class);
        bus.syncPublisher(ServiceEventListener.TOPIC).handle(event);
    }
}
