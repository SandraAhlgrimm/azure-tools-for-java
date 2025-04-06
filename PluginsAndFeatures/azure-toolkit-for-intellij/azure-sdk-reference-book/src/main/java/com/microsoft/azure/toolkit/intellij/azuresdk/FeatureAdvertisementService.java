package com.microsoft.azure.toolkit.intellij.azuresdk;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.intellij.azuresdk.service.MachineTaggingService;
import com.microsoft.azure.toolkit.intellij.azuresdk.service.WorkspaceTaggingService;
import com.microsoft.azure.toolkit.intellij.common.messager.IntellijAzureMessage;
import com.microsoft.azure.toolkit.lib.AzService;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessage;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import org.apache.commons.collections4.ListUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

public class FeatureAdvertisementService {
    private static final String ADVERTISED_SERVICES = "azure.advertised_services";

    private static final Map<String, String> MACHINE_SERVICES = new LinkedHashMap<>() {{
        put("docker", "Microsoft.ContainerRegistry");
        put("kubectl", "Microsoft.ContainerService");
        put("podman", "Microsoft.ContainerService");

        put("mysql", "Microsoft.DBforMySQL");
        put("psql", "Microsoft.DBforPostgreSQL");
        put("sqlservr", "Microsoft.Sql");
        put("redis-server", "Microsoft.Cache");
        put("redis-cli", "Microsoft.Cache");
        put("azurite", "Microsoft.Storage");
        put("Microsoft.Azure.Cosmos.Emulator", "Microsoft.DocumentDB");
    }};

    private static final Map<String, String> PROJECT_SERVICES = new LinkedHashMap<>() {{
        put("openai", "Microsoft.CognitiveServices");
        put("azure_openai", "Microsoft.CognitiveServices");
        put("redis", "Microsoft.Cache");
        put("eventhubs", "Microsoft.EventHub");
        put("servicebus", "Microsoft.ServiceBus");
        put("azure_storage", "Microsoft.Storage");
        put("cosmos", "Microsoft.DocumentDB");
        put("functions", "Microsoft.Web");
    }};
    public static final String MSG_TEMPLATE = "Your project appears to use %s, you can manage them in %s after signing-in";

    public static void advertiseProjectService(@Nonnull final Project project) {
        final String service = getNextProjectAdService(project);
        if (service != null) {
            doAdvertise(project, service);
        }
    }

    @AzureOperation("auto/sdk.advertise_service")
    private static void doAdvertise(@Nonnull Project project, String service) {
        final IntellijAzureMessage message = (IntellijAzureMessage) buildMessage(service);
        if (message != null) {
            message.setProject(project).setPriority(IntellijAzureMessage.PRIORITY_HIGH).show(AzureMessager.getMessager());
            final List<String> advertisedServices = Optional.ofNullable(PropertiesComponent.getInstance().getList(ADVERTISED_SERVICES)).orElse(new ArrayList<>());
            advertisedServices.add(service);
            PropertiesComponent.getInstance().setList(ADVERTISED_SERVICES, advertisedServices);
        }
    }

