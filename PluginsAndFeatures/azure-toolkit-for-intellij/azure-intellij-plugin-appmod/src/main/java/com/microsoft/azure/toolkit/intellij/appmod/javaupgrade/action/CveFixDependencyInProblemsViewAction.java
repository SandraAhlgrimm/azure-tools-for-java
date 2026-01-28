/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.appmod.javaupgrade.action;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.azure.toolkit.intellij.appmod.common.AppModPluginInstaller;
import com.microsoft.azure.toolkit.intellij.appmod.javaupgrade.dao.VulnerabilityInfo;
import com.microsoft.azure.toolkit.intellij.appmod.javaupgrade.service.JavaUpgradeIssuesCache;
import com.microsoft.azure.toolkit.intellij.appmod.javaupgrade.service.JavaVersionNotificationService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.microsoft.azure.toolkit.intellij.appmod.javaupgrade.Contants.*;
import static com.microsoft.azure.toolkit.intellij.appmod.javaupgrade.dao.VulnerabilityInfo.parseVulnerabilityDescription;

/**
 * Action to fix vulnerable dependencies by opening GitHub Copilot chat with an upgrade prompt.
 * This action appears in the Problems View context menu for vulnerable dependency issues.
 */
public class CveFixDependencyInProblemsViewAction extends AnAction implements DumbAware {

    private static final String CVE_MARKER = "CVE-";

    private VulnerabilityInfo vulnerabilityInfo;
    public CveFixDependencyInProblemsViewAction() {
        super();
    }


    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        final Project project = e.getData(CommonDataKeys.PROJECT);
        if (project == null || project.isDisposed()) {
            return;
        }
        if (vulnerabilityInfo == null) {
            JavaVersionNotificationService.getInstance().openCopilotChatWithPrompt(
                project,
                SCAN_AND_RESOLVE_CVES_PROMPT
        );
        } else {
            JavaVersionNotificationService.getInstance().openCopilotChatWithPrompt(
                project,
                String.format(FIX_VULNERABLE_DEPENDENCY_WITH_COPILOT_PROMPT,
                    vulnerabilityInfo.getDependencyCoordinate())
        );
        }

    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        final Project project = e.getData(CommonDataKeys.PROJECT);
        if (project == null || project.isDisposed()) {
            e.getPresentation().setEnabledAndVisible(false);
            return;
        }
        
        // Check if we're in the Problems View context with a vulnerability
        final String description = extractProblemDescription(e);
        if (description == null) {
            e.getPresentation().setEnabledAndVisible(false);
            return;
        }
        vulnerabilityInfo = parseVulnerabilityDescription(description);
        // Also check if the file is pom.xml or build.gradle (common for dependency issues)
        final VirtualFile file = e.getData(CommonDataKeys.VIRTUAL_FILE);
        final boolean isBuildFile = isBuildFile(file);
        
        if (!isBuildFile || !isCVEIssue(description)) {
            e.getPresentation().setEnabledAndVisible(false);
            return;
        }
        final var issue = JavaUpgradeIssuesCache.getInstance(project).findCveIssue(vulnerabilityInfo.getGroupId() + ":" + vulnerabilityInfo.getArtifactId());
        if (issue == null){
            e.getPresentation().setEnabledAndVisible(false);
            return;
        }
        e.getPresentation().setEnabledAndVisible(true);
      //  e.getPresentation().setText(SCAN_AND_RESOLVE_CVES_WITH_COPILOT_DISPLAY_NAME);
        if (!AppModPluginInstaller.isAppModPluginInstalled()) {
            e.getPresentation().setText(e.getPresentation().getText() + AppModPluginInstaller.TO_INSTALL_APP_MODE_PLUGIN);
        }
    }

    private boolean isBuildFile(VirtualFile file){
        return file != null &&
                (file.getName().equals("pom.xml") || file.getName().endsWith(".gradle") || file.getName().endsWith(".gradle.kts"));
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    /**
     * Extracts the problem description from the action event context.
     */
    @Nullable
    private String extractProblemDescription(@NotNull AnActionEvent e) {

        // Approach 3: Try to get selected items from the tree
        try {
            final Object[] selectedItems = e.getData(PlatformDataKeys.SELECTED_ITEMS);
            if (selectedItems != null && selectedItems.length > 0) {
                // Concatenate all selected items' string representations
                final StringBuilder sb = new StringBuilder();
                for (Object item : selectedItems) {
                    if (item != null) {
                        sb.append(item.toString()).append(" ");
                    }
                }
                final String result = sb.toString().trim();
                if (!result.isEmpty()) {
                    return result;
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    /**
     * Extracts CVE ID from the problem description.
     */
    private boolean isCVEIssue(@NotNull String description) {
        // Pattern: CVE-YYYY-NNNNN
        final int cveIndex = description.toUpperCase().indexOf(CVE_MARKER);
        if (cveIndex >= 0) {
            return true;
        }
        return false;
    }
}
