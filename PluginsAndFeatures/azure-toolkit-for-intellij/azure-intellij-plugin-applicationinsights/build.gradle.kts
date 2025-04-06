dependencies {
    implementation(project(":azure-intellij-plugin-lib"))
    // runtimeOnly project(path: ":azure-intellij-plugin-lib", configuration: "instrumentedJar")
    implementation(project(":azure-intellij-plugin-guidance"))
    // runtimeOnly project(path: ":azure-intellij-plugin-guidance", configuration: "instrumentedJar")
    implementation(project(":azure-intellij-resource-connector-lib"))
    // runtimeOnly project(path: ":azure-intellij-resource-connector-lib", configuration: "instrumentedJar")
    implementation(project(":azure-intellij-plugin-monitor"))
    // runtimeOnly project(path: ":azure-intellij-plugin-monitor", configuration: "instrumentedJar")
    implementation("com.microsoft.azure:azure-toolkit-common-lib")
    aspect("com.microsoft.azure:azure-toolkit-common-lib")
    implementation("com.microsoft.azure:azure-toolkit-ide-common-lib")
    implementation("com.microsoft.azure:azure-toolkit-ide-applicationinsights-lib")
}
