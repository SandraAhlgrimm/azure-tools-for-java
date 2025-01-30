// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.azure.toolkit.intellij.java.sdk.analyzer;

import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiReferenceExpression;
import com.microsoft.azure.toolkit.intellij.java.sdk.models.RuleConfig;
import java.util.Collections;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link GetCompletionsCheck} and {@link ConnectionStringCheck} .
 */
public class DetectDiscouragedAPIUsageCheckTest {

    @Mock
    private ProblemsHolder mockHolder;

    @Mock
    private JavaElementVisitor mockVisitor;

    @Mock
    private PsiMethodCallExpression methodCallExpression;

    @Mock
    private PsiElement problemElement;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        mockHolder = mock(ProblemsHolder.class);
        methodCallExpression = mock(PsiMethodCallExpression.class);
    }

    @ParameterizedTest
    @MethodSource("provideTestCases")
    public void detectsDiscouragedAPIUsage(TestCase testCase) {
        JavaElementVisitor visitor = createVisitor(testCase.visitorClass);
        setupMockAPI(testCase.methodToCheck, testCase.numOfInvocations, testCase.packageName,
            testCase.suggestionMessage);
        visitor.visitMethodCallExpression(methodCallExpression);
        verify(mockHolder, times(testCase.numOfInvocations)).registerProblem(eq(problemElement),
            eq(testCase.suggestionMessage));
    }

    private static RuleConfig getGetCompletionsConfig() {
        RuleConfig getCompletionsConfig = mock(RuleConfig.class);
        when(getCompletionsConfig.getUsagesToCheck()).thenReturn(Collections.singletonList("getCompletions"));
        when(getCompletionsConfig.getScopeToCheck()).thenReturn(Collections.singletonList("com.azure.ai.openai"));
        when(getCompletionsConfig.getAntiPatternMessage()).thenReturn(
            "getCompletions API usage detected. Use the getChatCompletions API instead.");
        return getCompletionsConfig;
    }

    private static RuleConfig getConnectionStringConfig() {
        RuleConfig connectionStringConfig = mock(RuleConfig.class);
        when(connectionStringConfig.getUsagesToCheck()).thenReturn(Collections.singletonList("connectionString"));
        when(connectionStringConfig.getScopeToCheck()).thenReturn(Collections.singletonList("com.azure"));
        when(connectionStringConfig.getAntiPatternMessage()).thenReturn(
            "Connection String API usage detected. Use DefaultAzureCredential for Azure service client authentication instead if the service client supports Token Credential (Entra ID Authentication).");
        return connectionStringConfig;
    }

    private void setupMockAPI(String methodToCheck, int numOfInvocations, String packageName,
        String suggestionMessage) {
        methodCallExpression = mock(PsiMethodCallExpression.class);
        PsiReferenceExpression methodExpression = mock(PsiReferenceExpression.class);
        PsiMethod resolvedMethod = mock(PsiMethod.class);
        PsiClass containingClass = mock(PsiClass.class);
        problemElement = mock(PsiElement.class);

        when(methodCallExpression.getMethodExpression()).thenReturn(methodExpression);
        when(methodExpression.resolve()).thenReturn(resolvedMethod);
        when(resolvedMethod.getContainingClass()).thenReturn(containingClass);
        when(resolvedMethod.getName()).thenReturn(methodToCheck);
        when(containingClass.getQualifiedName()).thenReturn(packageName);
        when(methodExpression.getReferenceNameElement()).thenReturn(problemElement);
    }

    private JavaElementVisitor createVisitor(Class<? extends PsiElementVisitor> visitorClass) {
        if (visitorClass == GetCompletionsCheck.GetCompletionsVisitor.class) {
            return new GetCompletionsCheck.GetCompletionsVisitor(mockHolder);
        } else if (visitorClass == ConnectionStringCheck.ConnectionStringCheckVisitor.class) {
            return new ConnectionStringCheck.ConnectionStringCheckVisitor(mockHolder);
        }
        throw new IllegalArgumentException("Unsupported visitor class: " + visitorClass);
    }

    private static Stream<TestCase> provideTestCases() {
        return Stream.of(
            new TestCase(ConnectionStringCheck.ConnectionStringCheckVisitor.class,
                getConnectionStringConfig(),
                "connectionString", "com.azure",
                "Connection String API usage detected. Use DefaultAzureCredential for Azure service client authentication instead if the service client supports Token Credential (Entra ID Authentication).",
                1),
            new TestCase(GetCompletionsCheck.GetCompletionsVisitor.class,
                getGetCompletionsConfig(), "getCompletions", "com.azure.ai.openai", "getCompletions API " +
                "detected. Use the getChatCompletions API instead.", 1),
            new TestCase(ConnectionStringCheck.ConnectionStringCheckVisitor.class,
                getConnectionStringConfig(), "allowedMethod", "com.azure", "", 0),
            new TestCase(ConnectionStringCheck.ConnectionStringCheckVisitor.class,
                getConnectionStringConfig(), "connectionString", "com.microsoft.azure", "", 0),
            new TestCase(GetCompletionsCheck.GetCompletionsVisitor.class,
                getGetCompletionsConfig(), "getCompletions", "com.azure.other", "", 0)
        );
    }

    private static class TestCase {
        Class<? extends PsiElementVisitor> visitorClass;
        String methodToCheck;
        String packageName;
        String suggestionMessage;
        int numOfInvocations;
        RuleConfig ruleConfig;

        TestCase(Class<? extends PsiElementVisitor> visitorClass, RuleConfig ruleConfig, String methodToCheck, String packageName,
            String suggestionMessage,
            int numOfInvocations) {
            this.visitorClass = visitorClass;
            this.methodToCheck = methodToCheck;
            this.packageName = packageName;
            this.suggestionMessage = suggestionMessage;
            this.numOfInvocations = numOfInvocations;
            this.ruleConfig = ruleConfig;
        }
    }
}