    @Nullable
    private static IAzureMessage buildMessage(@Nonnull String service) {
        final Action<Object> focusService = AzureActionManager.getInstance().getAction(ResourceCommonActionsContributor.SELECT_RESOURCE_IN_EXPLORER);
        final Action<Object> openExplorer = AzureActionManager.getInstance().getAction(ResourceCommonActionsContributor.OPEN_AZURE_EXPLORER).bind(new Object()).withLabel("Open Azure Explorer");
        final Action<Object> startAzurite = AzureActionManager.getInstance().getAction(Action.Id.of("user/storage.start_azurite_instance"));
        final Action<Object> tryOpenAI = AzureActionManager.getInstance().getAction(Action.Id.of("user/openai.try_openai"));
        final List<AzService> services = Azure.getServices(service);
        final Action<Object> openExplorerAction = services.isEmpty() ? openExplorer : focusService.bind(services.get(0)).withLabel("Open in Azure Explorer");

        return switch (service) {
            case "Microsoft.CognitiveServices" -> AzureMessager.getMessager().buildInfoMessage(
                AzureString.format(MSG_TEMPLATE + ", and try your own \"%s\" with \"%s\" model deployments in the integrated %s. <a href='https://azure.microsoft.com/en-us/products/ai-services/openai-service?_ijop_=openai.learn_more'>Learn more</a>.",
                    "Azure OpenAI services", "Azure Explorer", "Copilot", "GPT*", "AI playground"), openExplorerAction, tryOpenAI);
            case "Microsoft.Cache" -> AzureMessager.getMessager().buildInfoMessage(
                AzureString.format(MSG_TEMPLATE + ", and explore or manage cached data in the integrated %s. <a href='https://azure.microsoft.com/en-us/products/cache/?_ijop_=redis.learn_more'>Learn more</a>.",
                    "Azure Cache for Redis", "Azure Explorer", "Redis Explorer"), openExplorerAction);
            case "Microsoft.EventHub" -> AzureMessager.getMessager().buildInfoMessage(
                AzureString.format(MSG_TEMPLATE + ", and send messages to or monitor messages with the integrated %s. <a href='https://azure.microsoft.com/en-us/products/event-hubs?_ijop_=eventhubs.learn_more'>Learn more</a>.",
                    "Azure Event Hubs", "Azure Explorer", "Event Hub Explorer"), openExplorerAction);
            case "Microsoft.ServiceBus" -> AzureMessager.getMessager().buildInfoMessage(
                AzureString.format(MSG_TEMPLATE + ", and send messages to or monitor messages with the integrated %s. <a href='https://azure.microsoft.com/en-us/products/service-bus/?_ijop_=servicebus.learn_more'>Learn more</a>.",
                    "Azure Service Bus Messaging", "Azure Explorer", "Service Bus Explorer"), openExplorerAction);
            case "Microsoft.Storage" -> AzureMessager.getMessager().buildInfoMessage(
                AzureString.format(MSG_TEMPLATE + ". It offers features for inspecting blobs and files, and an integrated <a href='https://learn.microsoft.com/en-us/azure/storage/common/storage-use-azurite'>Azurite emulator</a> for local development.",
                    "Azure Storage", "Azure Explorer"), openExplorerAction, startAzurite);
            case "Microsoft.DocumentDB" -> AzureMessager.getMessager().buildInfoMessage(
                AzureString.format(MSG_TEMPLATE + ". It offers features for managing documents. <a href='https://azure.microsoft.com/en-us/products/cosmos-db?_ijop_=cosmos.learn_more'>Learn more</a>.",
                    "Azure Cosmos DB", "Azure Explorer"), openExplorerAction);
            case "Microsoft.Web" -> AzureMessager.getMessager().buildInfoMessage(
                AzureString.format(MSG_TEMPLATE + ". It offers rich features for debugging, streaming logs, and browsing online files...",
                    "Azure Functions", "Azure Explorer"), openExplorerAction);
            default -> null;
        };
    }

    @Nonnull
    public static List<String> getProjectServices(Project project) {
        final Set<String> tags = WorkspaceTaggingService.getWorkspaceTags(project);
        return PROJECT_SERVICES.entrySet().stream()
            .filter(e -> tags.contains(e.getKey()))
            .map(Map.Entry::getValue).toList();
    }

    @Nonnull
    public static List<String> getMachineServices() {
        final Set<String> tags = MachineTaggingService.getMachineTags();
        return MACHINE_SERVICES.entrySet().stream()
            .filter(e -> tags.contains(e.getKey()))
            .map(Map.Entry::getValue).toList();
    }

    @Nullable
    private static String getNextProjectAdService(@Nonnull final Project project) {
        final List<String> services = getProjectServices(project);
        if (!services.isEmpty()) {
            final List<String> advertisedServices = Optional.ofNullable(PropertiesComponent.getInstance().getList(ADVERTISED_SERVICES)).orElse(new ArrayList<>());
            final List<String> nonAdvertisedServices = ListUtils.subtract(services, advertisedServices);
            if (!nonAdvertisedServices.isEmpty()) {
                return nonAdvertisedServices.get(0);
            }
        }
        return null;
    }
}
