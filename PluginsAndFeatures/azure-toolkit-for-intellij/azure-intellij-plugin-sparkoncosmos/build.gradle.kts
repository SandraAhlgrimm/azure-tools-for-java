plugins{
    id("java")
    id("org.jetbrains.kotlin.jvm")
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.9.10")
    implementation(project(":azure-intellij-plugin-lib"))
    // runtimeOnly project(path: ":azure-intellij-plugin-lib", configuration: "instrumentedJar")
    implementation(project(":azure-intellij-plugin-lib-java"))
    // runtimeOnly project(path: ":azure-intellij-plugin-lib-java", configuration: "instrumentedJar")
    implementation(project(":azure-intellij-plugin-hdinsight-lib"))
    // runtimeOnly project(path: ":azure-intellij-plugin-hdinsight-lib", configuration: "instrumentedJar")
    implementation("com.microsoft.hdinsight:azure-toolkit-ide-cosmos-spark-lib")
}
