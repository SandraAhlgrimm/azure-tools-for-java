/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.legacy.webapp.action;

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
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;
import com.microsoft.azure.toolkit.intellij.container.model.DockerImage;
import com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.WebAppConfigurationType;
import com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.webapponlinux.WebAppOnLinuxDeployConfiguration;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.OperationContext;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.containerregistry.Tag;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;

import static com.microsoft.azure.toolkit.lib.common.action.Action.EMPTY_PLACE;
import static com.microsoft.azure.toolkit.lib.common.action.Action.PLACE;

public class WebAppOnLinuxAction extends AnAction {
    private static final String DIALOG_TITLE = "Deploy Image to Web App";
    private static final WebAppConfigurationType configType = WebAppConfigurationType.getInstance();
    private final DockerImage dockerImage;

    public WebAppOnLinuxAction() {
        this(null);
    }

    public WebAppOnLinuxAction(@Nullable DockerImage dockerImage) {
        //noinspection DialogTitleCapitalization
        super("Deploy Image to Web App...", "Build/push local image to Azure Web App", IntelliJAzureIcons.getIcon("/icons/DockerSupport/RunOnWebApp.svg"));
        this.dockerImage = dockerImage;
    }

    @Override
    @AzureOperation(name = "user/docker.start_app")
    public void actionPerformed(@Nonnull AnActionEvent e) {
        OperationContext.current().setTelemetryProperty(PLACE, StringUtils.firstNonBlank(e.getPlace(), EMPTY_PLACE));
        final Project project = e.getProject();
        if (project != null) {
            AzureActionManager.getInstance().getAction(Action.REQUIRE_AUTH)
                .handle((a) -> runConfiguration(project, this.dockerImage));
        }
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    public static void deploy(@Nonnull Tag tag, @Nonnull Project project) {
        final DockerImage image = DockerImage.builder()
            .isDraft(false)
            .repositoryName(tag.getParent().getParent().getName())
            .tagName(tag.getName())
            .build();
        runConfiguration(project, image);
    }

    private static void runConfiguration(@Nonnull Project project, @Nullable DockerImage image) {
        final RunManagerEx manager = RunManagerEx.getInstanceEx(project);
        final ConfigurationFactory factory = configType.getWebAppOnLinuxConfigurationFactory();
        final String configurationName = String.format("%s: %s", factory.getName(), project.getName());
        final RunnerAndConfigurationSettings existingSettings = manager.findConfigurationByName(configurationName);
        final RunnerAndConfigurationSettings settings = Optional.ofNullable(existingSettings)
            .orElseGet(() -> manager.createConfiguration(configurationName, factory));
        if (settings.getConfiguration() instanceof WebAppOnLinuxDeployConfiguration) {
            ((WebAppOnLinuxDeployConfiguration) settings.getConfiguration()).setDockerImage(image);
        }
        AzureTaskManager.getInstance().runLater(() -> {
            if (RunDialog.editConfiguration(project, settings, DIALOG_TITLE, DefaultRunExecutor.getRunExecutorInstance())) {
                settings.storeInLocalWorkspace();
                manager.addConfiguration(settings);
                manager.setSelectedConfiguration(settings);
                ProgramRunnerUtil.executeConfiguration(settings, DefaultRunExecutor.getRunExecutorInstance());
            }
        });
    }
}
