package org.kie.cloud.openshift.util;

import java.util.UUID;

public class NameGenerator {

    public static String generateDeploymentName(String deploymentPrefix) {
        return deploymentPrefix + "-" + generateRandomName();
    }

    public static String generateRandomNameUpperCase() {
        return generateRandomName().toUpperCase();
    }

    public static String generateRandomName() {
        return UUID.randomUUID().toString().substring(0, 4);
    }
}
