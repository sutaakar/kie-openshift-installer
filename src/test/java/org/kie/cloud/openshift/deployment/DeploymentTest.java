package org.kie.cloud.openshift.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.kie.cloud.openshift.AbstractCloudTest;
import org.kie.cloud.openshift.KieOpenShiftProvider;
import org.kie.cloud.openshift.template.TemplateLoader;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.openshift.api.model.DeploymentConfig;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.api.model.Template;
import io.fabric8.openshift.client.OpenShiftClient;

public class DeploymentTest extends AbstractCloudTest{

    @Test
    public void testGetUnsecureServices() {
        Template kieServerTemplate = new TemplateLoader(openShiftClient).loadKieServerTemplate();

        Deployment deployment = new Deployment(kieServerTemplate);
        List<Service> services = deployment.getUnsecureServices();

        assertThat(services).hasSize(1);
        assertThat(services.get(0).getMetadata().getName()).isEqualTo("${APPLICATION_NAME}-kieserver");
    }

    @Test
    public void testGetSecureServices() {
        Template kieServerTemplate = new TemplateLoader(openShiftClient).loadKieServerTemplate();

        Deployment deployment = new Deployment(kieServerTemplate);
        List<Service> services = deployment.getSecureServices();

        assertThat(services).hasSize(1);
        assertThat(services.get(0).getMetadata().getName()).isEqualTo("secure-${APPLICATION_NAME}-kieserver");
    }

    @Test
    public void testGetUnsecureRoutes() {
        Template kieServerTemplate = new TemplateLoader(openShiftClient).loadKieServerTemplate();

        Deployment deployment = new Deployment(kieServerTemplate);
        List<Route> routes = deployment.getUnsecureRoutes();

        assertThat(routes).hasSize(1);
        assertThat(routes.get(0).getMetadata().getName()).isEqualTo("${APPLICATION_NAME}-kieserver");
    }

    @Test
    public void testGetSecureRoutes() {
        Template kieServerTemplate = new TemplateLoader(openShiftClient).loadKieServerTemplate();

        Deployment deployment = new Deployment(kieServerTemplate);
        List<Route> routes = deployment.getSecureRoutes();

        assertThat(routes).hasSize(1);
        assertThat(routes.get(0).getMetadata().getName()).isEqualTo("secure-${APPLICATION_NAME}-kieserver");
    }

    @Test
    public void testGetDeploymentConfigs() {
        Template kieServerTemplate = new TemplateLoader(openShiftClient).loadKieServerTemplate();

        Deployment deployment = new Deployment(kieServerTemplate);
        List<DeploymentConfig> deploymentConfigs = deployment.getDeploymentConfigs();

        assertThat(deploymentConfigs).hasSize(1);
        assertThat(deploymentConfigs.get(0).getMetadata().getName()).isEqualTo("${APPLICATION_NAME}-kieserver");
    }
}
