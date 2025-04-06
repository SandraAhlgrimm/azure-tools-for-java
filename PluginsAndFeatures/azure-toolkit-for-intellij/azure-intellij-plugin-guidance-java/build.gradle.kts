dependencies {
    implementation(project(":azure-intellij-plugin-lib"))
    // runtimeOnly project(path: ":azure-intellij-plugin-lib", configuration: "instrumentedJar")
    implementation(project(":azure-intellij-plugin-lib-java"))
    // runtimeOnly project(path: ":azure-intellij-plugin-lib-java", configuration: "instrumentedJar")
    implementation(project(":azure-intellij-plugin-guidance"))
    // runtimeOnly project(path: ":azure-intellij-plugin-guidance", configuration: "instrumentedJar")
    implementation("com.microsoft.azure:azure-toolkit-ide-common-lib")
    implementation("org.eclipse.jgit:org.eclipse.jgit:6.1.0.202203080745-r")
}