/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.appmod.javaupgrade.action;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.appmod.common.AppModPluginInstaller;
import com.microsoft.azure.toolkit.intellij.appmod.javaupgrade.dao.JavaUpgradeIssue;
import com.microsoft.azure.toolkit.intellij.appmod.javaupgrade.service.JavaVersionNotificationService;

import static com.microsoft.azure.toolkit.intellij.appmod.javaupgrade.service.JavaUpgradeIssuesDetectionService.PACKAGE_ID_JDK;

import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

/**
 * Quick fix for Java/Spring version upgrade issues detected in pom.xml files.
 * This action is triggered from Java upgrade issue inspections and opens Copilot
 * to assist with the upgrade process.
 */
public class JavaUpgradeQuickFix implements LocalQuickFix {
    private final JavaUpgradeIssue issue;

    public JavaUpgradeQuickFix(@NotNull JavaUpgradeIssue issue) {
        this.issue = issue;
    }

    @Nls(capitalization = Nls.Capitalization.Sentence)
    @NotNull
    @Override
    public String getFamilyName() {
        return "Azure Toolkit";
    }

    @Nls(capitalization = Nls.Capitalization.Sentence)
    @NotNull
    @Override
    public String getName() {
        String name = "Upgrade " + issue.getPackageDisplayName() + " with Copilot";
        if (!AppModPluginInstaller.isAppModPluginInstalled()) {
            return name + AppModPluginInstaller.TO_INSTALL_APP_MODE_PLUGIN;
        }
        return name;
    }

    @Override
    public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
        String prompt = buildPromptForIssue(issue);
        JavaVersionNotificationService.getInstance().openCopilotChatWithPrompt(project, prompt);
    }

    private String buildPromptForIssue(@NotNull JavaUpgradeIssue issue) {
        String packageId = issue.getPackageId();

        // JDK upgrade
        if (PACKAGE_ID_JDK.equals(packageId)) {
            return String.format(
                "Upgrade Java runtime from version %s to Java %s (LTS) using java upgrade tools by invoking #generate_upgrade_plan",
                issue.getCurrentVersion(), issue.getSuggestedVersion()
            );
        }

        // Framework upgrade (Spring Boot, Spring Framework, etc.)
        return String.format(
            "Upgrade %s from version %s to %s using java upgrade tools by invoking #generate_upgrade_plan",
            issue.getPackageDisplayName(), issue.getCurrentVersion(), issue.getSuggestedVersion()
        );
    }
}
