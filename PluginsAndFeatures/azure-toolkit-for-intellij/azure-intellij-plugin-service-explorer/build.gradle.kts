dependencies {
    implementation(project(":azure-intellij-plugin-lib"))
    // runtimeOnly project(path: ":azure-intellij-plugin-lib", configuration: "instrumentedJar")
    implementation(project(":azure-intellij-plugin-appmod"))
    // runtimeOnly project(path: ":azure-intellij-plugin-appmod", configuration: "instrumentedJar")
    implementation("com.microsoft.azure:azure-toolkit-ide-common-lib")
}
