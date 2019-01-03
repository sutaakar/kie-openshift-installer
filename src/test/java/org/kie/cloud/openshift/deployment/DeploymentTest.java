package org.kie.cloud.openshift.deployment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimBuilder;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.openshift.api.model.DeploymentConfig;
import io.fabric8.openshift.api.model.DeploymentConfigBuilder;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.api.model.RouteBuilder;
import org.junit.Test;
import org.kie.cloud.openshift.AbstractCloudTest;

public class DeploymentTest extends AbstractCloudTest{

    @Test
    public void testGetUnsecureServices() {
        Deployment deployment = getDeploymentWithServiceAndRouteCombinations("custom");
        List<Service> services = deployment.getUnsecureServices();

        assertThat(services).hasSize(1);
        assertThat(services.get(0).getMetadata().getName()).isEqualTo("unsecured-service");
    }

    @Test
    public void testGetSecureServices() {
        Deployment deployment = getDeploymentWithServiceAndRouteCombinations("custom");
        List<Service> services = deployment.getSecureServices();

        assertThat(services).hasSize(1);
        assertThat(services.get(0).getMetadata().getName()).isEqualTo("secured-service");
    }

    @Test
    public void testGetServices() {
        Deployment deployment = getDeploymentWithServiceAndRouteCombinations("custom");
        List<Service> services = deployment.getServices();

        assertThat(services).hasSize(2);
        assertThat(services).extracting(n -> n.getMetadata().getName()).contains("secured-service", "unsecured-service");
    }

    @Test
    public void testGetUnsecureRoutes() {
        Deployment deployment = getDeploymentWithServiceAndRouteCombinations("custom");
        List<Route> routes = deployment.getUnsecureRoutes();

        assertThat(routes).hasSize(1);
        assertThat(routes.get(0).getMetadata().getName()).isEqualTo("unsecure-route");
    }

    @Test
    public void testGetSecureRoutes() {
        Deployment deployment = getDeploymentWithServiceAndRouteCombinations("custom");
        List<Route> routes = deployment.getSecureRoutes();

        assertThat(routes).hasSize(1);
        assertThat(routes.get(0).getMetadata().getName()).isEqualTo("secure-route");
    }

    @Test
    public void testGetDeploymentConfig() {
        Deployment deployment = getDeploymentWithServiceAndRouteCombinations("custom");
        DeploymentConfig deploymentConfig = deployment.getDeploymentConfig();

        assertThat(deploymentConfig.getMetadata().getName()).isEqualTo("my-deployment");
    }

    @Test
    public void testGetEnvironmentVariableValue() {
        Deployment deployment = getDeploymentWithServiceAndRouteCombinations("custom");
        String environmentVariableValue = deployment.getEnvironmentVariableValue("custom-variable-name");

        assertThat(environmentVariableValue).isEqualTo("custom-variable-value");
    }

    @Test
    public void testGetEnvironmentVariableValueNotExisting() {
        Deployment deployment = getDeploymentWithServiceAndRouteCombinations("custom");

        assertThatThrownBy(() -> deployment.getEnvironmentVariableValue("not-existing-variable-name")).isInstanceOf(RuntimeException.class)
                                                                                                      .hasMessageContaining("Environment variable with name not-existing-variable-name not found.");
    }

    @Test
    public void testGetOptionalEnvironmentVariableValue() {
        Deployment deployment = getDeploymentWithServiceAndRouteCombinations("custom");
        Optional<String> environmentVariableValue = deployment.getOptionalEnvironmentVariableValue("custom-variable-name");
        Optional<String> notExistingVariableValue = deployment.getOptionalEnvironmentVariableValue("not-existing-variable-name");

        assertThat(environmentVariableValue).contains("custom-variable-value");
        assertThat(notExistingVariableValue).isEmpty();
    }

    @Test
    public void testGetPersistentVolumeClaims() {
        Deployment deployment = getDeploymentWithServiceAndRouteCombinations("custom");
        List<PersistentVolumeClaim> persistentVolumeClaims = deployment.getPersistentVolumeClaims();

        assertThat(persistentVolumeClaims).hasSize(1);
        assertThat(persistentVolumeClaims.get(0).getMetadata().getName()).isEqualTo("my-persistent-volume-claim");
    }

    private Deployment getDeploymentWithServiceAndRouteCombinations(String deploymentName) {
        Deployment deployment = new Deployment(deploymentName);

        Service unsecureService = new ServiceBuilder().withNewMetadata()
                                                          .withName("unsecured-service")
                                                      .endMetadata()
                                                      .build();
        deployment.getObjects().add(unsecureService);

        Route unsecureRoute = new RouteBuilder().withNewMetadata()
                                                    .withName("unsecure-route")
                                                .endMetadata()
                                                .withNewSpec()
                                                    .withNewTo()
                                                        .withKind("Service")
                                                        .withName("unsecured-service")
                                                    .endTo()
                                                .endSpec()
                                                .build();
        deployment.getObjects().add(unsecureRoute);

        Service secureService = new ServiceBuilder().withNewMetadata()
                                                        .withName("secured-service")
                                                    .endMetadata()
                                                    .build();
        deployment.getObjects().add(secureService);

        Route secureRoute = new RouteBuilder().withNewMetadata()
                                                  .withName("secure-route")
                                              .endMetadata()
                                              .withNewSpec()
                                                  .withNewTo()
                                                      .withKind("Service")
                                                      .withName("secured-service")
                                                  .endTo()
                                                  .withNewTls()
                                                      .withTermination("Edge")
                                                  .endTls()
                                              .endSpec()
                                              .build();
        deployment.getObjects().add(secureRoute);

        Container container = new ContainerBuilder().withEnv(new EnvVar("custom-variable-name", "custom-variable-value", null))
                                                    .build();
        DeploymentConfig deploymentConfig = new DeploymentConfigBuilder().withNewMetadata()
                                                                             .withName("my-deployment")
                                                                         .endMetadata()
                                                                         .withNewSpec()
                                                                             .withNewTemplate()
                                                                                 .withNewSpec()
                                                                                     .withContainers(container)
                                                                                 .endSpec()
                                                                             .endTemplate()
                                                                         .endSpec()
                                                                         .build();
        deployment.getObjects().add(deploymentConfig);

        PersistentVolumeClaim persistentVolumeClaim = new PersistentVolumeClaimBuilder().withNewMetadata()
                                                                                            .withName("my-persistent-volume-claim")
                                                                                        .endMetadata()
                                                                                        .build();
        deployment.getObjects().add(persistentVolumeClaim);

        return deployment;
    }
}
