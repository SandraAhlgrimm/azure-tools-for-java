sourceSets {
    main {
        resources {
            srcDir("src/main/resources")
        }
    }
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.7.22")
    implementation(project(":azure-intellij-plugin-lib"))
    // runtimeOnly project(path: ":azure-intellij-plugin-lib", configuration: "instrumentedJar")
    implementation(project(":azure-intellij-plugin-storage"))
    // runtimeOnly project(path: ":azure-intellij-plugin-storage", configuration: "instrumentedJar")
    implementation("com.microsoft.azure:azure-toolkit-compute-lib")
    implementation("com.microsoft.azure:azure-toolkit-ide-common-lib")
    implementation("com.microsoft.azure:azure-toolkit-ide-vm-lib")
    intellijPlatform {
        // Plugin Dependencies. Uses `platformBundledPlugins` property from the gradle.properties file for bundled IntelliJ Platform plugins.
        bundledPlugin("org.jetbrains.plugins.remote-run")
        bundledPlugin("com.jetbrains.plugins.webDeployment")
        bundledPlugin("org.jetbrains.plugins.terminal")
    }
}
