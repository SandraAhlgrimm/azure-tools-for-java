/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.appmod.javaupgrade;

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import com.microsoft.azure.toolkit.intellij.appmod.javaupgrade.dao.JavaUpgradeIssue;
import com.microsoft.azure.toolkit.intellij.appmod.javaupgrade.service.JavaUpgradeIssuesDetectionService;
import com.microsoft.azure.toolkit.intellij.appmod.javaupgrade.service.JavaUpgradeIssuesCache;
import com.microsoft.azure.toolkit.intellij.appmod.javaupgrade.service.JavaVersionNotificationService;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.time.Duration;
import java.util.List;

/**
 * Startup activity that detects outdated JDK and framework versions when a project is opened.
 * This runs after the project is fully loaded and shows notifications for any detected issues.
 */
public class JavaUpgradeCheckStartupActivity implements ProjectActivity, DumbAware {
    
    // Additional delay after smart mode to ensure Maven/Gradle sync is complete
    private static final long POST_INDEXING_DELAY_SECONDS = 3;
    
    @Override
    public Object execute(@Nonnull Project project, @Nonnull Continuation<? super Unit> continuation) {
        // Wait for indexing to complete before running the check
        DumbService.getInstance(project).runWhenSmart(() -> {
            // Add a small delay after smart mode to ensure Maven/Gradle sync is done
            Mono.delay(Duration.ofSeconds(POST_INDEXING_DELAY_SECONDS))
                .subscribe(
                    next -> {
                        if (project.isDisposed()) {
                            return;
                        }
                        performJavaUpgradeCheck(project);
                    },
                    error -> { /* Error during Java upgrade check startup */ }
                );
        });
        
        return null;
    }
    
    /**
     * Performs the jdk version, framework version and CVE issue check and shows notifications for any issues found.
     */
    private void performJavaUpgradeCheck(@Nonnull Project project) {
        try {
            // Run the analysis in a background thread
            AzureTaskManager.getInstance().runInBackground("Checking Java upgrade issues", () -> {
                if (project.isDisposed()) {
                    return;
                }
                
                // Refresh the cache (this populates JDK and dependency issues for use by inspections)
                final JavaUpgradeIssuesCache cache = JavaUpgradeIssuesCache.getInstance(project);
                cache.refresh();
                
                // Get all issues including CVEs
                final JavaUpgradeIssuesDetectionService detectionService = JavaUpgradeIssuesDetectionService.getInstance();
                final List<JavaUpgradeIssue> allIssues = new java.util.ArrayList<>();
                allIssues.addAll(cache.getJdkIssues());
                allIssues.addAll(cache.getDependencyIssues());
                allIssues.addAll(detectionService.getCVEIssues(project));
                
                // Update UI on the main thread
                AzureTaskManager.getInstance().runLater(() -> {
                    if (project.isDisposed()) {
                        return;
                    }
                    
                    // Restart code analysis to refresh inspections in open editors
                    // This ensures wavy underlines appear for JDK/framework issues
                    DaemonCodeAnalyzer.getInstance(project).restart();
                    
                    // Show notifications if there are issues
                    if (!allIssues.isEmpty()) {
                        final JavaVersionNotificationService notificationService = JavaVersionNotificationService.getInstance();
                        notificationService.showNotifications(project, allIssues);
                    }
                });
            });
            
        } catch (Exception e) {
            // Error performing Java version check
        }
    }
}
