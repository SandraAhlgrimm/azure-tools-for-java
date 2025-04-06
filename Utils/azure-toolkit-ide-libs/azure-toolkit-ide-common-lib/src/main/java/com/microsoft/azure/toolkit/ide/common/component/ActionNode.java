/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.common.component;

import com.microsoft.azure.toolkit.ide.common.icon.AzureIcon;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ActionNode<T> extends Node<Action<T>> {
    @Nullable
    private final T resource;

    public ActionNode(@Nonnull Action.Id<T> id) {
        this(id, null);
    }

    public ActionNode(@Nonnull Action.Id<T> id, @Nullable final T resource) {
        this(AzureActionManager.getInstance().getAction(id), resource);
    }

    public ActionNode(@Nonnull Action<T> action) {
        this(action, null);
    }

    public ActionNode(@Nonnull Action<T> action, @Nullable final T resource) {
        super(action);
        this.resource = resource;
        this.withIcon(i -> AzureIcon.builder().iconPath(getAction().getView(resource).getIconPath()).build());
        this.withLabel(i -> getAction().getView(resource).getLabel());
        this.withDescription(i -> getAction().getView(resource).getDescription());
        this.onClicked((a, event) -> getValue().handle(resource, event));
    }

    public boolean isEnabled() {
        return getAction().getView(resource).isEnabled();
    }

    public void invoke(@Nullable final Object event) {
        getAction().handle(resource, event);
    }

    private Action<T> getAction() {
        return getValue();
    }
}
