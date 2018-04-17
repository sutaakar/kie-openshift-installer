package org.kie.cloud.openshift;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.kie.cloud.openshift.scenario.Scenario;
import org.kie.cloud.openshift.settings.builder.DeploymentBuilder;

public class KieOpenShiftProviderTest extends AbstractCloudTest{

    @Test
    public void testCreateKieServerDeploymentBuilder() {
        try (KieOpenShiftProvider kieOpenShiftProvider = new KieOpenShiftProvider(getOfflineOpenShiftClient())) {
            DeploymentBuilder kieServerSettingsBuilder = kieOpenShiftProvider.createKieServerDeploymentBuilder();
            assertThat(kieServerSettingsBuilder).isNotNull();
        }
    }

    @Test
    public void testCreateScenario() {
        try (KieOpenShiftProvider kieOpenShiftProvider = new KieOpenShiftProvider(getOfflineOpenShiftClient())) {
            Scenario scenario = kieOpenShiftProvider.createScenario("test-project");
            assertThat(scenario).isNotNull();
        }
    }
}
