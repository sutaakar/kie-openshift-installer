package org.kie.cloud.openshift.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.kie.cloud.openshift.AbstractCloudTest;

public class ConfigurationLoaderTest extends AbstractCloudTest {

    @Test
    public void testGetKieServerImageStreamNamespaceDefault() {
        String imageStreamTag = ConfigurationLoader.getImageStreamNamespaceDefault();
        assertThat(imageStreamTag).isEqualTo("openshift");
    }

    @Test
    public void testGetKieServerImageStreamNamespaceFromSystemProperty() {
        System.setProperty("image.stream.namespace.default", "custom-namespace");
        try {
            String imageStreamTag = ConfigurationLoader.getImageStreamNamespaceDefault();
            assertThat(imageStreamTag).isEqualTo("custom-namespace");
        } finally {
            System.clearProperty("image.stream.namespace.default");
        }
    }

    @Test
    public void testGetKieServerImageStreamName() {
        String kieServerImageStreamName = ConfigurationLoader.getKieServerImageStreamName();
        assertThat(kieServerImageStreamName).contains("kieserver-openshift");
    }

    @Test
    public void testGetKieServerImageStreamTag() {
        String imageStreamTag = ConfigurationLoader.getImageStreamTag();
        assertThat(imageStreamTag).containsPattern("[0-9]\\.[0-9]*");
    }

    @Test
    public void testGetKieServerMemoryLimit() {
        String kieServerMemoryLimit = ConfigurationLoader.getKieServerMemoryLimit();
        assertThat(kieServerMemoryLimit).isEqualTo("1Gi");
    }

    @Test
    public void testGetMySqlImageStreamName() {
        String mySqlImageStreamName = ConfigurationLoader.getMySqlImageStreamName();
        assertThat(mySqlImageStreamName).isEqualTo("mysql");
    }

    @Test
    public void testGetMySqlImageStreamTag() {
        String mySqlImageStreamTag = ConfigurationLoader.getMySqlImageStreamTag();
        assertThat(mySqlImageStreamTag).isEqualTo("5.7");
    }

    @Test
    public void testGetPostgreSqlImageStreamName() {
        String postgreSqlImageStreamName = ConfigurationLoader.getPostgreSqlImageStreamName();
        assertThat(postgreSqlImageStreamName).isEqualTo("postgresql");
    }

    @Test
    public void testGetPostgreSqlImageStreamTag() {
        String postgreSqlImageStreamTag = ConfigurationLoader.getPostgreSqlImageStreamTag();
        assertThat(postgreSqlImageStreamTag).isEqualTo("9.6");
    }

    @Test
    public void testGetKieServerDatasourceJndi() {
        String kieServerDatasourceJndi = ConfigurationLoader.getKieServerDatasourceJndi();
        assertThat(kieServerDatasourceJndi).startsWith("java:/jboss/datasources/");
    }
}
