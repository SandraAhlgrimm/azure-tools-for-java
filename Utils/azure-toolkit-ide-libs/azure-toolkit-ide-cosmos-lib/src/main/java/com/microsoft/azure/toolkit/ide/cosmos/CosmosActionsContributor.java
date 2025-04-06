/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.cosmos;

import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.ActionGroup;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.action.IActionGroup;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.cosmos.CosmosDBAccount;
import com.microsoft.azure.toolkit.lib.cosmos.ICosmosDocument;
import com.microsoft.azure.toolkit.lib.cosmos.ICosmosDocumentContainer;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroup;

public class CosmosActionsContributor implements IActionsContributor {
    public static final int INITIALIZE_ORDER = ResourceCommonActionsContributor.INITIALIZE_ORDER + 1;

    public static final String SERVICE_ACTIONS = "actions.cosmos.service";
    public static final String ACCOUNT_ACTIONS = "actions.cosmos.account";
    public static final String SQL_ACCOUNT_ACTIONS = "actions.cosmos.sql_account";
    public static final String MONGO_ACCOUNT_ACTIONS = "actions.cosmos.mongo_account";
    public static final String CASSANDRA_ACCOUNT_ACTIONS = "actions.cosmos.cassandra_account";
    public static final String SQL_DATABASE_ACTIONS = "actions.cosmos.sql_database";
    public static final String MONGO_DATABASE_ACTIONS = "actions.cosmos.mongo_database";
    public static final String CASSANDRA_KEYSPACE_ACTIONS = "actions.cosmos.cassandra_keyspace";
    public static final String SQL_CONTAINER_ACTIONS = "actions.cosmos.sql_container";
    public static final String MONGO_COLLECTION_ACTIONS = "actions.cosmos.mongo_collection";
    public static final String CASSANDRA_TABLE_ACTIONS = "actions.cosmos.cassandra_table";
    public static final String COSMOS_DOCUMENT_ACTIONS = "actions.cosmos.sql_document";

    public static final Action.Id<CosmosDBAccount> OPEN_DATABASE_TOOL = Action.Id.of("user/cosmos.open_database_tools.account");
    public static final Action.Id<CosmosDBAccount> OPEN_DATA_EXPLORER = Action.Id.of("user/cosmos.open_data_explorer.account");
    public static final Action.Id<CosmosDBAccount> COPY_CONNECTION_STRING = Action.Id.of("user/cosmos.copy_connection_string.account");
    public static final Action.Id<ICosmosDocumentContainer<?>> IMPORT_DOCUMENT = ICosmosDocumentContainer.IMPORT_DOCUMENT;
    public static final Action.Id<ICosmosDocumentContainer<?>> CREATE_DOCUMENT = ICosmosDocumentContainer.CREATE_DOCUMENT;
    public static final Action.Id<ICosmosDocument> OPEN_DOCUMENT = Action.Id.of("user/cosmos.open_document.document");
    public static final Action.Id<ResourceGroup> GROUP_CREATE_COSMOS_SERVICE = Action.Id.of("user/cosmos.create_cosmos_db_account.group");

