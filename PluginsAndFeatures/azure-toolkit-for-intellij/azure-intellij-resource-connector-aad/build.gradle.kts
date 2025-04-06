dependencies {
    implementation(project(":azure-intellij-plugin-lib"))
    // runtimeOnly project(path: ":azure-intellij-plugin-lib", configuration: "instrumentedJar")

    implementation("com.microsoft.graph:microsoft-graph:3.7.0")
    implementation("com.microsoft.azure:azure-toolkit-auth-lib")
}
