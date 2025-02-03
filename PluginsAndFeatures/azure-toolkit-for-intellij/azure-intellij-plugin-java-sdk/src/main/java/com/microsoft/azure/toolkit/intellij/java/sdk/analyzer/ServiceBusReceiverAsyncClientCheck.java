package com.microsoft.azure.toolkit.intellij.java.sdk.analyzer;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiTypeElement;
import com.microsoft.azure.toolkit.intellij.java.sdk.models.RuleConfig;
import com.microsoft.azure.toolkit.intellij.java.sdk.utils.HelperUtils;
import com.microsoft.azure.toolkit.intellij.java.sdk.utils.RuleConfigLoader;
import org.jetbrains.annotations.NotNull;

/**
 * This class is used to check if the ServiceBusReceiverAsyncClient is being used in the code.
 */
public class ServiceBusReceiverAsyncClientCheck extends LocalInspectionTool {

    @NotNull
    @Override
    public JavaElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new ServiceBusReceiverAsyncClientCheck.ServiceBusReceiverAsyncClientCheckVisitor(holder,
            RuleConfigLoader.getInstance());
    }


    static class ServiceBusReceiverAsyncClientCheckVisitor extends JavaElementVisitor {

        private final ProblemsHolder holder;
        private static RuleConfig RULE_CONFIG;
        private static boolean SKIP_WHOLE_RULE;

        /**
         * Constructor to initialize the visitor with the holder and isOnTheFly flag.
         *
         * @param holder Holder for the problems found by the inspection
         * @param ruleConfigLoader RuleConfigLoader object to load the rule configuration
         */
        public ServiceBusReceiverAsyncClientCheckVisitor(ProblemsHolder holder, RuleConfigLoader ruleConfigLoader) {
            this.holder = holder;
            initializeRuleConfig(ruleConfigLoader);
        }

        private void initializeRuleConfig(RuleConfigLoader ruleConfigLoader) {
            if (RULE_CONFIG == null) {
                final String ruleName = "ServiceBusReceiverAsyncClientCheck";
                RULE_CONFIG = ruleConfigLoader.getRuleConfig(ruleName);
                SKIP_WHOLE_RULE = RULE_CONFIG.skipRuleCheck();
            }
        }

        @Override
        public void visitTypeElement(PsiTypeElement element) {
            super.visitTypeElement(element);
            if (SKIP_WHOLE_RULE) {
                return;
            }
            if (HelperUtils.isItDiscouragedClient(element, RULE_CONFIG.getUsagesToCheck())) {
                holder.registerProblem(element, RULE_CONFIG.getAntiPatternMessage());
            }
        }
    }
}