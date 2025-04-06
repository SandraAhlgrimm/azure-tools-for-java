/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.applicationinsights;

import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.lib.applicationinsights.ApplicationInsight;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.ActionGroup;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.action.IActionGroup;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroup;

public class ApplicationInsightsActionsContributor implements IActionsContributor {
    public static final int INITIALIZE_ORDER = ResourceCommonActionsContributor.INITIALIZE_ORDER + 1;

    public static final String SERVICE_ACTIONS = "actions.applicationinsights.service";
    public static final String INSIGHT_ACTIONS = "actions.applicationinsights.management";

    public static final Action.Id<ApplicationInsight> OPEN_APPLICATION_MAP = Action.Id.of("user/ai.open_application_map.ai");
    public static final Action.Id<ApplicationInsight> OPEN_LIVE_METRICS = ApplicationInsight.OPEN_LIVE_METRICS;
    public static final Action.Id<ApplicationInsight> OPEN_TRANSACTION_SEARCH = Action.Id.of("user/ai.open_transaction_search.ai");
    public static final Action.Id<ApplicationInsight> COPY_INSTRUMENTATION_KEY = Action.Id.of("user/ai.copy_instrumentation_key.ai");
    public static final Action.Id<ApplicationInsight> COPY_CONNECTION_STRING = ApplicationInsight.COPY_CONNECTION_STRING;
    public static final Action.Id<ResourceGroup> GROUP_CREATE_APPLICATIONINSIGHT = Action.Id.of("user/ai.create_ai.group");
    public static final Action.Id<ApplicationInsight> OPEN_LOGS_IN_MONITOR = Action.Id.of("user/ai.open_azure_monitor.ai");

    @Override
    public void registerActions(AzureActionManager am) {
        new Action<>(COPY_CONNECTION_STRING)
            .withLabel("Copy Connection String")
            .withIdParam(AzResource::getName)
            .visibleWhen(s -> s instanceof ApplicationInsight)
            .enableWhen(s -> s.getFormalStatus().isConnected())
            .withHandler((insight, e) -> {
                am.getAction(ResourceCommonActionsContributor.COPY_STRING).handle(insight.getConnectionString(), e);
                AzureMessager.getMessager().info("Connection string copied");
            })
            .register(am);

        new Action<>(COPY_INSTRUMENTATION_KEY)
            .withLabel("Copy Instrumentation Key")
            .withIdParam(AzResource::getName)
            .visibleWhen(s -> s instanceof ApplicationInsight)
            .enableWhen(s -> s.getFormalStatus().isConnected())
            .withHandler((insight, e) -> {
                am.getAction(ResourceCommonActionsContributor.COPY_STRING).handle(insight.getInstrumentationKey(), e);
                AzureMessager.getMessager().info("Instrumentation key copied");
            })
            .register(am);

        new Action<>(OPEN_APPLICATION_MAP)
            .withLabel("Open Application Map")
            .withIcon(AzureIcons.Action.PORTAL.getIconPath())
            .withIdParam(AzResource::getName)
            .visibleWhen(s -> s instanceof ApplicationInsight)
            .enableWhen(s -> s.getFormalStatus().isConnected())
            .withHandler((insight, e) -> am.getAction(ResourceCommonActionsContributor.OPEN_URL).handle(insight.getPortalUrl() + "/applicationMap", e))
            .register(am);

        new Action<>(OPEN_LIVE_METRICS)
            .withLabel("Open Live Metrics")
            .withIcon(AzureIcons.Action.PORTAL.getIconPath())
            .withIdParam(AzResource::getName)
            .visibleWhen(s -> s instanceof ApplicationInsight)
            .enableWhen(s -> s.getFormalStatus().isConnected())
            .withHandler((insight, e) -> am.getAction(ResourceCommonActionsContributor.OPEN_URL).handle(insight.getPortalUrl() + "/quickPulse", e))
            .register(am);

        new Action<>(OPEN_TRANSACTION_SEARCH)
            .withLabel("Open Transaction Search")
            .withIcon(AzureIcons.Action.PORTAL.getIconPath())
            .withIdParam(AzResource::getName)
            .visibleWhen(s -> s instanceof ApplicationInsight)
            .enableWhen(s -> s.getFormalStatus().isConnected())
            .withHandler((insight, e) -> am.getAction(ResourceCommonActionsContributor.OPEN_URL).handle(insight.getPortalUrl() + "/searchV1", e))
            .register(am);

        new Action<>(GROUP_CREATE_APPLICATIONINSIGHT)
            .withLabel("Application Insights")
            .withIdParam(AzResource::getName)
            .visibleWhen(s -> s instanceof ResourceGroup)
            .enableWhen(s -> s.getFormalStatus().isConnected())
            .register(am);

        new Action<>(OPEN_LOGS_IN_MONITOR)
            .withLabel("Open Logs with Azure Monitor")
            .withIcon(AzureIcons.Common.AZURE_MONITOR.getIconPath())
            .withIdParam(AzResource::getName)
            .visibleWhen(s -> s instanceof ApplicationInsight)
            .enableWhen(s -> s.getFormalStatus().isConnected())
            .register(am);
    }

    @Override
    public void registerGroups(AzureActionManager am) {
        final ActionGroup serviceActionGroup = new ActionGroup(
            ResourceCommonActionsContributor.REFRESH,
            ResourceCommonActionsContributor.OPEN_AZURE_REFERENCE_BOOK,
            ResourceCommonActionsContributor.BROWSE_SERVICE_AZURE_SAMPLES,
            "---",
            ResourceCommonActionsContributor.CREATE
        );
        am.registerGroup(SERVICE_ACTIONS, serviceActionGroup);

        final ActionGroup accountActionGroup = new ActionGroup(
            ResourceCommonActionsContributor.PIN,
            "---",
            ResourceCommonActionsContributor.REFRESH,
            ResourceCommonActionsContributor.OPEN_AZURE_REFERENCE_BOOK,
            ResourceCommonActionsContributor.BROWSE_SERVICE_AZURE_SAMPLES,
            ResourceCommonActionsContributor.OPEN_PORTAL_URL,
            "---",
            ApplicationInsightsActionsContributor.COPY_CONNECTION_STRING,
            ApplicationInsightsActionsContributor.COPY_INSTRUMENTATION_KEY,
            "---",
            ResourceCommonActionsContributor.CONNECT,
            ResourceCommonActionsContributor.DELETE,
            "---",
            ApplicationInsightsActionsContributor.OPEN_APPLICATION_MAP,
            ApplicationInsightsActionsContributor.OPEN_LIVE_METRICS,
            ApplicationInsightsActionsContributor.OPEN_TRANSACTION_SEARCH,
            "---",
            ApplicationInsightsActionsContributor.OPEN_LOGS_IN_MONITOR
        );
        am.registerGroup(INSIGHT_ACTIONS, accountActionGroup);

        final IActionGroup group = am.getGroup(ResourceCommonActionsContributor.RESOURCE_GROUP_CREATE_ACTIONS);
        group.addAction(GROUP_CREATE_APPLICATIONINSIGHT);
    }

    @Override
    public int getOrder() {
        return INITIALIZE_ORDER;
    }
}
