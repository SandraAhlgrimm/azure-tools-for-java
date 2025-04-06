dependencies {
    implementation(project(":azure-intellij-plugin-lib"))
    // runtimeOnly project(path: ":azure-intellij-plugin-lib", configuration: "instrumentedJar")
    implementation(project(":azure-intellij-plugin-database"))
    // runtimeOnly project(path: ":azure-intellij-plugin-database", configuration: "instrumentedJar")
    implementation(project(":azure-intellij-resource-connector-lib"))
    // runtimeOnly project(path: ":azure-intellij-resource-connector-lib", configuration: "instrumentedJar")
    implementation(project(":azure-intellij-resource-connector-lib-java"))
    // runtimeOnly project(path: ":azure-intellij-resource-connector-lib-java", configuration: "instrumentedJar")
    implementation("com.microsoft.azure:azure-toolkit-database-lib")
    implementation("com.microsoft.azure:azure-toolkit-mysql-lib")
    implementation("com.microsoft.azure:azure-toolkit-sqlserver-lib")
    implementation("com.microsoft.azure:azure-toolkit-postgre-lib")
}