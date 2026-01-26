/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.appmod.javaupgrade.action;

import com.intellij.analysis.problemsView.Problem;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.microsoft.azure.toolkit.intellij.appmod.javaupgrade.dao.JavaUpgradeIssue;
import com.microsoft.azure.toolkit.intellij.appmod.javaupgrade.service.JavaUpgradeIssuesCache;
import com.microsoft.azure.toolkit.intellij.appmod.javaupgrade.service.JavaVersionNotificationService;

import static com.microsoft.azure.toolkit.intellij.appmod.javaupgrade.service.JavaUpgradeIssuesDetectionService.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


/**
 * Action to fix vulnerable dependencies by opening GitHub Copilot chat with an upgrade prompt.
 * This action appears in the Problems View context menu for vulnerable dependency issues.
 */
public class UpgradeInProblemsViewAction extends AnAction implements DumbAware {

    private static final String CVE_MARKER = "CVE-";
    
    // Data key for problems in the Problems View
    private static final String PROBLEMS_VIEW_PROBLEM_KEY = "Problem";

    public UpgradeInProblemsViewAction() {
        super();
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        final Project project = e.getData(CommonDataKeys.PROJECT);
        if (project == null || project.isDisposed()) {
            return;
        }

        // Try to get issue from cache using PsiElement context
        final JavaUpgradeIssue cachedIssue = findIssueFromContext(e, project);
        if (cachedIssue != null) {
            final String prompt = buildPromptFromIssue(cachedIssue);
            JavaVersionNotificationService.getInstance().openCopilotChatWithPrompt(project, prompt);
            return;
        }

        // Fallback: Get the problem description from the context
        final String problemDescription = extractProblemDescription(e);
        
        if (problemDescription != null && !problemDescription.isEmpty()) {
            // Extract dependency info and CVE from the problem description
            final String prompt = buildUpgradePrompt(problemDescription);
            JavaVersionNotificationService.getInstance().openCopilotChatWithPrompt(project, prompt);
        } else {
            // Fallback: generic CVE fix prompt
            JavaVersionNotificationService.getInstance().openCopilotChatWithPrompt(
                project, 
                "run CVE scan for this project using java upgrade tools by invoking #validate_cves_for_java"
            );
        }
    }

    /**
     * Tries to find the JavaUpgradeIssue from the context by examining the PsiElement.
     */
    @Nullable
    private JavaUpgradeIssue findIssueFromContext(@NotNull AnActionEvent e, @NotNull Project project) {
        final JavaUpgradeIssuesCache cache = JavaUpgradeIssuesCache.getInstance(project);
        if (!cache.isInitialized()) {
            return null;
        }

        // Try to get PsiElement directly
        PsiElement element = e.getData(CommonDataKeys.PSI_ELEMENT);
        
        // If no direct element, try to get from file + offset
        if (element == null) {
            element = findElementFromProblem(e, project);
        }

        if (element == null) {
            return null;
        }

        // Navigate to find dependency/parent context
        return findIssueFromElement(element, cache);
    }

    /**
     * Finds the PsiElement from a Problem in the Problems View.
     * Uses reflection for cross-version compatibility as Problem API varies.
     */
    @Nullable
    private PsiElement findElementFromProblem(@NotNull AnActionEvent e, @NotNull Project project) {
        try {
            final Object problemData = e.getDataContext().getData(PROBLEMS_VIEW_PROBLEM_KEY);
            if (problemData instanceof Problem) {
                final Problem problem = (Problem) problemData;
                
                // Use reflection to get file - API varies across IntelliJ versions
                VirtualFile file = null;
                try {
                    java.lang.reflect.Method getFileMethod = problem.getClass().getMethod("getFile");
                    Object fileObj = getFileMethod.invoke(problem);
                    if (fileObj instanceof VirtualFile) {
                        file = (VirtualFile) fileObj;
                    }
                } catch (Exception ignored) {
                    // getFile method might not exist in this version
                    System.out.println("error" + ignored.getMessage());
                }
                
                if (file != null && file.getName().equals("pom.xml")) {
                    final PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
                    if (psiFile instanceof XmlFile) {
                        // Try to get offset from problem and find element
                        try {
                            java.lang.reflect.Method getOffsetMethod = problem.getClass().getMethod("getOffset");
                            Object offsetObj = getOffsetMethod.invoke(problem);
                            if (offsetObj instanceof Integer) {
                                int offset = (Integer) offsetObj;
                                if (offset >= 0) {
                                    return psiFile.findElementAt(offset);
                                }
                            }
                        } catch (Exception ignored) {
                            // getOffset method might not exist
                            System.out.println("error" + ignored.getMessage());
                        }
                    }
                }
            }
        } catch (Exception ignored) {
            System.out.println("error" + ignored.getMessage());

        }
        return null;
    }

