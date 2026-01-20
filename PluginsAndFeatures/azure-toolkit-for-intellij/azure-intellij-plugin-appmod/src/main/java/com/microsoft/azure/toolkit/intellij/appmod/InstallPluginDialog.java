/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.appmod;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

/**
 * Dialog for confirming plugin installation.
 * Similar to ConfirmAndRunDialog used in AzdNode.
 */
public class InstallPluginDialog extends DialogWrapper {

    private final Project project;
    private String label;
    private Runnable onOkAction;

    public InstallPluginDialog(Project project, String title) {
        super(project, true);
        this.project = project;
        setSize(400, 150);
        setTitle(Objects.requireNonNull(title, "Title must not be null"));
        setOKButtonText("Install");
    }

    public InstallPluginDialog setLabel(String label) {
        this.label = Objects.requireNonNull(label, "Label must not be null");
        return this;
    }

    public InstallPluginDialog setOnOkAction(Runnable onOkAction) {
        this.onOkAction = onOkAction;
        return this;
    }

    @Override
    public void show() {
        init();
        super.show();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Support HTML for multi-line labels
        JLabel labelComponent;
        if (label != null && label.contains("\n")) {
            // Convert newlines to HTML breaks
            String htmlLabel = "<html>" + label.replace("\n", "<br>") + "</html>";
            labelComponent = new JLabel(htmlLabel);
        } else {
            labelComponent = new JLabel(label);
        }
        labelComponent.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(labelComponent, BorderLayout.CENTER);
        return panel;
    }

    @Override
    protected Action @NotNull [] createActions() {
        return new Action[]{getOKAction(), getCancelAction()};
    }

    @Override
    protected void doOKAction() {
        super.doOKAction();
        if (onOkAction != null) {
            onOkAction.run();
        }
    }
}
