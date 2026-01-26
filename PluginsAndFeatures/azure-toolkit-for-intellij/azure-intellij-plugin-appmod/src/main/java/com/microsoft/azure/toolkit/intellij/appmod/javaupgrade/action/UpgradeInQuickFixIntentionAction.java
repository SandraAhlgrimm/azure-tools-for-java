/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.appmod.javaupgrade.action;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInsight.intention.PriorityAction;
import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.codeInspection.util.IntentionName;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import com.microsoft.azure.toolkit.intellij.appmod.javaupgrade.service.JavaVersionNotificationService;

import org.jetbrains.annotations.NotNull;

/**
 * Intention action to upgrade vulnerable dependencies using GitHub Copilot.
 * This action appears in the editor's quick-fix popup (More actions...) for pom.xml files
 * when a vulnerable dependency is detected.
 */
public class UpgradeInQuickFixIntentionAction implements IntentionAction, PriorityAction {

    private static final String DEFAULT_TEXT = "Scan and Resolve CVEs by Copilot";
    
    // Cached dependency info from isAvailable() for use in getText()
    private String cachedGroupId;
    private String cachedArtifactId;

    @Override
    public @IntentionName @NotNull String getText() {
        // Return dynamic text based on cached dependency info
        if (cachedGroupId != null) {
            // Use a friendly display name for known dependencies
            String displayName = getDisplayName(cachedGroupId, cachedArtifactId);
            return displayName;
        }
        return DEFAULT_TEXT;
    }
    
    /**
     * Gets a friendly display name for a dependency.
     */
    private String getDisplayName(String groupId, String artifactId) {
        return "Scan and Resolve CVEs by Copilot";
//        if (groupId == null) {
//            return "Dependency";
//        }
//
//        // Map known groupIds to friendly names
//        if (groupId.equals("org.springframework.boot")) {
//            return "Upgrade Spring Boot with Copilot";
//        } else if (groupId.equals("org.springframework.security")) {
//            return "Upgrade Spring Security with Copilot";
//        } else if (groupId.equals("org.springframework")) {
//            return "Spring Framework";
//        } else if (groupId.startsWith("org.springframework")) {
//            return "Spring " + (artifactId != null ? artifactId : "dependency");
//        }
//
//        // For other dependencies, use groupId:artifactId or just groupId
//        if (artifactId != null) {
//            return groupId + ":" + artifactId;
//        }
//        return groupId;
    }

    @Override
    public @IntentionFamilyName @NotNull String getFamilyName() {
        return "Azure Toolkit";
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
        // Reset cached values
        cachedGroupId = null;
        cachedArtifactId = null;
        
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
                cachedGroupId = extractXmlValue(dependencyBlock, "groupId");
                cachedArtifactId = extractXmlValue(dependencyBlock, "artifactId");
                
                // Only show if we have valid dependency info (not for parent/plugin sections)
                return cachedGroupId != null && cachedArtifactId != null;
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
//        try {
//            final int offset = editor.getCaretModel().getOffset();
//            final String documentText = editor.getDocument().getText();
//
//            // Find the dependency block around the cursor
//            final int dependencyStart = findDependencyStart(documentText, offset);
//            final int dependencyEnd = findDependencyEnd(documentText, offset);
//
//            if (dependencyStart >= 0 && dependencyEnd > dependencyStart) {
//                final String dependencyBlock = documentText.substring(dependencyStart, dependencyEnd);
//
//                // Extract groupId and artifactId
//                final String groupId = extractXmlValue(dependencyBlock, "groupId");
//                final String artifactId = extractXmlValue(dependencyBlock, "artifactId");
//                final String version = extractXmlValue(dependencyBlock, "version");
//
//                if (groupId != null && artifactId != null) {
//                    final StringBuilder prompt = new StringBuilder();
//                    prompt.append("Fix security vulnerabilities in ");
//                    prompt.append(groupId).append(":").append(artifactId);
//                    if (version != null) {
//                        prompt.append(":").append(version);
//                    }
//                    prompt.append(" by using #validate_cves_for_java");
//                    return prompt.toString();
//                }
//            }
//        } catch (Exception e) {
//            // Fall back to generic prompt
//        }
        
        return "run CVE scan for this project using java upgrade tools by invoking #validate_cves_for_java";
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

    @Override
    public @NotNull Priority getPriority() {
        return Priority.NORMAL;
    }
}
