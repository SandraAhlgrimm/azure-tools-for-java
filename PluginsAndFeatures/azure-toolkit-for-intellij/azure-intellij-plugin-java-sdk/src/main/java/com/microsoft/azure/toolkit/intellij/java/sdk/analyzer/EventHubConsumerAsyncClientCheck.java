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
 * This class is used to check if the EventHubConsumerAsyncClient is being used in the code.
 */
public class EventHubConsumerAsyncClientCheck extends LocalInspectionTool {

    private final RuleConfig ruleConfig;
    private final boolean skipRuleCheck;

    public EventHubConsumerAsyncClientCheck() {
        super();
        RuleConfigLoader ruleConfigLoader = RuleConfigLoader.getInstance();
        this.ruleConfig = ruleConfigLoader.getRuleConfig("EventHubConsumerAsyncClientCheck");
        this.skipRuleCheck = ruleConfig.skipRuleCheck();
    }

    @NotNull
    @Override
    public JavaElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        if (skipRuleCheck) {
            return new JavaElementVisitor() {};
        }
        return new JavaElementVisitor() {
            @Override
            public void visitTypeElement(PsiTypeElement element) {
                super.visitTypeElement(element);
                if (HelperUtils.isItDiscouragedClient(element, ruleConfig.getUsagesToCheck())) {
                    holder.registerProblem(element, ruleConfig.getAntiPatternMessage());
                }
            }
        };
    }
}