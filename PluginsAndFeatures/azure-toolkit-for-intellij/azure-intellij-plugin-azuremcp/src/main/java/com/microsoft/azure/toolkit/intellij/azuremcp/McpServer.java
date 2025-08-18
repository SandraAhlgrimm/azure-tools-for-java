package com.microsoft.azure.toolkit.intellij.azuremcp;

import java.util.List;
import java.util.Map;

public class McpServer {
    private String command;
    private List<String> args;
    private Map<String, String> env;
    private String description;

    public McpServer() {
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public List<String> getArgs() {
        return args;
    }

    public void setArgs(List<String> args) {
        this.args = args;
    }

    public Map<String, String> getEnv() {
        return env;
    }

    public void setEnv(Map<String, String> env) {
        this.env = env;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
