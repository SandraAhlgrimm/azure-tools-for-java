/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.cognitiveservices;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.ide.cognitiveservices.CognitiveServicesActionsContributor;
import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.ide.guidance.GuidanceViewManager;
import com.microsoft.azure.toolkit.intellij.cognitiveservices.creation.CognitiveAccountCreationDialog;
import com.microsoft.azure.toolkit.intellij.cognitiveservices.creation.CognitiveDeploymentCreationDialog;
import com.microsoft.azure.toolkit.intellij.common.properties.IntellijShowPropertiesViewAction;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.cognitiveservices.*;
import com.microsoft.azure.toolkit.lib.cognitiveservices.model.DeploymentModel;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzService;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.common.utils.Utils;
import com.microsoft.azure.toolkit.lib.resource.AzureResources;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroup;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Supplier;

public class IntelliJCognitiveServicesActionsContributor implements IActionsContributor {
    public static final Action.Id<Project> TRY_OPENAI = Action.Id.of("user/openai.try_openai");
    public static final Action.Id<CognitiveDeployment> TRY_PLAYGROUND = Action.Id.of("user/openai.try_playground.deployment");

    @Override
    public void registerHandlers(AzureActionManager am) {
        final BiPredicate<AbstractAzService<?, ?>, AnActionEvent> gettingStartCondition = (r, e) -> r instanceof AzureCognitiveServices;
        am.registerHandler(ResourceCommonActionsContributor.GETTING_STARTED, gettingStartCondition,
            (AbstractAzService<?, ?> c, AnActionEvent e) -> GuidanceViewManager.getInstance().openCourseView(e.getProject(), "hello-openai"));

        final BiPredicate<AzureCognitiveServices, AnActionEvent> serviceCondition = (r, e) -> r instanceof AzureCognitiveServices;
        final BiConsumer<AzureCognitiveServices, AnActionEvent> createAccountHandler = (c, e) -> openAccountCreationDialog(e.getProject(), null);
        am.registerHandler(CognitiveServicesActionsContributor.CREATE_ACCOUNT, serviceCondition, createAccountHandler);
        am.registerHandler(CognitiveServicesActionsContributor.GROUP_CREATE_ACCOUNT,
            (ResourceGroup r, AnActionEvent e) -> openAccountCreationDialog(e.getProject(), r));

        final BiPredicate<CognitiveAccount, AnActionEvent> accountCondition = (r, e) -> r instanceof CognitiveAccount;
        am.registerHandler(CognitiveServicesActionsContributor.OPEN_ACCOUNT_IN_PLAYGROUND, accountCondition, this::openAccountInAIPlayground);

        final BiConsumer<CognitiveAccount, AnActionEvent> createDeploymentHandler = (c, e) -> openDeploymentCreationDialog(c, e.getProject());
        am.registerHandler(CognitiveServicesActionsContributor.CREATE_DEPLOYMENT, accountCondition, createDeploymentHandler);

        final BiPredicate<CognitiveDeployment, AnActionEvent> deploymentCondition = (r, e) -> r instanceof CognitiveDeployment;
        final BiConsumer<CognitiveDeployment, AnActionEvent> openDeploymentHandler = (c, e) -> IntellijShowPropertiesViewAction.showPropertyView(c, e.getProject());
        am.registerHandler(CognitiveServicesActionsContributor.OPEN_DEPLOYMENT_IN_PLAYGROUND, deploymentCondition, openDeploymentHandler);
    }

    @Override
    public void registerActions(final AzureActionManager am) {
        new Action<>(TRY_OPENAI)
            .withLabel("Try Azure OpenAI")
            .withHandler((_d, e) -> GuidanceViewManager.getInstance().openCourseView(((AnActionEvent) e).getProject(), "hello-openai"))
            .withAuthRequired(false)
            .register(am);
    }

    private void openAccountInAIPlayground(@Nonnull final CognitiveAccount account, @Nonnull final AnActionEvent event) {
        final boolean isGPTDeploymentExists = account.deployments().list().stream()
                .anyMatch(deployment -> Optional.ofNullable(deployment.getModel()).map(DeploymentModel::isGPTModel).orElse(false));
        if (!isGPTDeploymentExists) {
            final String errorMessage = String.format("There is no GPT model in current Azure OpenAI service %s", account.getName());
            final Action<CognitiveAccount> errorAction = AzureActionManager.getInstance().getAction(CognitiveServicesActionsContributor.CREATE_DEPLOYMENT).bind(account);
            throw new AzureToolkitRuntimeException(errorMessage, errorAction);
        }
        IntellijShowPropertiesViewAction.showPropertyView(account, event.getProject());
    }

    public static void openAccountCreationDialog(@Nullable Project project, @Nullable ResourceGroup resourceGroup) {
        // action is auth required, so skip validation for authentication
        final String account = Utils.generateRandomResourceName("service", 40);
        final String rgName = Optional.ofNullable(resourceGroup).map(AzResource::getName)
            .orElseGet(() -> String.format("rg-%s", account));
        final Supplier<Subscription> supplier = () -> Azure.az(AzureAccount.class).account().getSelectedSubscriptions()
                .stream().findFirst().orElseThrow(() -> new AzureToolkitRuntimeException("there are no subscription selected in your account"));
        final Subscription subscription = Optional.ofNullable(resourceGroup).map(ResourceGroup::getSubscription)
            .orElseGet(supplier);
        final ResourceGroup group = Optional.ofNullable(resourceGroup)
            .orElseGet(() -> Azure.az(AzureResources.class).groups(subscription.getId()).create(rgName, rgName));
        final CognitiveAccountDraft accountDraft =
            Azure.az(AzureCognitiveServices.class).accounts(subscription.getId()).create(account, rgName);
        accountDraft.setConfig(CognitiveAccountDraft.Config.builder().resourceGroup(group).build());
        AzureTaskManager.getInstance().runLater(() -> {
            final CognitiveAccountCreationDialog dialog = new CognitiveAccountCreationDialog(project);
            dialog.setValue(accountDraft);
            dialog.setOkAction(new Action<CognitiveAccountDraft>(Action.Id.of("user/openai.create_account.account"))
                .withIdParam(accountDraft.getName())
                .withLabel("Create")
                .withAuthRequired(true)
                .withHandler(AzResource.Draft::commit));
            dialog.show();
        });
    }

    public static void openDeploymentCreationDialog(@Nonnull CognitiveAccount account, @Nullable Project project) {
        final String name = Utils.generateRandomResourceName("deployment", 40);
        final CognitiveDeploymentDraft draft = account.deployments().create(name, account.getResourceGroupName());
        AzureTaskManager.getInstance().runLater(() -> {
            final CognitiveDeploymentCreationDialog dialog = new CognitiveDeploymentCreationDialog(account, project);
            dialog.setOkAction(new Action<CognitiveDeploymentDraft>(Action.Id.of("user/openai.create_deployment.deployment|account"))
                .withLabel("Create")
                .withIdParam(draft.getName())
                .withIdParam(account.getName())
                .withAuthRequired(true)
                .withHandler(AzResource.Draft::commit));
            dialog.setValue(draft);
            dialog.show();
        });
    }
}
