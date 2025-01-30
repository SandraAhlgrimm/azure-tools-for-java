// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.azure.toolkit.intellij.java.sdk.analyzer;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.microsoft.azure.toolkit.intellij.java.sdk.models.RuleConfig;
import com.microsoft.azure.toolkit.intellij.java.sdk.utils.HelperUtils;
import com.microsoft.azure.toolkit.intellij.java.sdk.utils.RuleConfigLoader;
import org.jetbrains.annotations.NotNull;

/**
 * Inspection tool to check discouraged Connection String usage.
 */
public class ConnectionStringCheck extends LocalInspectionTool {

    @Override
    public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new ConnectionStringCheckVisitor(holder);
    }

    static class ConnectionStringCheckVisitor extends JavaElementVisitor {
        private final ProblemsHolder holder;
        private static final RuleConfig RULE_CONFIG;
        private static final boolean SKIP_WHOLE_RULE;

        ConnectionStringCheckVisitor(ProblemsHolder holder) {
            this.holder = holder;
        }

        static {
            RuleConfigLoader ruleConfigLoader = RuleConfigLoader.getInstance();
            RULE_CONFIG = ruleConfigLoader.getRuleConfig("ConnectionStringCheck");
            SKIP_WHOLE_RULE = RULE_CONFIG.skipRuleCheck();
        }

        @Override
        public void visitMethodCallExpression(@NotNull PsiMethodCallExpression expression) {
            super.visitMethodCallExpression(expression);
            // Check if the rule should be skipped
            if (SKIP_WHOLE_RULE) {
                return;
            }

            PsiMethod method = HelperUtils.getResolvedMethod(expression);
            if (HelperUtils.isItDiscouragedAPI(method, RULE_CONFIG.getUsagesToCheck(), RULE_CONFIG.getScopeToCheck())) {

                PsiElement problemElement = expression.getMethodExpression().getReferenceNameElement();
                if (problemElement != null) {
                    holder.registerProblem(problemElement, RULE_CONFIG.getAntiPatternMessage());
                }
            }
        }
    }
}