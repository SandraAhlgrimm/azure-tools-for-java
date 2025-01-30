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
 * This class extends the AbstractUpdateCheckpointChecker class to check for the usage of the updateCheckpointAsync()
 * method call in the code. The visitor inspects the method call expressions and checks if the method call is
 * updateCheckpointAsync(). If the method call is updateCheckpointAsync() and the following method is subscribe, a
 * problem is registered.
 */
public class UpdateCheckpointAsyncSubscribeCheck extends LocalInspectionTool {

    /**
     * Build the visitor for the inspection. This visitor will be used to traverse the PSI tree.
     *
     * @param holder The holder for the problems found
     *
     * @return The visitor for the inspection. This is not used anywhere else in the code.
     */
    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new UpdateCheckpointAsyncVisitor(holder);
    }

    /**
     * This class extends the JavaElementVisitor to visit the elements in the code. It checks if the method call is
     * updateCheckpointAsync() and if the following method is `subscribe`. If both conditions are met, a problem is
     * registered with the suggestion message.
     */
    static class UpdateCheckpointAsyncVisitor extends JavaElementVisitor {

        // Define the holder to register problems
        private final ProblemsHolder holder;

        private static final RuleConfig RULE_CONFIG;
        private static final boolean SKIP_WHOLE_RULE;

        /**
         * Constructor to initialize the visitor with the holder.
         *
         * @param holder ProblemsHolder to register problems
         */
        UpdateCheckpointAsyncVisitor(ProblemsHolder holder) {
            this.holder = holder;
        }

        static {
            RuleConfigLoader ruleConfigLoader = RuleConfigLoader.getInstance();
            RULE_CONFIG = ruleConfigLoader.getRuleConfig("UpdateCheckpointAsyncSubscribeCheck");
            SKIP_WHOLE_RULE = RULE_CONFIG.skipRuleCheck();
        }

        /**
         * This method visits the method call expressions in the code. It checks if the method call is
         * updateCheckpointAsync() and if the following method is `subscribe`. If both conditions are met, a problem is
         * registered with the suggestion message.
         *
         * @param expression The method call expression to inspect
         */
        @Override
        public void visitMethodCallExpression(@NotNull PsiMethodCallExpression expression) {
            super.visitMethodCallExpression(expression);

            // Check if the rule should be skipped
            if (SKIP_WHOLE_RULE) {
                return;
            }

            if (expression.getMethodExpression().getReferenceName() == null) {
                return;
            }

            // Check if the updateCheckpointAsync() method call is called on an provided context
            // (EventBatchContext) object
            PsiMethod method = HelperUtils.getResolvedMethod(expression);
            if (HelperUtils.isItDiscouragedAPI(method, RULE_CONFIG.getUsagesToCheck(), RULE_CONFIG.getScopeToCheck())) {

                // Check if the following method is `subscribe` and
                if (HelperUtils.isFollowedBySubscribe(expression)) {
                    PsiElement problemElement = expression.getMethodExpression().getReferenceNameElement();
                    holder.registerProblem(problemElement, RULE_CONFIG.getAntiPatternMessage());
                }
            }
        }
    }
}