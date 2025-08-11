package com.microsoft.azure.toolkit.intellij.explorer.azd;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.terminal.ui.TerminalWidget;
import com.microsoft.azure.toolkit.intellij.common.TerminalUtils;
import com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemeter;
import com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemetry;

import java.util.Map;

public final class AzdUtils {

    private static final String AZURE_DEVELOPER_CLI = "azd";

    private AzdUtils() {
        // Utility class, no instantiation allowed
    }

    public static void logTelemetryEvent(String eventName) {
        Map<String, String> properties = Map.of(
                AzureTelemeter.OP_NAME, eventName,
                AzureTelemeter.OP_PARENT_ID, AZURE_DEVELOPER_CLI,
                AzureTelemeter.OPERATION_NAME, eventName, // what's the difference between OP_NAME and OPERATION_NAME?
                AzureTelemeter.SERVICE_NAME, AZURE_DEVELOPER_CLI
        );
        AzureTelemeter.log(AzureTelemetry.Type.INFO, properties);
    }

    public static void executeInTerminal(Project project, String command) {
        final TerminalWidget azdTerminal = TerminalUtils.getTerminalWidget(project, null, "azd");
        if (TerminalUtils.hasRunningCommands(azdTerminal)) {
            Messages.showErrorDialog(project, "Another command is already running. Please try again later.", "Error");
        } else {
            TerminalUtils.executeInTerminal(azdTerminal, command);
        }
    }
}