    /**
     * Finds the JavaUpgradeIssue based on the XML element context.
     */
    @Nullable
    private JavaUpgradeIssue findIssueFromElement(@NotNull PsiElement element, @NotNull JavaUpgradeIssuesCache cache) {
        // Find the containing XmlTag
        XmlTag tag = findParentTag(element);
        if (tag == null) {
            return null;
        }

        // Check if this is a Java version property
        if (isJavaVersionContext(tag)) {
            return cache.getJdkIssue();
        }

        // Check if this is a dependency or parent version
        final String groupId = extractGroupId(tag);
        if (groupId != null) {
            if (groupId.equals(GROUP_ID_SPRING_BOOT)) {
                return cache.findDependencyIssue(GROUP_ID_SPRING_BOOT);
            } else if (groupId.equals(GROUP_ID_SPRING_SECURITY)) {
                return cache.findDependencyIssue(GROUP_ID_SPRING_SECURITY);
            } else if (groupId.equals(GROUP_ID_SPRING_FRAMEWORK)) {
                return cache.findDependencyIssue(GROUP_ID_SPRING_FRAMEWORK + ":");
            }
        }

        return null;
    }

    /**
     * Finds the parent XmlTag of an element.
     */
    @Nullable
    private XmlTag findParentTag(@NotNull PsiElement element) {
        PsiElement current = element;
        while (current != null) {
            if (current instanceof XmlTag) {
                return (XmlTag) current;
            }
            current = current.getParent();
        }
        return null;
    }

    /**
     * Checks if the tag is in a Java version context.
     */
    private boolean isJavaVersionContext(@NotNull XmlTag tag) {
        String tagName = tag.getName();
        
        // Check for properties like java.version, maven.compiler.source, etc.
        if ("java.version".equals(tagName) || 
            "maven.compiler.source".equals(tagName) ||
            "maven.compiler.target".equals(tagName) ||
            "maven.compiler.release".equals(tagName)) {
            XmlTag parent = tag.getParentTag();
            return parent != null && "properties".equals(parent.getName());
        }
        
        // Check for maven-compiler-plugin source/target/release
        if ("source".equals(tagName) || "target".equals(tagName) || "release".equals(tagName)) {
            XmlTag current = tag.getParentTag();
            while (current != null) {
                if ("plugin".equals(current.getName())) {
                    XmlTag artifactIdTag = current.findFirstSubTag("artifactId");
                    if (artifactIdTag != null && ARTIFACT_ID_MAVEN_COMPILER_PLUGIN.equals(artifactIdTag.getValue().getText())) {
                        return true;
                    }
                }
                current = current.getParentTag();
            }
        }
        
        return false;
    }

    /**
     * Extracts the groupId from a dependency or parent tag context.
     */
    @Nullable
    private String extractGroupId(@NotNull XmlTag tag) {
        // If we're on a version tag, look at parent (dependency or parent)
        XmlTag container = tag;
        if ("version".equals(tag.getName())) {
            container = tag.getParentTag();
        }
        
        if (container == null) {
            return null;
        }
        
        // Check if it's a dependency or parent tag
        String containerName = container.getName();
        if ("dependency".equals(containerName) || "parent".equals(containerName)) {
            XmlTag groupIdTag = container.findFirstSubTag("groupId");
            if (groupIdTag != null) {
                return groupIdTag.getValue().getText();
            }
        }
        
        return null;
    }

    /**
     * Builds a prompt from a cached JavaUpgradeIssue.
     */
    @NotNull
    private String buildPromptFromIssue(@NotNull JavaUpgradeIssue issue) {
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
        
        // Also check if the file is pom.xml or build.gradle (common for dependency issues)
        final VirtualFile file = e.getData(CommonDataKeys.VIRTUAL_FILE);
        final boolean isBuildFile = file != null &&
                (file.getName().equals("pom.xml") || file.getName().endsWith(".gradle") || file.getName().endsWith(".gradle.kts"));

        final boolean isVulnerability = isVulnerabilityDescription(description);
        
        if (!isBuildFile && !isVulnerability) {
            e.getPresentation().setEnabledAndVisible(false);
            return;
        }
        
        // Set dynamic text based on issue type detected from description
        String actionText = getDynamicActionText(description, project);
        e.getPresentation().setText(actionText);
        e.getPresentation().setEnabledAndVisible(true);
    }
    