    @Override
    public void registerActions(AzureActionManager am) {
        new Action<>(OPEN_DATABASE_TOOL)
            .withLabel("Open with Database Tools")
            .withIcon(AzureIcons.Action.OPEN_DATABASE_TOOL.getIconPath())
            .withIdParam(AzResource::getName)
            .visibleWhen(s -> s instanceof CosmosDBAccount)
            .enableWhen(s -> s.getFormalStatus().isRunning())
            .register(am);

        new Action<>(COPY_CONNECTION_STRING)
            .withLabel("Copy Connection String")
            .withIdParam(AzResource::getName)
            .visibleWhen(s -> s instanceof CosmosDBAccount)
            .enableWhen(s -> s.getFormalStatus().isConnected())
            .withHandler((resource, e) -> {
                final String connectionString = resource.listConnectionStrings().getPrimaryConnectionString();
                am.getAction(ResourceCommonActionsContributor.COPY_STRING).handle(connectionString, e);
                AzureMessager.getMessager().info("Connection string copied");
            })
            .register(am);

        new Action<>(OPEN_DATA_EXPLORER)
            .withLabel("Open Data Explorer")
            .withIdParam(AzResource::getName)
            .visibleWhen(s -> s instanceof CosmosDBAccount)
            .enableWhen(s -> s.getFormalStatus().isConnected())
            .withHandler((resource, e) -> am.getAction(ResourceCommonActionsContributor.OPEN_URL).handle(resource.getPortalUrl() + "/dataExplorer", e))
            .register(am);

        new Action<>(GROUP_CREATE_COSMOS_SERVICE)
            .withLabel("Azure Cosmos DB")
            .withIdParam(AzResource::getName)
            .visibleWhen(s -> s instanceof ResourceGroup)
            .enableWhen(s -> s.getFormalStatus().isConnected())
            .register(am);

        new Action<>(OPEN_DOCUMENT)
            .withLabel("Open Document")
            .withIdParam(AzResource::getName)
            .visibleWhen(s -> s instanceof ICosmosDocument)
            .enableWhen(s -> s.getFormalStatus().isConnected())
            .register(am);

        new Action<>(IMPORT_DOCUMENT)
            .withLabel("Import Document")
            .withIdParam(AzResource::getName)
            .visibleWhen(s -> s instanceof ICosmosDocumentContainer<?>)
            .enableWhen(s -> s.getFormalStatus().isConnected())
            .register(am);

        new Action<>(CREATE_DOCUMENT)
            .withLabel("New Document")
            .withIdParam(AzResource::getName)
            .visibleWhen(s -> s instanceof ICosmosDocumentContainer<?>)
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
            CosmosActionsContributor.OPEN_DATA_EXPLORER,
            "---",
            CosmosActionsContributor.OPEN_DATABASE_TOOL,
            "---",
            ResourceCommonActionsContributor.CREATE,
            ResourceCommonActionsContributor.DELETE,
            "---",
            ResourceCommonActionsContributor.CONNECT,
            CosmosActionsContributor.COPY_CONNECTION_STRING
        );
        am.registerGroup(ACCOUNT_ACTIONS, accountActionGroup);
        am.registerGroup(SQL_ACCOUNT_ACTIONS, accountActionGroup);
        am.registerGroup(MONGO_ACCOUNT_ACTIONS, accountActionGroup);
        am.registerGroup(CASSANDRA_ACCOUNT_ACTIONS, accountActionGroup);

        final ActionGroup databaseGroup = new ActionGroup(
            ResourceCommonActionsContributor.PIN,
            "---",
            ResourceCommonActionsContributor.REFRESH,
            ResourceCommonActionsContributor.OPEN_PORTAL_URL,
            "---",
            ResourceCommonActionsContributor.CONNECT,
            "---",
            ResourceCommonActionsContributor.CREATE,
            ResourceCommonActionsContributor.DELETE
        );
        am.registerGroup(SQL_DATABASE_ACTIONS, databaseGroup);
        am.registerGroup(MONGO_DATABASE_ACTIONS, databaseGroup);
        am.registerGroup(CASSANDRA_KEYSPACE_ACTIONS, databaseGroup);

        final ActionGroup collectionGroup = new ActionGroup(
            ResourceCommonActionsContributor.PIN,
            "---",
            ResourceCommonActionsContributor.REFRESH,
            ResourceCommonActionsContributor.OPEN_PORTAL_URL,
            "---",
            ResourceCommonActionsContributor.DELETE
        );
        am.registerGroup(CASSANDRA_TABLE_ACTIONS, collectionGroup);

        final ActionGroup cosmosDocumentModuleGroup = new ActionGroup(
            ResourceCommonActionsContributor.PIN,
            "---",
            ResourceCommonActionsContributor.REFRESH,
            ResourceCommonActionsContributor.OPEN_PORTAL_URL,
            "---",
            CosmosActionsContributor.CREATE_DOCUMENT,
            CosmosActionsContributor.IMPORT_DOCUMENT,
            "---",
            ResourceCommonActionsContributor.DELETE
        );
        am.registerGroup(SQL_CONTAINER_ACTIONS, cosmosDocumentModuleGroup);
        am.registerGroup(MONGO_COLLECTION_ACTIONS, cosmosDocumentModuleGroup);

        am.registerGroup(COSMOS_DOCUMENT_ACTIONS, new ActionGroup(
            CosmosActionsContributor.OPEN_DOCUMENT,
            "---",
            ResourceCommonActionsContributor.DELETE
        ));

        final IActionGroup group = am.getGroup(ResourceCommonActionsContributor.RESOURCE_GROUP_CREATE_ACTIONS);
        group.addAction(GROUP_CREATE_COSMOS_SERVICE);
    }

    @Override
    public int getOrder() {
        return INITIALIZE_ORDER;
    }
}
