package com.microsoft.azure.toolkit.intellij.explorer.azd;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.microsoft.azure.toolkit.intellij.common.TerminalUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class ConfirmAndRunDialog extends DialogWrapper {

    private final Project project;
    private final String label;
    private final String command;

    public ConfirmAndRunDialog(Project project, String title, String label, String command) {
        super(true);
        this.project = project;
        setTitle(title);
        setOKButtonText("Run");
        this.label = label;
        init();
        setSize(300, 150);
        this.command = command;
    }

    public void setOkButtonText(String okButtonText) {
        setOKButtonText(okButtonText);
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        JLabel label = new JLabel(this.label);
        label.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(label, BorderLayout.CENTER);
        return panel;
    }

    @Override
    protected Action @NotNull [] createActions() {
        return new Action[]{getOKAction(), getCancelAction()};
    }

    @Override
    protected void doOKAction() {
        super.doOKAction();
        TerminalUtils.executeInTerminal(project, command, "azd");
    }
}
