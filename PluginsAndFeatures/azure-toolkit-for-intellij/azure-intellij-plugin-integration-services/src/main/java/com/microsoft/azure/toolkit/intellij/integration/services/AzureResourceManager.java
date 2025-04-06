package com.microsoft.azure.toolkit.intellij.integration.services;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.messages.Topic;
import com.intellij.util.xmlb.annotations.Property;
import com.intellij.util.xmlb.annotations.XCollection;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@State(
    name = "ServiceViewAzureResources",
    storages = {@Storage("service.azure-resource.xml")}
)
public class AzureResourceManager implements PersistentStateComponent<AzureResourceManager.State> {

    private final List<String> myResourceIds = new ArrayList<>();

    public AzureResourceManager() {

    }

    public static AzureResourceManager getInstance() {
        return ApplicationManager.getApplication().getService(AzureResourceManager.class);
    }

    public @Unmodifiable List<String> getResources() {
        return List.copyOf(this.myResourceIds);
    }

    @Override
    public @Nullable AzureResourceManager.State getState() {
        final State state = new State();
        state.myResources.addAll(this.myResourceIds.stream().toList());
        return state;
    }

    public void loadState(@NotNull State state) {
        this.myResourceIds.clear();
        this.myResourceIds.addAll(state.myResources);
    }

    public void addResource(@NotNull AbstractAzResource<?, ?, ?> resource) {
        if (this.myResourceIds.stream().anyMatch(r -> r.equalsIgnoreCase(resource.getId()))) { // ResourceId doesn't override equals
            return;
        }
        this.myResourceIds.add(resource.getId());
        ApplicationManager.getApplication().getMessageBus().syncPublisher(Listener.TOPIC).resourceAdded(resource);
    }

    public void addResources(@NotNull List<AbstractAzResource<?, ?, ?>> resources) {
        final List<String> newResourceIds = resources.stream().map(AbstractAzResource::getId).toList();
        final Collection<String> toAdd = CollectionUtils.subtract(newResourceIds, this.myResourceIds);
        if (!toAdd.isEmpty()) {
            this.myResourceIds.addAll(toAdd);
            ApplicationManager.getApplication().getMessageBus().syncPublisher(Listener.TOPIC).resourcesAdded(resources);
        }
    }

    public void removeResource(@NotNull AbstractAzResource<?, ?, ?> resource) {
        this.myResourceIds.removeIf(r -> r.equalsIgnoreCase(resource.getId()));
        ApplicationManager.getApplication().getMessageBus().syncPublisher(Listener.TOPIC).resourceRemoved(resource);
    }

    public interface Listener {
        Topic<Listener> TOPIC = Topic.create("ServiceViewAzureResourceManager.topic", Listener.class);

        void resourceAdded(AbstractAzResource<?, ?, ?> resource);

        void resourcesAdded(List<AbstractAzResource<?, ?, ?>> resources);

        void resourceRemoved(AbstractAzResource<?, ?, ?> resource);
    }

    public static class State {
        @Property(
            surroundWithTag = false
        )
        @XCollection
        public List<String> myResources = new ArrayList<>();
    }
}
