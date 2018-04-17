package org.kie.cloud.openshift;

import io.fabric8.openshift.client.DefaultOpenShiftClient;
import io.fabric8.openshift.client.OpenShiftClient;
import io.fabric8.openshift.client.OpenShiftConfig;
import io.fabric8.openshift.client.OpenShiftConfigBuilder;

public abstract class AbstractCloudTest {

    protected OpenShiftClient getOfflineOpenShiftClient() {
        OpenShiftConfig openShiftConfig = new OpenShiftConfigBuilder()
                .withDisableApiGroupCheck(true)
                .build();

        return new DefaultOpenShiftClient(openShiftConfig);
    }
}
