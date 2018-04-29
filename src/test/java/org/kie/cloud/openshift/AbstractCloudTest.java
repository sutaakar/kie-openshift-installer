package org.kie.cloud.openshift;

import io.fabric8.openshift.client.DefaultOpenShiftClient;
import io.fabric8.openshift.client.OpenShiftClient;
import io.fabric8.openshift.client.OpenShiftConfig;
import io.fabric8.openshift.client.OpenShiftConfigBuilder;
import org.junit.After;
import org.junit.Before;

public abstract class AbstractCloudTest {

    protected OpenShiftClient openShiftClient;

    @Before
    public void createOfflineOpenShiftClient() {
        OpenShiftConfig openShiftConfig = new OpenShiftConfigBuilder()
                .withDisableApiGroupCheck(true)
                .build();

        openShiftClient = new DefaultOpenShiftClient(openShiftConfig);
    }

    @After
    public void closeOpenShiftClient() {
        openShiftClient.close();
    }
}
