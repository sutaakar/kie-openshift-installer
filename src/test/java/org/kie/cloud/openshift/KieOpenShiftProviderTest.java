package org.kie.cloud.openshift;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.kie.cloud.openshift.scenario.Scenario;
import org.kie.cloud.openshift.scenario.builder.ScenarioBuilder;
import org.kie.cloud.openshift.settings.builder.KieServerDeploymentBuilder;
import org.kie.cloud.openshift.settings.builder.MySqlDeploymentBuilder;
import org.kie.cloud.openshift.settings.builder.PostgreSqlDeploymentBuilder;

public class KieOpenShiftProviderTest extends AbstractCloudTest{

    @Test
    public void testCreateKieServerDeploymentBuilder() {
        KieServerDeploymentBuilder kieServerSettingsBuilder = KieOpenShiftProvider.createKieServerDeploymentBuilder();
        assertThat(kieServerSettingsBuilder).isNotNull();
    }

    @Test
    public void testCreateKieServerDeploymentBuilderCustomDeploymentName() {
        KieServerDeploymentBuilder kieServerSettingsBuilder = KieOpenShiftProvider.createKieServerDeploymentBuilder("custom-server");
        assertThat(kieServerSettingsBuilder).isNotNull();
        assertThat(kieServerSettingsBuilder.build().getDeploymentName()).isEqualTo("custom-server");
    }

    @Test
    public void testCreateMySqlDeploymentBuilder() {
        MySqlDeploymentBuilder mySqlSettingsBuilder = KieOpenShiftProvider.createMySqlDeploymentBuilder();
        assertThat(mySqlSettingsBuilder).isNotNull();
    }

    @Test
    public void testCreateMySqlDeploymentBuilderCustomDeploymentName() {
        MySqlDeploymentBuilder mySqlSettingsBuilder = KieOpenShiftProvider.createMySqlDeploymentBuilder("custom-sql");
        assertThat(mySqlSettingsBuilder).isNotNull();
        assertThat(mySqlSettingsBuilder.build().getDeploymentName()).isEqualTo("custom-sql");
    }

    @Test
    public void testCreatePostgreSqlDeploymentBuilder() {
        PostgreSqlDeploymentBuilder postgreSqlDeploymentBuilder = KieOpenShiftProvider.createPostgreSqlDeploymentBuilder();
        assertThat(postgreSqlDeploymentBuilder).isNotNull();
    }

    @Test
    public void testCreatePostgreSqlDeploymentBuilderCustomDeploymentName() {
        PostgreSqlDeploymentBuilder postgreSqlDeploymentBuilder = KieOpenShiftProvider.createPostgreSqlDeploymentBuilder("custom-sql");
        assertThat(postgreSqlDeploymentBuilder).isNotNull();
        assertThat(postgreSqlDeploymentBuilder.build().getDeploymentName()).isEqualTo("custom-sql");
    }

    @Test
    public void testCreateScenario() {
        ScenarioBuilder scenarioBuilder = KieOpenShiftProvider.createScenarioBuilder();
        assertThat(scenarioBuilder).isNotNull();
    }
}
