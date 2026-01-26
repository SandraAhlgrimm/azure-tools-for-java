/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.appmod.javaupgrade.inspection;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import com.intellij.psi.XmlElementVisitor;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.microsoft.azure.toolkit.intellij.appmod.javaupgrade.dao.JavaUpgradeIssue;
import com.microsoft.azure.toolkit.intellij.appmod.javaupgrade.service.JavaUpgradeIssuesCache;
import com.microsoft.azure.toolkit.intellij.appmod.javaupgrade.service.JavaVersionNotificationService;

import static com.microsoft.azure.toolkit.intellij.appmod.javaupgrade.service.JavaUpgradeIssuesDetectionService.*;

import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

/**
 * Inspection that displays Java upgrade issues detected by JavaUpgradeDetectionService.
 * Shows JDK version and framework version issues in pom.xml files with wavy underlines.
 * 
 * Note: Issues are cached at project startup via JavaUpgradeIssueCache to avoid
 * repeated expensive scans during inspection runs.
 */
public class JavaUpgradeIssuesInspection extends LocalInspectionTool {

    private static final Logger LOG = Logger.getInstance(JavaUpgradeIssuesInspection.class);

    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        final PsiFile file = holder.getFile();

        // Only process pom.xml files
        if (!(file instanceof XmlFile) || !file.getName().equals("pom.xml")) {
            return PsiElementVisitor.EMPTY_VISITOR;
        }

        final Project project = holder.getProject();
        final JavaUpgradeIssuesCache cache = JavaUpgradeIssuesCache.getInstance(project);

        // Skip if cache is not yet initialized (will show issues after startup completes)
        if (!cache.isInitialized()) {
            return PsiElementVisitor.EMPTY_VISITOR;
        }

        // Get cached issues (computed once at project startup)
        final JavaUpgradeIssue jdkIssue = cache.getJdkIssue();
        final JavaUpgradeIssue springBootIssue = cache.findDependencyIssue(GROUP_ID_SPRING_BOOT);
        final JavaUpgradeIssue springFrameworkIssue = cache.findDependencyIssue(GROUP_ID_SPRING_FRAMEWORK + ":");
        final JavaUpgradeIssue springSecurityIssue = cache.findDependencyIssue(GROUP_ID_SPRING_SECURITY);

        // Debug logging
        LOG.info("JavaUpgradeIssuesInspection: Cache initialized=" + cache.isInitialized() + 
                 ", jdkIssue=" + (jdkIssue != null) + 
                 ", springBootIssue=" + (springBootIssue != null ? springBootIssue.getCurrentVersion() : "null") +
                 ", dependencyIssues=" + cache.getDependencyIssues().size());

