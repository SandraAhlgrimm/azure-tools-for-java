package com.microsoft.azure.toolkit.intellij.explorer.azd;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.terminal.ui.TerminalWidget;
import com.microsoft.azure.toolkit.intellij.common.TerminalUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.Locale;
import java.util.Objects;

public class ConfirmAndRunDialog extends DialogWrapper {

    private final Project project;
    private final String label;
    private final String command;
    private final String eventName;

    public ConfirmAndRunDialog(Project project, String title, String label, String command) {
        super(true);
        this.project = project;
        setTitle(Objects.requireNonNull(title, "Title must not be null"));
        setOKButtonText("Run");
        this.label = label;
        init();
        setSize(300, 150);
        this.command = command;
        this.eventName = title.toLowerCase(Locale.ROOT).replace(" ", "-");
        AzdUtils.logTelemetryEvent("azd-" + eventName);
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
        AzdUtils.logTelemetryEvent("azd-" + eventName + "-ok");
        super.doOKAction();
        final TerminalWidget azdTerminal = TerminalUtils.getOrCreateTerminalWidget(project, null, "azd");
        TerminalUtils.executeInTerminal(azdTerminal, command);
    }

    @Override
    public void doCancelAction() {
        AzdUtils.logTelemetryEvent("azd-" + eventName + "-cancel");
        super.doCancelAction();
    }
}
