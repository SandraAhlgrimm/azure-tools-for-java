package com.microsoft.azure.toolkit.intellij.explorer.azd;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.ide.common.component.Node;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.intellij.common.TerminalUtils;

import javax.annotation.Nonnull;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.stream.Collectors;

public class AzdNode extends Node<String> {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    private final Project project;

    public AzdNode(Project project) {
        super("Azure Developer (Preview)");
        this.project = project;
        withIcon(AzureIcons.Common.SERVICES);
        addChildren();
    }

    public void addChildren() {
        if (isAzdInstalled()) {
            if (isAzdSignedIn()) {
                withDescription("Signed In");
                addChild(getCreateFromTemplatesNode(project));
                addChild(getInitializeFromSourceNode(project));
                addChild(getProvisionResourcesNode(project));
                addChild(getDeployToAzureNode(project));
                addChild(getProvisionAndDeployToAzureNode(project));
            } else {
                withDescription("Not Signed In");
                onClicked(e -> {
                    final ConfirmAndRunDialog confirmAndRunDialog = new ConfirmAndRunDialog(project, "Sign in", "Do you want to sign in to Azure Developer CLI (azd)?", "azd auth login");
                    confirmAndRunDialog.setOkButtonText("Sign In");
                    confirmAndRunDialog.show();
                });
            }
        } else {
            withDescription("Install azd");
            onClicked(e -> {
                final String command;
                if (System.getProperties().getProperty("os.name").toLowerCase().contains("windows")) {
                    command = "winget install microsoft.azd";
                    TerminalUtils.executeInTerminal(project, "winget install microsoft.azd", "azd");
                } else if (System.getProperties().getProperty("os.name").toLowerCase().contains("linux")) {
                    command = "curl -fsSL https://aka.ms/install-azd.sh | bash";
                    TerminalUtils.executeInTerminal(project, "curl -fsSL https://aka.ms/install-azd.sh | bash", "azd");
                } else {
                    command = "brew tap azure/azd && brew install azd";
                    TerminalUtils.executeInTerminal(project, "brew tap azure/azd && brew install azd", "azd");
                }
                final ConfirmAndRunDialog installDialog = new ConfirmAndRunDialog(project, "Install azd", "Do you want to install Azure Developer CLI (azd)?", command);
                installDialog.setOkButtonText("Install");
                installDialog.show();
            });
        }
    }

    private static Node<String> getProvisionAndDeployToAzureNode(Project project) {
        return new Node<>("Provision and Deploy")
                .withIcon(AzureIcons.Action.START)
                .onClicked(e -> new ConfirmAndRunDialog(project, "Provision and deploy", "Do you want to provision and deploy to Azure?", "azd up").show());
    }

    private static Node<String> getDeployToAzureNode(Project project) {
        return new Node<>("Deploy to Azure")
                .withIcon(AzureIcons.Action.DEPLOY)
                .onClicked(e -> new ConfirmAndRunDialog(project, "Deploy to Azure", "Do you want to start deployment to Azure?", "azd deploy").show());
    }

    private static Node<String> getProvisionResourcesNode(Project project) {
        return new Node<>("Provision resources")
                .withIcon(AzureIcons.Action.EXPORT)
                .onClicked(e -> new ConfirmAndRunDialog(project, "Provision Resources", "Do you want to provision Azure resources?", "azd provision").show());
    }

    private static Node<String> getInitializeFromSourceNode(Project project) {
        return new Node<>("Initialize from source")
                .withIcon(AzureIcons.Action.EDIT)
                .onClicked(e -> new ConfirmAndRunDialog(project, "Initialize from source", "Do you want to initialize using existing code?", "azd init").show());
    }

    private static Node<String> getCreateFromTemplatesNode(Project project) {
        return new Node<>("Create from templates")
                .withIcon(AzureIcons.Common.CREATE)
                .onClicked(e -> new AzdTemplatesDialog(project).show());
    }

    private static boolean isAzdInstalled() {
        final String azdVersionJson = runCommand("azd version -o json");
        if (azdVersionJson != null && !azdVersionJson.isEmpty()) {
            try {
                final Map<String, String> response = OBJECT_MAPPER.readValue(azdVersionJson, Map.class);
                if (response.containsKey("azd")) {
                    return true;
                }
            } catch (JsonProcessingException e) {
            }
        }
        return false;
    }

    private static boolean isAzdSignedIn() {
        final String loginStatusJson = runCommand("azd auth login --check-status -o json");
        if (loginStatusJson != null && !loginStatusJson.isEmpty()) {
            try {
                final Map<String, String> response = OBJECT_MAPPER.readValue(loginStatusJson, Map.class);
                if (response.containsKey("status") && "success".equals(response.get("status"))) {
                    return true;
                }
            } catch (JsonProcessingException e) {
            }
        }
        return false;
    }

    public static String runCommand(String command) {
        try {
            final ProcessBuilder processBuilder = new ProcessBuilder();
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
                return output;
            }
        } catch (Exception e) {
            return null; // Handle error appropriately
        }
    }
}
