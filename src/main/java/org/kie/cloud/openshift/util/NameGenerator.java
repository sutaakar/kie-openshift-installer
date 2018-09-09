package org.kie.cloud.openshift.util;

import java.util.UUID;

public class NameGenerator {

    public static String generateDeploymentName(String deploymentPrefix) {
        return deploymentPrefix + "-" + UUID.randomUUID().toString().substring(0, 4);
    }
}
