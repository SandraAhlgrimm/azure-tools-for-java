/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.appmod.javaupgrade.service;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.appmod.javaupgrade.dao.JavaUpgradeIssue;

import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Project-level cache for Java upgrade issues.
 * Issues are computed once at startup and cached to avoid repeated expensive scans
 * during inspection runs. The cache can be invalidated when project model changes.
 */
@Service(Service.Level.PROJECT)
public final class JavaUpgradeIssuesCache implements Disposable {

    private final Project project;
    private final AtomicReference<List<JavaUpgradeIssue>> jdkIssuesCache = new AtomicReference<>();
    private final AtomicReference<List<JavaUpgradeIssue>> dependencyIssuesCache = new AtomicReference<>();
    private final AtomicReference<List<JavaUpgradeIssue>> cvesIssuesCache = new AtomicReference<>();
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    public JavaUpgradeIssuesCache(@NotNull Project project) {
        this.project = project;
    }

    public static JavaUpgradeIssuesCache getInstance(@NotNull Project project) {
        return project.getService(JavaUpgradeIssuesCache.class);
    }

    /**
     * Gets cached JDK issues. Returns empty list if not yet initialized.
     */
    @Nonnull
    public List<JavaUpgradeIssue> getJdkIssues() {
        List<JavaUpgradeIssue> cached = jdkIssuesCache.get();
        return cached != null ? cached : Collections.emptyList();
    }

    /**
     * Gets cached dependency issues. Returns empty list if not yet initialized.
     */
    @Nonnull
    public List<JavaUpgradeIssue> getDependencyIssues() {
        List<JavaUpgradeIssue> cached = dependencyIssuesCache.get();
        return cached != null ? cached : Collections.emptyList();
    }

    /**
     * Gets cached CVE issues. Returns empty list if not yet initialized.
     */
    @Nonnull
    public List<JavaUpgradeIssue> getCveIssues() {
        List<JavaUpgradeIssue> cached = cvesIssuesCache.get();
        return cached != null ? cached : Collections.emptyList();
    }

    /**
     * Finds a specific CVE issue by package ID prefix.
     */
    @Nullable
    public JavaUpgradeIssue findCveIssue(@Nonnull String packageIdPrefix) {
        return getCveIssues().stream()
            .filter(i -> i.getPackageId().startsWith(packageIdPrefix))
            .findFirst()
            .orElse(null);
    }
    /**
     * Finds the first issue matching a package ID prefix.
     * @deprecated Use {@link #findDependencyIssues(String)} to handle multiple dependencies with the same groupId.
     */
    @Deprecated
    @Nullable
    public JavaUpgradeIssue findDependencyIssue(@Nonnull String packageIdPrefix) {
        return getDependencyIssues().stream()
            .filter(i -> i.getPackageId().startsWith(packageIdPrefix))
            .findFirst()
            .orElse(null);
    }

    /**
     * Finds all issues matching a package ID prefix (groupId).
     * This handles the case where multiple dependencies share the same groupId.
     */
    @Nonnull
    public List<JavaUpgradeIssue> findDependencyIssues(@Nonnull String packageIdPrefix) {
        return getDependencyIssues().stream()
            .filter(i -> i.getPackageId().startsWith(packageIdPrefix))
            .toList();
    }

    /**
     * Gets the first JDK issue if present.
     */
    @Nullable
    public JavaUpgradeIssue getJdkIssue() {
        List<JavaUpgradeIssue> issues = getJdkIssues();
        return issues.isEmpty() ? null : issues.get(0);
    }

    /**
     * Checks if the cache has been initialized.
     */
    public boolean isInitialized() {
        return initialized.get();
    }

    /**
     * Refreshes the cache by re-scanning the project.
     * This should be called at project startup and when the project model changes.
     */
    public void refresh() {
        if (project.isDisposed()) {
            return;
        }

        final JavaUpgradeIssuesDetectionService detectionService = JavaUpgradeIssuesDetectionService.getInstance();
        
        // Scan for issues
        List<JavaUpgradeIssue> jdkIssues = detectionService.getJavaIssues(project);
        List<JavaUpgradeIssue> dependencyIssues = detectionService.getDependencyIssues(project);
        List<JavaUpgradeIssue> cveIssues = detectionService.getCVEIssues(project);
        // Update cache
        jdkIssuesCache.set(Collections.unmodifiableList(jdkIssues));
        dependencyIssuesCache.set(Collections.unmodifiableList(dependencyIssues));
        cvesIssuesCache.set(Collections.unmodifiableList(cveIssues));
        initialized.set(true);
    }

    /**
     * Invalidates the cache, forcing a refresh on next access.
     */
    public void invalidate() {
        jdkIssuesCache.set(null);
        dependencyIssuesCache.set(null);
        cvesIssuesCache.set(null);
        initialized.set(false);
    }

    @Override
    public void dispose() {
        invalidate();
    }
}
