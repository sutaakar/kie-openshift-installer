package org.kie.cloud.openshift;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.kie.cloud.openshift.scenario.Scenario;
import org.kie.cloud.openshift.settings.builder.KieServerDeploymentBuilder;
import org.kie.cloud.openshift.settings.builder.MySqlDeploymentBuilder;
import org.kie.cloud.openshift.settings.builder.PostgreSqlDeploymentBuilder;

public class KieOpenShiftProviderTest extends AbstractCloudTest{

    @Test
    public void testCreateKieServerDeploymentBuilder() {
        try (KieOpenShiftProvider kieOpenShiftProvider = new KieOpenShiftProvider(openShiftClient)) {
            KieServerDeploymentBuilder kieServerSettingsBuilder = kieOpenShiftProvider.createKieServerDeploymentBuilder();
            assertThat(kieServerSettingsBuilder).isNotNull();
        }
    }

    @Test
    public void testCreateMySqlDeploymentBuilder() {
        try (KieOpenShiftProvider kieOpenShiftProvider = new KieOpenShiftProvider(openShiftClient)) {
            MySqlDeploymentBuilder mySqlSettingsBuilder = kieOpenShiftProvider.createMySqlDeploymentBuilder();
            assertThat(mySqlSettingsBuilder).isNotNull();
        }
    }

    @Test
    public void testCreatePostgreSqlDeploymentBuilder() {
        try (KieOpenShiftProvider kieOpenShiftProvider = new KieOpenShiftProvider(openShiftClient)) {
            PostgreSqlDeploymentBuilder postgreSqlDeploymentBuilder = kieOpenShiftProvider.createPostgreSqlDeploymentBuilder();
            assertThat(postgreSqlDeploymentBuilder).isNotNull();
        }
    }

    @Test
    public void testCreateScenario() {
        try (KieOpenShiftProvider kieOpenShiftProvider = new KieOpenShiftProvider(openShiftClient)) {
            Scenario scenario = kieOpenShiftProvider.createScenario();
            assertThat(scenario).isNotNull();
        }
    }
}
