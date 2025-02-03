package com.microsoft.azure.toolkit.intellij.java.sdk.utils;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import javax.annotation.Nonnull;
import kotlin.Unit;
import kotlin.coroutines.Continuation;

/**
 * The RuleConfigLoaderInitializer class is responsible for initializing the RuleConfigLoader class.
 */
public class RuleConfigLoaderInitializer implements ProjectActivity {

    @javax.annotation.Nullable
    @Override
    public Object execute(@Nonnull Project project, @Nonnull Continuation<? super Unit> continuation) {
        RuleConfigLoader.initialize();
        return null;
    }
}