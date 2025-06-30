package com.microsoft.azure.toolkit.intellij.explorer.azd;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.popup.IconButton;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.stream.Collectors;

public class AzdInitializeFromTemplates {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    private Project project;

    /**
     * Shows the tool popup
     */
    public static void showToolPopup(Project project) {
        String azdVersionJson = runCommand("azd version -o json");
        if (azdVersionJson != null || !azdVersionJson.isEmpty()) {
            try {
                Map<String, String> response = OBJECT_MAPPER.readValue(azdVersionJson, Map.class);
                if (!response.containsKey("azd")) {
                    JOptionPane.showMessageDialog(null, "Please install Azure Developer CLI (azd) first.", "Installation Required", JOptionPane.WARNING_MESSAGE);
                    return;
                }
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        } else {
            JOptionPane.showMessageDialog(null, "Please install Azure Developer CLI (azd) first.", "Installation Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String loginStatusJson = runCommand("azd auth login --check-status -o json");
        if (loginStatusJson != null && !loginStatusJson.isEmpty()) {
            try {
                Map<String, String> response = OBJECT_MAPPER.readValue(loginStatusJson, Map.class);
                if (!response.containsKey("status") || !response.get("status").equals("success")) {
                    JOptionPane.showMessageDialog(null, "Please login to Azure using 'azd auth login' command first.", "Login Required", JOptionPane.WARNING_MESSAGE);
                    return;
                }
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        } else {
            JOptionPane.showMessageDialog(null, "Please login to Azure using 'azd auth login' command first.", "Login Required", JOptionPane.WARNING_MESSAGE);
        }
        AzdAvailableTemplatesPopup popupPanel = new AzdAvailableTemplatesPopup(project);

        JBPopup popup = JBPopupFactory.getInstance()
                .createComponentPopupBuilder(popupPanel, null)
                .setTitle("Available AZD templates for Java")
                .setResizable(true)
                .setMovable(true)
                .setRequestFocus(true)
                .setMinSize(new Dimension(1400, 800))
                .setCancelOnClickOutside(false)
                .setCancelOnWindowDeactivation(false)
                .setCancelButton(new IconButton("Close", AllIcons.Actions.Close, AllIcons.Actions.CloseHovered))
                .createPopup();

        popupPanel.setPopup(popup);

        Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
        if (editor != null) {
            popup.showInBestPositionFor(editor);
        } else {
            popup.showCenteredInCurrentWindow(project);
        }
    }


    /**
     * Dialog to show confirmation for running a command
     */
    private static class CloseConfirmationDialog extends DialogWrapper {

        public CloseConfirmationDialog(Project project) {
            super(project, false);
            setTitle("Exit AZD Window");
            init();
        }

        @Override
        protected @Nullable JComponent createCenterPanel() {
            JPanel panel = new JPanel(new BorderLayout());
            panel.add(new JLabel("Are you sure?"), BorderLayout.CENTER);
            return panel;
        }

        @Override
        protected void doOKAction() {
            super.doOKAction();
        }
    }

    public static String runCommand(String command) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder();
            // Detect OS and set the appropriate command
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) {
                processBuilder.command("cmd", "/c", command); // Windows
            } else {
                processBuilder.command("bash", "-c", command); // Linux/Unix
            }

            Process process = processBuilder.start();

            // Read the command output
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String output = reader.lines().collect(Collectors.joining("\n"));
                int exitCode = process.waitFor();
                if (exitCode != 0) {
                    return null;
                }
                return output; // JSON output as a string
            }
        } catch (Exception e) {
            return null; // Handle error appropriately
        }

    }
}
