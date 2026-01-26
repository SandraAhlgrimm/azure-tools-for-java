package com.microsoft.azure.toolkit.intellij.appmod.javaupgrade;

import static com.microsoft.azure.toolkit.intellij.appmod.javaupgrade.service.JavaUpgradeIssuesDetectionService.MATURE_JAVA_LTS_VERSION;

public class Contants {
    public static final String UPGRADE_JAVA_AND_FRAMEWORK_PROMPT = "Upgrade java runtime and java framework dependencies of this project to the latest LTS version using java upgrade tools by invoking #generate_upgrade_plan";
    public static final String UPGRADE_JAVA_VERSION_PROMPT = "upgrade java runtime to Java " + MATURE_JAVA_LTS_VERSION + " (LTS) using java upgrade tools by invoking #generate_upgrade_plan";
    public static final String UPGRADE_JAVA_FRAMEWORK_PROMPT = "upgrade java framework dependencies of this project to latest LTS version using java upgrade tools by invoking #generate_upgrade_plan";
    public static final String SCAN_AND_RESOLVE_CVES_PROMPT = "run CVE scan for this project using java upgrade tools by invoking #validate_cves_for_java";
    public static final String UPGRADE_JDK_WITH_COPILOT_DISPLAY_NAME = "Upgrade JDK with Copilot";
    public static final String UPGRADE_SPRING_BOOT_WITH_COPILOT_DISPLAY_NAME = "Upgrade Spring Boot with Copilot";
    public static final String UPGRADE_SPRING_FRAMEWORK_WITH_COPILOT_DISPLAY_NAME = "Upgrade Spring Framework with Copilot";
    public static final String UPGRADE_SPRING_SECURITY_WITH_COPILOT_DISPLAY_NAME = "Upgrade Spring Security with Copilot";
    public static final String SCAN_AND_RESOLVE_CVES_WITH_COPILOT_DISPLAY_NAME = "Scan and Resolve CVEs with Copilot";
}
