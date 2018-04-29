package org.kie.cloud.openshift.settings.builder;

import static org.assertj.core.api.Assertions.assertThat;

import io.fabric8.openshift.api.model.Template;
import org.junit.Test;
import org.kie.cloud.openshift.AbstractCloudTest;
import org.kie.cloud.openshift.OpenShiftImageConstants;
import org.kie.cloud.openshift.deployment.Deployment;
import org.kie.cloud.openshift.template.TemplateLoader;

public class MySqlDeploymentBuilderTest extends AbstractCloudTest{

    @Test
    public void testBuildMySqlDeployment() {
        Template mySqlTemplate = new TemplateLoader(openShiftClient).loadMySqlTemplate();

        MySqlDeploymentBuilder settingsBuilder = new MySqlDeploymentBuilder(mySqlTemplate);
        Deployment builtMySqlDeployment = settingsBuilder.build();

        assertThat(builtMySqlDeployment).isNotNull();
        assertThat(builtMySqlDeployment.geTemplate().getMetadata().getName()).contains("-mysql");
    }

    @Test
    public void testBuildMySqlDeploymentMySqlUser() {
        Template mySqlTemplate = new TemplateLoader(openShiftClient).loadMySqlTemplate();

        MySqlDeploymentBuilder settingsBuilder = new MySqlDeploymentBuilder(mySqlTemplate);
        Deployment builtMySqlDeployment = settingsBuilder.withUser("mySqlName", "mySqlPassword")
                                                         .build();

        assertThat(builtMySqlDeployment).isNotNull();
        assertThat(builtMySqlDeployment.getDeploymentConfigs()).hasSize(1);
        assertThat(builtMySqlDeployment.getDeploymentConfigs().get(0).getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> OpenShiftImageConstants.MYSQL_USER.equals(e.getName()))
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo("mySqlName"));
        assertThat(builtMySqlDeployment.getDeploymentConfigs().get(0).getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> OpenShiftImageConstants.MYSQL_PASSWORD.equals(e.getName()))
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo("mySqlPassword"));
    }

    @Test
    public void testBuildMySqlDeploymentDbName() {
        Template mySqlTemplate = new TemplateLoader(openShiftClient).loadMySqlTemplate();

        MySqlDeploymentBuilder settingsBuilder = new MySqlDeploymentBuilder(mySqlTemplate);
        Deployment builtMySqlDeployment = settingsBuilder.withDatabaseName("custom-db")
                                                         .build();

        assertThat(builtMySqlDeployment).isNotNull();
        assertThat(builtMySqlDeployment.getDeploymentConfigs()).hasSize(1);
        assertThat(builtMySqlDeployment.getDeploymentConfigs().get(0).getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> OpenShiftImageConstants.MYSQL_DATABASE.equals(e.getName()))
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo("custom-db"));
    }
}
