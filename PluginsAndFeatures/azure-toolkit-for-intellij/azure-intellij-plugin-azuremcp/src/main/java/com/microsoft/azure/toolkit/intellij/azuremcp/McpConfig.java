package com.microsoft.azure.toolkit.intellij.azuremcp;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public class McpConfig {
    @JsonProperty("servers")
    private Map<String, McpServer> servers;

    public McpConfig() {
    }

    public Map<String, McpServer> getServers() {
        return servers;
    }

    public McpConfig setServers(Map<String, McpServer> servers) {
        this.servers = servers;
        return this;
    }
}
