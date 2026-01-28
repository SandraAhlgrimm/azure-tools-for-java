/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.appmod.javaupgrade.action;

import com.intellij.codeInsight.intention.HighPriorityAction;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.codeInspection.util.IntentionName;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import com.microsoft.azure.toolkit.intellij.appmod.common.AppModPluginInstaller;
import com.microsoft.azure.toolkit.intellij.appmod.javaupgrade.dao.VulnerabilityInfo;
import com.microsoft.azure.toolkit.intellij.appmod.javaupgrade.service.JavaUpgradeIssuesCache;
import com.microsoft.azure.toolkit.intellij.appmod.javaupgrade.service.JavaVersionNotificationService;

import org.jetbrains.annotations.NotNull;

import static com.microsoft.azure.toolkit.intellij.appmod.javaupgrade.Contants.*;

/**
 * Intention action to fix CVE vulnerabilities in dependencies using GitHub Copilot.
 * This action appears in the editor's quick-fix popup (More actions...) for pom.xml files(yellow wavy dependency with CVE issue)
 * when a vulnerable dependency with CVE issues is detected.
 * 
 * Implements HighPriorityAction to appear at the top of the quick-fix list.
 */
public class CveFixDependencyIntentionAction implements IntentionAction, HighPriorityAction {
    
    // Cached dependency info from isAvailable() for use in getText()
    private VulnerabilityInfo vulnerabilityInfo;

    @Override
    public @IntentionName @NotNull String getText() {
        if (!AppModPluginInstaller.isAppModPluginInstalled()) {
            return  FIX_VULNERABLE_DEPENDENCY_WITH_COPILOT_DISPLAY_NAME + AppModPluginInstaller.TO_INSTALL_APP_MODE_PLUGIN;
        }
        return FIX_VULNERABLE_DEPENDENCY_WITH_COPILOT_DISPLAY_NAME;
    }

    @Override
    public @IntentionFamilyName @NotNull String getFamilyName() {
        return "Azure Toolkit";
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
        // Reset cached values
        vulnerabilityInfo = null;
        
        if (file == null || editor == null) {
            return false;
        }
        
        // Only available for pom.xml files
        final String fileName = file.getName();
        if (!fileName.equals("pom.xml")) {
            return false;
        }
        
        try {
            final int offset = editor.getCaretModel().getOffset();
            final String documentText = editor.getDocument().getText();
            
            // Try to extract dependency info - only show if cursor is within a <dependency> block
            final int dependencyStart = findDependencyStart(documentText, offset);
            final int dependencyEnd = findDependencyEnd(documentText, offset);
            
            if (dependencyStart >= 0 && dependencyEnd > dependencyStart) {
                final String dependencyBlock = documentText.substring(dependencyStart, dependencyEnd);
                String cachedGroupId = extractXmlValue(dependencyBlock, "groupId");
                String cachedArtifactId = extractXmlValue(dependencyBlock, "artifactId");
                String cachedVersion = extractXmlValue(dependencyBlock, "version");
                vulnerabilityInfo = VulnerabilityInfo.builder().groupId(cachedGroupId).artifactId(cachedArtifactId).version(cachedVersion).build();
                // Only show if we have valid dependency info (not for parent/plugin sections)
                if(cachedGroupId != null && cachedArtifactId != null) {
                    //if the artifact is in the cached cve issues, show the intention
                    final var issue = JavaUpgradeIssuesCache.getInstance(project).findCveIssue(cachedGroupId + ":" + cachedArtifactId);
                    return issue != null;
                }
            }
        } catch (Exception e) {
            // Ignore and return false
        }
        
        return false;
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
        if (file == null || editor == null) {
            return;
        }
        
        // Try to extract dependency information from the current context
        final String prompt = buildPromptFromContext(editor, file);
        JavaVersionNotificationService.getInstance().openCopilotChatWithPrompt(project, prompt);
    }

    /**
     * Builds a prompt based on the current editor context.
     */
    private String buildPromptFromContext(@NotNull Editor editor, @NotNull PsiFile file) {
        return String.format(FIX_VULNERABLE_DEPENDENCY_WITH_COPILOT_PROMPT,
                vulnerabilityInfo.getDependencyCoordinate());
    }

    /**
     * Finds the start of the dependency block containing the given offset.
     */
    private int findDependencyStart(@NotNull String text, int offset) {
        // Look for <dependency> tag before the offset
        int searchStart = Math.max(0, offset - 500);
        String searchArea = text.substring(searchStart, offset);
        int lastDependency = searchArea.lastIndexOf("<dependency>");
        if (lastDependency >= 0) {
            return searchStart + lastDependency;
        }
        return -1;
    }

    /**
     * Finds the end of the dependency block containing the given offset.
     */
    private int findDependencyEnd(@NotNull String text, int offset) {
        // Look for </dependency> tag after the offset
        int searchEnd = Math.min(text.length(), offset + 500);
        String searchArea = text.substring(offset, searchEnd);
        int endDependency = searchArea.indexOf("</dependency>");
        if (endDependency >= 0) {
            return offset + endDependency + "</dependency>".length();
        }
        return -1;
    }

    /**
     * Extracts a value from an XML tag.
     */
    private String extractXmlValue(@NotNull String xml, @NotNull String tagName) {
        final String startTag = "<" + tagName + ">";
        final String endTag = "</" + tagName + ">";
        
        int start = xml.indexOf(startTag);
        if (start < 0) {
            return null;
        }
        start += startTag.length();
        
        int end = xml.indexOf(endTag, start);
        if (end < 0) {
            return null;
        }
        
        return xml.substring(start, end).trim();
    }

    @Override
    public boolean startInWriteAction() {
        return false;
    }
}
