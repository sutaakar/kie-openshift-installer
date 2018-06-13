package org.kie.cloud.openshift.integrationtests;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import io.fabric8.openshift.api.model.Project;
import org.junit.Test;
import org.kie.cloud.openshift.AbstractCloudTest;
import org.kie.cloud.openshift.KieOpenShiftProvider;
import org.kie.cloud.openshift.scenario.Scenario;

public class KieOpenShiftProviderIntegrationTest extends AbstractCloudTest{

    @Test
    public void testDeployEmptyScenario() {
        final String projectName = "test-project-" + UUID.randomUUID().toString().substring(0, 4);

        try (KieOpenShiftProvider kieOpenShiftProvider = new KieOpenShiftProvider(openShiftClient)) {
            Scenario scenario = kieOpenShiftProvider.createScenario();
            kieOpenShiftProvider.deployScenario(scenario, projectName);

            Project project = openShiftClient.projects().withName(projectName).get();
            assertThat(project).isNotNull();
        } finally {
            openShiftClient.projects().withName(projectName).delete();
        }
    }
}
