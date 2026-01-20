/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.appmod;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * Dialog for prompting user after plugin installation.
 * Can be configured to show restart options or just an info message.
 */
public class RestartIdeDialog extends DialogWrapper {

    private final String message;
    private boolean showRestartOption = true;

    public RestartIdeDialog(Project project, String title, String message) {
        super(project, true);
        this.message = message;
        setSize(450, 150);
        setTitle(title);
        setOKButtonText("Restart Now");
        setCancelButtonText("Restart Later");
        init();
    }

    /**
     * Sets whether to show restart option or just an info dialog.
     * @param showRestartOption true for restart dialog, false for info-only dialog
     */
    public RestartIdeDialog setShowRestartOption(boolean showRestartOption) {
        this.showRestartOption = showRestartOption;
        if (!showRestartOption) {
            setOKButtonText("Got it");
        }
        return this;
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Support HTML for multi-line messages
        JLabel labelComponent;
        if (message != null && message.contains("\n")) {
            String htmlMessage = "<html>" + message.replace("\n", "<br>") + "</html>";
            labelComponent = new JLabel(htmlMessage);
        } else {
            labelComponent = new JLabel(message);
        }
        labelComponent.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(labelComponent, BorderLayout.CENTER);
        return panel;
    }

    @Override
    protected Action @NotNull [] createActions() {
        if (showRestartOption) {
            return new Action[]{getOKAction(), getCancelAction()};
        } else {
            return new Action[]{getOKAction()};
        }
    }

    @Override
    protected void doOKAction() {
        super.doOKAction();
        if (showRestartOption) {
            ApplicationManager.getApplication().restart();
        }
    }
}
