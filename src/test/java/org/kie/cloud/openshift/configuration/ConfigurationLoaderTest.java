package org.kie.cloud.openshift.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.kie.cloud.openshift.AbstractCloudTest;

public class ConfigurationLoaderTest extends AbstractCloudTest {

    @Test
    public void testGetKieServerImageStreamName() {
        String kieServerImageStreamName = ConfigurationLoader.getKieServerImageStreamName();
        assertThat(kieServerImageStreamName).contains("kieserver-openshift");
    }

    @Test
    public void testGetKieServerImageStreamTag() {
        String kieServerImageStreamName = ConfigurationLoader.getKieServerImageStreamTag();
        assertThat(kieServerImageStreamName).containsPattern("[0-9]\\.[0-9]*");
    }
}