        return new XmlElementVisitor() {
            @Override
            public void visitXmlTag(@NotNull XmlTag tag) {
                super.visitXmlTag(tag);

                // Check for JDK version tags
                if (jdkIssue != null) {
                    if (isJavaVersionProperty(tag) || isCompilerPluginVersionTag(tag)) {
                        registerProblem(holder, tag, jdkIssue);
                    }
                }

                // Check for Spring Boot parent version
                if (springBootIssue != null && isSpringBootParentVersion(tag)) {
                    registerProblem(holder, tag, springBootIssue);
                }

                // Check for Spring Framework dependency version
                if (springFrameworkIssue != null && isSpringFrameworkDependencyVersion(tag)) {
                    registerProblem(holder, tag, springFrameworkIssue);
                }

                // Check for Spring Boot dependency version (when not using parent)
                if (springBootIssue != null && isSpringBootDependencyVersion(tag)) {
                    registerProblem(holder, tag, springBootIssue);
                }

                // Check for Spring Security dependency version
                if (springSecurityIssue != null && isSpringSecurityDependencyVersion(tag)) {
                    registerProblem(holder, tag, springSecurityIssue);
                }
            }
        };
    }

    private void registerProblem(@NotNull ProblemsHolder holder, @NotNull XmlTag tag, @NotNull JavaUpgradeIssue issue) {
        // ProblemHighlightType highlightType = issue.getSeverity() == JavaUpgradeIssue.Severity.CRITICAL
        //     ? ProblemHighlightType.ERROR
        //     : ProblemHighlightType.WARNING;

        holder.registerProblem(
            tag,
            issue.getMessage(),
            ProblemHighlightType.WARNING,
            new UpgradeWithCopilotQuickFix(issue)
        );
    }

    /**
     * Checks if the tag is a Java version property (java.version, maven.compiler.source, maven.compiler.target).
     */
    private boolean isJavaVersionProperty(@NotNull XmlTag tag) {
        String tagName = tag.getName();
        XmlTag parent = tag.getParentTag();
        
        if (parent == null || !"properties".equals(parent.getName())) {
            return false;
        }

        return "java.version".equals(tagName) ||
               "maven.compiler.source".equals(tagName) ||
               "maven.compiler.target".equals(tagName) ||
               "maven.compiler.release".equals(tagName);
    }

    /**
     * Checks if the tag is a version tag inside maven-compiler-plugin configuration.
     */
    private boolean isCompilerPluginVersionTag(@NotNull XmlTag tag) {
        String tagName = tag.getName();
        if (!"source".equals(tagName) && !"target".equals(tagName) && !"release".equals(tagName)) {
            return false;
        }

        // Check if we're inside maven-compiler-plugin
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
        return false;
    }

    /**
     * Checks if the tag is the version tag inside a Spring Boot parent declaration.
     */
    private boolean isSpringBootParentVersion(@NotNull XmlTag tag) {
        if (!"version".equals(tag.getName())) {
            return false;
        }

        XmlTag parent = tag.getParentTag();
        if (parent == null || !"parent".equals(parent.getName())) {
            return false;
        }

        XmlTag groupIdTag = parent.findFirstSubTag("groupId");
        XmlTag artifactIdTag = parent.findFirstSubTag("artifactId");

        return groupIdTag != null && GROUP_ID_SPRING_BOOT.equals(groupIdTag.getValue().getText()) &&
               artifactIdTag != null && ARTIFACT_ID_SPRING_BOOT_STARTER_PARENT.equals(artifactIdTag.getValue().getText());
    }

    /**
     * Checks if the tag is a version tag inside a Spring Boot dependency.
     */
    private boolean isSpringBootDependencyVersion(@NotNull XmlTag tag) {
        if (!"version".equals(tag.getName())) {
            return false;
        }

        XmlTag dependency = tag.getParentTag();
        if (dependency == null || !"dependency".equals(dependency.getName())) {
            return false;
        }

        XmlTag groupIdTag = dependency.findFirstSubTag("groupId");
        return groupIdTag != null && GROUP_ID_SPRING_BOOT.equals(groupIdTag.getValue().getText());
    }

    /**
     * Checks if the tag is a version tag inside a Spring Framework dependency.
     */
    private boolean isSpringFrameworkDependencyVersion(@NotNull XmlTag tag) {
        if (!"version".equals(tag.getName())) {
            return false;
        }

        XmlTag dependency = tag.getParentTag();
        if (dependency == null || !"dependency".equals(dependency.getName())) {
            return false;
        }

        XmlTag groupIdTag = dependency.findFirstSubTag("groupId");
        return groupIdTag != null && GROUP_ID_SPRING_FRAMEWORK.equals(groupIdTag.getValue().getText());
    }

    /**
     * Checks if the tag is a version tag inside a Spring Security dependency.
     */
    private boolean isSpringSecurityDependencyVersion(@NotNull XmlTag tag) {
        if (!"version".equals(tag.getName())) {
            return false;
        }

        XmlTag dependency = tag.getParentTag();
        if (dependency == null || !"dependency".equals(dependency.getName())) {
            return false;
        }

        XmlTag groupIdTag = dependency.findFirstSubTag("groupId");
        return groupIdTag != null && GROUP_ID_SPRING_SECURITY.equals(groupIdTag.getValue().getText());
    }

    /**
     * Quick fix to upgrade using Copilot based on the issue type.
     */
    private static class UpgradeWithCopilotQuickFix implements LocalQuickFix {
        private final JavaUpgradeIssue issue;

        public UpgradeWithCopilotQuickFix(@NotNull JavaUpgradeIssue issue) {
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
            return "Upgrade " + issue.getPackageDisplayName() + " with Copilot";
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
}