    /**
     * Gets dynamic action text based on the problem description.
     */
    private String getDynamicActionText(@NotNull String description, @NotNull Project project) {
        // Try to detect JDK issue
        if (description.toLowerCase().contains("jdk") || 
            description.toLowerCase().contains("java runtime") ||
            description.toLowerCase().contains("java version")) {
            return "Upgrade JDK with Copilot";
        }
        
        // Try to detect Spring Boot
        if (description.toLowerCase().contains("spring boot")) {
            return "Upgrade Spring Boot with Copilot";
        }
        
        // Try to detect Spring Framework
        if (description.toLowerCase().contains("spring framework")) {
            return "Upgrade Spring Framework with Copilot";
        }
        
        // Try to detect Spring Security
        if (description.toLowerCase().contains("spring security")) {
            return "Upgrade Spring Security with Copilot";
        }
        
        // Try to get from cache if available
        final JavaUpgradeIssuesCache cache = JavaUpgradeIssuesCache.getInstance(project);
        if (cache.isInitialized()) {
            // Check for JDK issue
            if (cache.getJdkIssue() != null && description.contains(cache.getJdkIssue().getMessage())) {
                return "Upgrade JDK with Copilot";
            }
            // Check for Spring Boot
            JavaUpgradeIssue springBootIssue = cache.findDependencyIssue(GROUP_ID_SPRING_BOOT);
            if (springBootIssue != null && description.contains(springBootIssue.getMessage())) {
                return "Upgrade Spring Boot with Copilot";
            }
        }
        
        // Default text
        return "Scan and Resolve CVEs with Copilot";
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    /**
     * Checks if the description indicates a vulnerability.
     */
    private boolean isVulnerabilityDescription(@NotNull String description) {
        final String lowerDescription = description.toLowerCase();
        return lowerDescription.contains("vulnerable") ||
               lowerDescription.contains("cve-") ||
               lowerDescription.contains("security") ||
               lowerDescription.contains("vulnerability");
    }

    /**
     * Extracts the problem description from the action event context.
     */
    @Nullable
    private String extractProblemDescription(@NotNull AnActionEvent e) {
        // Try multiple approaches to get the problem description
        
        // Approach 1: Try to get Problem object directly from Problems View
        try {
            final Object problemData = e.getDataContext().getData(PROBLEMS_VIEW_PROBLEM_KEY);
            if (problemData instanceof Problem) {
                final Problem problem = (Problem) problemData;
                final String text = problem.getText();
                if (text != null && !text.isEmpty()) {
                    return text;
                }
            }
        } catch (Exception ignored) {
            // Problem class might not be available
        }
        
        // Approach 2: Try "problem.description" data key
        try {
            @SuppressWarnings("deprecation")
            final Object data = e.getDataContext().getData("problem.description");
            if (data instanceof String && !((String) data).isEmpty()) {
                return (String) data;
            }
        } catch (Exception ignored) {
        }
        
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
        
        // Approach 4: Try SELECTED_ITEM
        try {
            final Object selectedItem = e.getData(PlatformDataKeys.SELECTED_ITEM);
            if (selectedItem != null) {
                final String text = selectedItem.toString();
                if (!text.isEmpty()) {
                    return text;
                }
            }
        } catch (Exception ignored) {
        }
        
        // Approach 5: Try getting from context component
        try {
            final java.awt.Component component = e.getData(PlatformDataKeys.CONTEXT_COMPONENT);
            if (component instanceof javax.swing.JTree) {
                final javax.swing.JTree tree = (javax.swing.JTree) component;
                final javax.swing.tree.TreePath[] paths = tree.getSelectionPaths();
                if (paths != null && paths.length > 0) {
                    final StringBuilder sb = new StringBuilder();
                    for (javax.swing.tree.TreePath path : paths) {
                        final Object lastComponent = path.getLastPathComponent();
                        if (lastComponent != null) {
                            sb.append(lastComponent.toString()).append(" ");
                        }
                    }
                    final String result = sb.toString().trim();
                    if (!result.isEmpty()) {
                        return result;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        
        // Approach 6: Try getting selected text from editor
        try {
            final com.intellij.openapi.editor.Editor editor = e.getData(CommonDataKeys.EDITOR);
            if (editor != null && editor.getSelectionModel().hasSelection()) {
                final String selectedText = editor.getSelectionModel().getSelectedText();
                if (selectedText != null && !selectedText.isEmpty()) {
                    return selectedText;
                }
            }
        } catch (Exception ignored) {
        }
        
        return null;
    }

    /**
     * Builds an upgrade prompt based on the problem description.
     */
    private String buildUpgradePrompt(@NotNull String problemDescription) {

        // Extract dependency coordinates if present
        final String dependency = extractDependencyCoordinates(problemDescription);

        if (problemDescription != null) {
            // Try to detect JDK issue
            if (problemDescription.toLowerCase().contains("jdk") ||
                    problemDescription.toLowerCase().contains("java runtime") ||
                    problemDescription.toLowerCase().contains("java version")) {
                return "upgrade java runtime to Java " + MATURE_JAVA_LTS_VERSION + " (LTS) using java upgrade tools by invoking #generate_upgrade_plan";
            }

            // Try to detect Spring Boot
            if (problemDescription.toLowerCase().contains("spring boot") || problemDescription.toLowerCase().contains("spring framework") || problemDescription.toLowerCase().contains("spring security")) {
                return "upgrade java framework dependencies of this project to latest LTS version using java upgrade tools by invoking #generate_upgrade_plan";
            }

        }
        // Default text
        return "run CVE scan for this project using java upgrade tools by invoking #validate_cves_for_java";
    }

    /**
     * Extracts CVE ID from the problem description.
     */
    private String extractCVEId(@NotNull String description) {
        // Pattern: CVE-YYYY-NNNNN
        final int cveIndex = description.toUpperCase().indexOf(CVE_MARKER);
        if (cveIndex >= 0) {
            final int endIndex = findCVEEndIndex(description, cveIndex);
            if (endIndex > cveIndex) {
                return description.substring(cveIndex, endIndex);
            }
        }
        return null;
    }

    /**
     * Finds the end index of a CVE ID in the description.
     */
    private int findCVEEndIndex(@NotNull String description, int startIndex) {
        int index = startIndex + CVE_MARKER.length();
        // Skip year (4 digits)
        while (index < description.length() && Character.isDigit(description.charAt(index))) {
            index++;
        }
        // Skip separator
        if (index < description.length() && description.charAt(index) == '-') {
            index++;
        }
        // Skip ID number
        while (index < description.length() && Character.isDigit(description.charAt(index))) {
            index++;
        }
        return index;
    }

    /**
     * Extracts Maven dependency coordinates from the problem description.
     * Looks for patterns like groupId:artifactId:version
     */
    private String extractDependencyCoordinates(@NotNull String description) {
        // Look for Maven coordinate pattern: groupId:artifactId:version
        // Common patterns in vulnerability reports
        final String[] patterns = {
            "maven:", // maven:groupId:artifactId:version
            "dependency " // dependency groupId:artifactId
        };
        
        for (String pattern : patterns) {
            final int index = description.toLowerCase().indexOf(pattern);
            if (index >= 0) {
                return extractCoordinatesAfterPattern(description, index + pattern.length());
            }
        }
        
        // Try to find standalone coordinate pattern (e.g., org.example:artifact:1.0.0)
        return findStandaloneCoordinates(description);
    }

    /**
     * Extracts coordinates after a known pattern.
     */
    private String extractCoordinatesAfterPattern(@NotNull String description, int startIndex) {
        final StringBuilder coords = new StringBuilder();
        int colonCount = 0;
        
        for (int i = startIndex; i < description.length(); i++) {
            final char c = description.charAt(i);
            if (Character.isLetterOrDigit(c) || c == '.' || c == '-' || c == '_') {
                coords.append(c);
            } else if (c == ':' && colonCount < 2) {
                coords.append(c);
                colonCount++;
            } else if (!coords.isEmpty()) {
                break;
            }
        }
        
        final String result = coords.toString();
        return result.contains(":") ? result : null;
    }

    /**
     * Finds standalone Maven coordinates in the description.
     */
    private String findStandaloneCoordinates(@NotNull String description) {
        // Simple heuristic: look for pattern like "org.xxx:xxx" or "com.xxx:xxx"
        final String[] prefixes = {"org.", "com.", "io.", "net."};
        
        for (String prefix : prefixes) {
            int index = description.indexOf(prefix);
            while (index >= 0) {
                final String coords = extractCoordinatesAfterPattern(description, index);
                if (coords != null && coords.contains(":")) {
                    return coords;
                }
                index = description.indexOf(prefix, index + 1);
            }
        }
        
        return null;
    }
}
