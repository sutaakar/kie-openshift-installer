package org.kie.cloud.openshift.integrationtests;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import io.fabric8.kubernetes.api.model.ServiceList;
import io.fabric8.openshift.api.model.DeploymentConfigList;
import io.fabric8.openshift.api.model.Project;
import io.fabric8.openshift.api.model.RouteList;
import org.junit.Test;
import org.kie.cloud.openshift.AbstractCloudTest;
import org.kie.cloud.openshift.KieOpenShiftProvider;
import org.kie.cloud.openshift.OpenShiftImageConstants;
import org.kie.cloud.openshift.deployment.Deployment;
import org.kie.cloud.openshift.scenario.Scenario;

public class KieServerIntegrationTest extends AbstractCloudTest{

    @Test
    public void testCreateAndDeployKieServer() {
        final String projectName = "test-project-" + UUID.randomUUID().toString().substring(0, 4);

        try (KieOpenShiftProvider kieOpenShiftProvider = new KieOpenShiftProvider(openShiftClient)) {
            Deployment kieServerDeployment = kieOpenShiftProvider.createKieServerDeploymentBuilder()
                                                                      .withKieServerUser("john", "john123")
                                                                      .build();
            Scenario kieServerScenario = kieOpenShiftProvider.createScenario();
            kieServerScenario.addDeployment(kieServerDeployment);
            kieOpenShiftProvider.deployScenario(kieServerScenario, projectName);

            Project project = openShiftClient.projects().withName(projectName).get();
            assertThat(project).isNotNull();

            RouteList routes = openShiftClient.routes().inNamespace(projectName).list();
            assertThat(routes.getItems()).hasSize(1)
                                         .anySatisfy(n -> assertThat(n.getMetadata().getName()).isEqualTo("myapp-kieserver"));

            ServiceList services = openShiftClient.services().inNamespace(projectName).list();
            assertThat(services.getItems()).hasSize(1)
                                           .anySatisfy(n -> assertThat(n.getMetadata().getName()).isEqualTo("myapp-kieserver"));

            DeploymentConfigList deploymentConfigs = openShiftClient.deploymentConfigs().inNamespace(projectName).list();
            assertThat(deploymentConfigs.getItems()).hasSize(1);
            assertThat(deploymentConfigs.getItems().get(0).getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                                                                                           .anySatisfy(e -> {
                                                                                               assertThat(e.getName()).isEqualTo(OpenShiftImageConstants.KIE_SERVER_USER);
                                                                                               assertThat(e.getValue()).isEqualTo("john");
                                                                                           });
            assertThat(deploymentConfigs.getItems().get(0).getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                                                                                            .anySatisfy(e -> {
                                                                                                assertThat(e.getName()).isEqualTo(OpenShiftImageConstants.KIE_SERVER_PWD);
                                                                                                assertThat(e.getValue()).isEqualTo("john123");
                                                                                            });
        } finally {
            openShiftClient.projects().withName(projectName).delete();
        }
    }
}
