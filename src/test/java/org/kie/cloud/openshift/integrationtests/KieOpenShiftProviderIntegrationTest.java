package org.kie.cloud.openshift.integrationtests;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

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
            kieOpenShiftProvider.deployScenario(scenario, projectName, Collections.emptyMap());

            Project project = openShiftClient.projects().withName(projectName).get();
            assertThat(project).isNotNull();
        } finally {
            openShiftClient.projects().withName(projectName).delete();
        }
    }

    @Test
    public void testDeployEmptyScenarioExistingProject() {
        final String projectName = "test-project-" + UUID.randomUUID().toString().substring(0, 4);

        try (KieOpenShiftProvider kieOpenShiftProvider = new KieOpenShiftProvider(openShiftClient)) {
            Scenario scenario = kieOpenShiftProvider.createScenario();
            kieOpenShiftProvider.deployScenario(scenario, projectName, Collections.emptyMap());

            waitUntilProjectExists(projectName);

            kieOpenShiftProvider.deployScenario(scenario, projectName, Collections.emptyMap());

            Project project = openShiftClient.projects().withName(projectName).get();
            assertThat(project).isNotNull();
        } finally {
            openShiftClient.projects().withName(projectName).delete();
        }
    }

    private void waitUntilProjectExists(String projectName) {
        Duration waitingDuration = Duration.ofSeconds(30);
        Instant timeout = Instant.now().plus(waitingDuration);

        while(Instant.now().isBefore(timeout)) {
            Optional<Project> foundProject = openShiftClient.projects().list().getItems().stream()
                                                                                         .filter(n -> n.getMetadata().getName().equals(projectName))
                                                                                         .findAny();
            if(foundProject.isPresent()) {
                return;
            } else {
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
}
