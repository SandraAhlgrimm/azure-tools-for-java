/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.appmod;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;

import javax.annotation.Nonnull;

/**
 * Helper class for opening the App Modernization Panel.
 */
public final class AppModPanelHelper {
    
    public static final String TOOL_WINDOW_ID = "GitHub Copilot app modernization";
    
    private AppModPanelHelper() {
        // Utility class
    }
    
    /**
     * Opens the App Modernization Panel tool window.
     * 
     * @param project the current project
     */
    public static void openAppModPanel(@Nonnull Project project) {
        final ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
        final ToolWindow toolWindow = toolWindowManager.getToolWindow(TOOL_WINDOW_ID);
        
        if (toolWindow != null) {
            toolWindow.show();
        } else {
            AzureMessager.getMessager().warning("App Modernization Panel is not available. Please ensure the GitHub Copilot App Modernization plugin is installed and enabled.");
        }
    }
}
