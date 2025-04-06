dependencies {
    implementation(project(":azure-intellij-plugin-lib"))
    // runtimeOnly project(path: ":azure-intellij-plugin-lib", configuration: "instrumentedJar")
    implementation(project(":azure-intellij-plugin-guidance"))
    // runtimeOnly project(path: ":azure-intellij-plugin-guidance", configuration: "instrumentedJar")
    implementation(project(":azure-intellij-resource-connector-lib"))
    // runtimeOnly project(path: ":azure-intellij-resource-connector-lib", configuration: "instrumentedJar")
    implementation(project(":azure-intellij-plugin-monitor"))
    // runtimeOnly project(path: ":azure-intellij-plugin-monitor", configuration: "instrumentedJar")
    implementation("com.microsoft.azure:azure-toolkit-appservice-lib")
    implementation("com.microsoft.azure:azure-toolkit-ide-appservice-lib")
    implementation("com.microsoft.azure:azure-toolkit-ide-containerregistry-lib")
    implementation("com.jcraft:jsch:0.1.55")
    implementation("org.codehaus.plexus:plexus-archiver:4.2.7")
    implementation("org.codehaus.plexus:plexus-container-default:2.1.1")
    implementation("com.neovisionaries:nv-websocket-client:2.14")

    intellijPlatform {
        // Plugin Dependencies. Uses `platformBundledPlugins` property from the gradle.properties file for bundled IntelliJ Platform plugins.
        bundledPlugin("com.intellij.properties")
        bundledPlugin("org.jetbrains.plugins.terminal")
    }
}
