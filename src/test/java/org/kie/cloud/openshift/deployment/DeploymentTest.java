package org.kie.cloud.openshift.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.openshift.api.model.DeploymentConfig;
import io.fabric8.openshift.api.model.DeploymentConfigBuilder;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.api.model.RouteBuilder;
import io.fabric8.openshift.api.model.Template;
import io.fabric8.openshift.api.model.TemplateBuilder;
import org.junit.Test;
import org.kie.cloud.openshift.AbstractCloudTest;

public class DeploymentTest extends AbstractCloudTest{

    @Test
    public void testGetUnsecureServices() {
        Service unsecureService = new ServiceBuilder().withNewMetadata()
                                                          .withName("unsecured-service")
                                                      .endMetadata()
                                                      .build();
        Route unsecureRoute = new RouteBuilder().withNewSpec()
                                                    .withNewTo()
                                                        .withKind("Service")
                                                        .withName("unsecured-service")
                                                    .endTo()
                                                .endSpec()
                                                .build();
        Template template = new TemplateBuilder().withObjects(unsecureService, unsecureRoute)
                                                 .build();

        Deployment deployment = new Deployment(template);
        List<Service> services = deployment.getUnsecureServices();

        assertThat(services).hasSize(1);
        assertThat(services.get(0).getMetadata().getName()).isEqualTo("unsecured-service");
    }

    @Test
    public void testGetSecureServices() {
        Service secureService = new ServiceBuilder().withNewMetadata()
                                                        .withName("secured-service")
                                                    .endMetadata()
                                                    .build();
        Route secureRoute = new RouteBuilder().withNewSpec()
                                                  .withNewTo()
                                                      .withKind("Service")
                                                      .withName("secured-service")
                                                  .endTo()
                                                  .withNewTls()
                                                      .withTermination("Edge")
                                                  .endTls()
                                              .endSpec()
                                              .build();
        Template template = new TemplateBuilder().withObjects(secureService, secureRoute)
                                                 .build();

        Deployment deployment = new Deployment(template);
        List<Service> services = deployment.getSecureServices();

        assertThat(services).hasSize(1);
        assertThat(services.get(0).getMetadata().getName()).isEqualTo("secured-service");
    }

    @Test
    public void testGetUnsecureRoutes() {
        Route unsecureRoute = new RouteBuilder().withNewMetadata()
                                                    .withName("unsecure-route")
                                                .endMetadata()
                                                .withNewSpec()
                                                .endSpec()
                                                .build();
        Template template = new TemplateBuilder().withObjects(unsecureRoute)
                                                 .build();

        Deployment deployment = new Deployment(template);
        List<Route> routes = deployment.getUnsecureRoutes();

        assertThat(routes).hasSize(1);
        assertThat(routes.get(0).getMetadata().getName()).isEqualTo("unsecure-route");
    }

    @Test
    public void testGetSecureRoutes() {
        Route unsecureRoute = new RouteBuilder().withNewMetadata()
                                                    .withName("secure-route")
                                                .endMetadata()
                                                .withNewSpec()
                                                    .withNewTls()
                                                        .withTermination("Edge")
                                                    .endTls()
                                                .endSpec()
                                                .build();
        Template template = new TemplateBuilder().withObjects(unsecureRoute)
                                                 .build();

        Deployment deployment = new Deployment(template);
        List<Route> routes = deployment.getSecureRoutes();

        assertThat(routes).hasSize(1);
        assertThat(routes.get(0).getMetadata().getName()).isEqualTo("secure-route");
    }

    @Test
    public void testGetDeploymentConfigs() {
        DeploymentConfig deploymentConfig = new DeploymentConfigBuilder().withNewMetadata()
                                                                             .withName("my-deployment")
                                                                         .endMetadata()
                                                                         .build();
        Template template = new TemplateBuilder().withObjects(deploymentConfig)
                                                 .build();

        Deployment deployment = new Deployment(template);
        List<DeploymentConfig> deploymentConfigs = deployment.getDeploymentConfigs();

        assertThat(deploymentConfigs).hasSize(1);
        assertThat(deploymentConfigs.get(0).getMetadata().getName()).isEqualTo("my-deployment");
    }
}
