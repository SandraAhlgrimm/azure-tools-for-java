/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */


package com.microsoft.azure.toolkit.intellij.springcloud.deplolyment;

import com.intellij.execution.ProgramRunnerUtil;
import com.intellij.execution.RunManagerEx;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.impl.RunDialog;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.messager.ExceptionNotification;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.OperationContext;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudApp;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudDeployment;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudDeploymentDraft;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;

import static com.microsoft.azure.toolkit.lib.common.action.Action.EMPTY_PLACE;
import static com.microsoft.azure.toolkit.lib.common.action.Action.PLACE;

public class DeploySpringCloudAppAction extends AnAction {
    private static final String DEPLOY_SPRING_CLOUD_APP_TITLE = "Deploy to Azure";
    private static final SpringCloudDeploymentConfigurationType configType = SpringCloudDeploymentConfigurationType.getInstance();

    @ExceptionNotification
    @AzureOperation(name = "user/springcloud.deploy_app")
    public void actionPerformed(@Nonnull AnActionEvent anActionEvent) {
        OperationContext.current().setTelemetryProperty(PLACE, StringUtils.firstNonBlank(anActionEvent.getPlace(), EMPTY_PLACE));
        final Project project = anActionEvent.getProject();
        if (project != null) {
            AzureActionManager.getInstance().getAction(Action.REQUIRE_AUTH).handle((a) -> deploy((SpringCloudApp) null, project));
        }
    }

    public static void deploy(@Nullable SpringCloudApp app, @Nonnull Project project) {
        final RunnerAndConfigurationSettings settings = getOrCreateConfigurationSettings(project);
        final SpringCloudDeploymentConfiguration configuration = ((SpringCloudDeploymentConfiguration) settings.getConfiguration());
        if (Objects.nonNull(app)) {
            final SpringCloudDeployment d = Optional.ofNullable(app.getActiveDeployment()).orElseGet(() -> app.deployments().create("default", null));
            final SpringCloudDeploymentDraft deployment = (SpringCloudDeploymentDraft) (d.isDraft() ? d : d.update());
            configuration.setDeployment(deployment);
        }
        runConfiguration(project, settings);
    }

    public static void deploy(@Nonnull SpringCloudDeploymentConfiguration configuration, @Nonnull Project project) {
        final RunnerAndConfigurationSettings settings = getOrCreateConfigurationSettings(project);
        runConfiguration(project, settings);
    }

    @AzureOperation(name = "boundary/springcloud.run_deploy_configuration")
    private static void runConfiguration(@Nonnull Project project, RunnerAndConfigurationSettings settings) {
        final RunManagerEx manager = RunManagerEx.getInstanceEx(project);
        AzureTaskManager.getInstance().runLater(() -> {
            if (RunDialog.editConfiguration(project, settings, DEPLOY_SPRING_CLOUD_APP_TITLE, DefaultRunExecutor.getRunExecutorInstance())) {
                settings.storeInLocalWorkspace();
                manager.addConfiguration(settings);
                manager.setBeforeRunTasks(settings.getConfiguration(), new ArrayList<>(manager.getBeforeRunTasks(settings.getConfiguration())));
                manager.setSelectedConfiguration(settings);
                ProgramRunnerUtil.executeConfiguration(settings, DefaultRunExecutor.getRunExecutorInstance());
            }
        });
    }

    @Nonnull
    private static RunnerAndConfigurationSettings getOrCreateConfigurationSettings(@Nonnull Project project) {
        final RunManagerEx manager = RunManagerEx.getInstanceEx(project);
        final ConfigurationFactory factory = configType.getConfigurationFactories()[0];
        final String configurationName = String.format("%s: %s", factory.getName(), project.getName());
        final RunnerAndConfigurationSettings existed = manager.findConfigurationByName(configurationName);
        return Objects.nonNull(existed) ? existed : manager.createConfiguration(configurationName, factory);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}
