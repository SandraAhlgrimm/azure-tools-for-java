/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.appmod.javaupgrade.action;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.azure.toolkit.intellij.appmod.javaupgrade.service.JavaVersionNotificationService;

import org.jetbrains.annotations.NotNull;

/**
 * Action to upgrade a Java project using GitHub Copilot.
 * This action appears in the GitHub Copilot submenu when right-clicking on:
 * - Project root folder
 * - pom.xml (Maven projects)
 * - build.gradle or build.gradle.kts (Gradle projects)
 */
public class UpgradeProjectAction extends AnAction {

    private static final String UPGRADE_JAVA_PROMPT = "Upgrade java runtime and java framework dependencies of this project to the latest LTS version using java upgrade tools by invoking #generate_upgrade_plan";

    // text, description, and icon are defined in azure-intellij-plugin-appmod.xml
    public UpgradeProjectAction() {
        super();
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        final Project project = e.getProject();
        final VirtualFile file = e.getData(CommonDataKeys.VIRTUAL_FILE);
        
        boolean visible = false;
        
        if (project != null && file != null) {
            // Check if it's a project root, pom.xml, or build.gradle file
            visible = isProjectRoot(project, file) || 
                      isMavenBuildFile(file) || 
                      isGradleBuildFile(file);
        }
        
        e.getPresentation().setEnabledAndVisible(visible);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        final Project project = e.getProject();
        if (project == null) {
            return;
        }

        final VirtualFile file = e.getData(CommonDataKeys.VIRTUAL_FILE);
        String prompt = buildUpgradePrompt(project, file);
        
        // Open Copilot chat with the upgrade prompt
        JavaVersionNotificationService.getInstance().openCopilotChatWithPrompt(project, prompt);
    }

    /**
     * Builds the upgrade prompt based on the selected file.
     */
    private String buildUpgradePrompt(Project project, VirtualFile file) {
        return UPGRADE_JAVA_PROMPT;
    }

    /**
     * Checks if the file is the project root directory.
     */
    private boolean isProjectRoot(Project project, VirtualFile file) {
        if (!file.isDirectory()) {
            return false;
        }
        
        final VirtualFile projectBaseDir = project.getBaseDir();
        if (projectBaseDir == null) {
            return false;
        }
        
        return file.equals(projectBaseDir);
    }

    /**
     * Checks if the file is a Maven build file (pom.xml).
     */
    private boolean isMavenBuildFile(VirtualFile file) {
        return file != null && !file.isDirectory() && "pom.xml".equals(file.getName());
    }

    /**
     * Checks if the file is a Gradle build file (build.gradle or build.gradle.kts).
     */
    private boolean isGradleBuildFile(VirtualFile file) {
        if (file == null || file.isDirectory()) {
            return false;
        }
        final String name = file.getName();
        return "build.gradle".equals(name) || "build.gradle.kts".equals(name);
    }
}
