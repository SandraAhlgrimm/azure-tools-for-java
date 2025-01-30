// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.azure.toolkit.intellij.java.sdk.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.toolkit.intellij.java.sdk.models.RuleConfig;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.microsoft.azure.toolkit.intellij.java.sdk.models.RuleConfig.EMPTY_RULE;

public class RuleConfigLoader {
    private static final String CONFIG_FILE_PATH = "./META-INF/ruleConfigs.json";
    private static final Logger LOGGER = Logger.getLogger(RuleConfigLoader.class.getName());
    private static final RuleConfigLoader INSTANCE;
    private static boolean initializationFailed = false;
    private final Map<String, RuleConfig> ruleConfigs;

    static {
        RuleConfigLoader tempInstance;
        try {
            tempInstance = new RuleConfigLoader(CONFIG_FILE_PATH);
        } catch (IOException e) {
            tempInstance = null;
            initializationFailed = true;
            LOGGER.log(Level.SEVERE, "Failed to initialize RuleConfigLoader: " + e.getMessage(), e);
        }
        INSTANCE = tempInstance;
    }

    private RuleConfigLoader() {
        this.ruleConfigs = new HashMap<>();
    }

    private RuleConfigLoader(String filePath) throws IOException {
        this.ruleConfigs = loadRuleConfigurations(filePath);
    }

    /**
     * Gets the singleton instance of RuleConfigLoader.
     *
     * @return The singleton instance of RuleConfigLoader.
     */
    public static RuleConfigLoader getInstance() {
        if (initializationFailed) {
            LOGGER.log(Level.WARNING, "RuleConfigLoader initialization failed. Returning default instance.");
            return new RuleConfigLoader();
        }
        return INSTANCE;
    }

    /**
     * Gets the rule configuration for the specified key.
     *
     * @param key The key of the rule configuration.
     * @return The rule configuration for the specified key.
     */
    public RuleConfig getRuleConfig(String key) {
        if (initializationFailed) {
            return EMPTY_RULE;
        }
        return ruleConfigs.getOrDefault(key, EMPTY_RULE);
    }

    private Map<String, RuleConfig> loadRuleConfigurations(String filePath) throws IOException {
        InputStream configStream = getClass().getClassLoader().getResourceAsStream(filePath);
        if (configStream == null) {
            initializationFailed = true;
            LOGGER.log(Level.WARNING, "Rule configuration file not found: " + filePath);
            return new HashMap<>();
        }

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(configStream);
        Map<String, RuleConfig> configs = new HashMap<>();

        rootNode.fields().forEachRemaining(entry -> {
            String ruleName = entry.getKey();
            JsonNode ruleNode = entry.getValue();

            if (ruleNode.path("hasDerivedRules").asBoolean(false)) {
                // Parse derived rules
                ruleNode.fields().forEachRemaining(derivedEntry -> {
                    if (!derivedEntry.getKey().equals("hasDerivedRules")) {
                        String derivedRuleName = derivedEntry.getKey();
                        JsonNode derivedRuleNode = derivedEntry.getValue();
                        RuleConfig derivedRuleConfig = parseRuleConfig(derivedRuleNode);
                        configs.put(derivedRuleName, derivedRuleConfig);
                    }
                });
            } else {
                RuleConfig ruleConfig = parseRuleConfig(ruleNode);
                configs.put(ruleName, ruleConfig);
            }
        });

        return configs;
    }

    private RuleConfig parseRuleConfig(JsonNode ruleNode) {
        List<String> usages = parseStringOrArray(ruleNode.path("usages"));
        List<String> scope = parseStringOrArray(ruleNode.path("scope"));
        String antiPatternMessage = ruleNode.path("antiPatternMessage").asText(null);
        Map<String, String> regexPatterns = parseRegexPatterns(ruleNode.path("regexPatterns"));
        boolean skipRuleCheck = ruleNode.path("skip").asBoolean(false);

        return new RuleConfig(usages, scope, antiPatternMessage, regexPatterns, skipRuleCheck);
    }

    private List<String> parseStringOrArray(JsonNode node) {
        List<String> values = new ArrayList<>();
        if (node.isTextual()) {
            values.add(node.asText());
        } else if (node.isArray()) {
            node.forEach(element -> values.add(element.asText()));
        }
        return values;
    }

    private Map<String, String> parseRegexPatterns(JsonNode regexPatternsNode) {
        Map<String, String> regexPatterns = new HashMap<>();
        if (regexPatternsNode != null && regexPatternsNode.isObject()) {
            regexPatternsNode.fields().forEachRemaining(entry -> regexPatterns.put(entry.getKey(), entry.getValue().asText()));
        }
        return regexPatterns;
    }
}
