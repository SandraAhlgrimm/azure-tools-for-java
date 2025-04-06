dependencies {
    implementation(project(":azure-intellij-plugin-lib"))
    // runtimeOnly project(path: ":azure-intellij-plugin-lib", configuration: "instrumentedJar")
    implementation(project(":azure-intellij-plugin-guidance"))
    // runtimeOnly project(path: ":azure-intellij-plugin-guidance", configuration: "instrumentedJar")
    implementation(project(":azure-intellij-plugin-guidance-java"))
    // runtimeOnly project(path: ":azure-intellij-plugin-guidance-java", configuration: "instrumentedJar")
    implementation(project(":azure-intellij-resource-connector-lib"))
    // runtimeOnly project(path: ":azure-intellij-resource-connector-lib", configuration: "instrumentedJar")
    implementation("com.microsoft.azure:azure-toolkit-common-lib")
    implementation("com.microsoft.azure:azure-toolkit-ide-common-lib")
    implementation("com.microsoft.azure:azure-toolkit-ide-cognitiveservices-lib")
    implementation("com.azure:azure-ai-openai")

    intellijPlatform {
        // Plugin Dependencies. Uses `platformBundledPlugins` property from the gradle.properties file for bundled IntelliJ Platform plugins.
        bundledPlugin("org.intellij.plugins.markdown")
    }
}