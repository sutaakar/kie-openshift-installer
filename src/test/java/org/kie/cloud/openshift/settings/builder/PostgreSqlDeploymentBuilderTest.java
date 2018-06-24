package org.kie.cloud.openshift.settings.builder;

import static org.assertj.core.api.Assertions.assertThat;

import io.fabric8.openshift.api.model.Template;
import org.junit.Test;
import org.kie.cloud.openshift.AbstractCloudTest;
import org.kie.cloud.openshift.OpenShiftImageConstants;
import org.kie.cloud.openshift.deployment.Deployment;
import org.kie.cloud.openshift.template.TemplateLoader;

public class PostgreSqlDeploymentBuilderTest extends AbstractCloudTest{

    @Test
    public void testBuildPostgreSqlDeployment() {
        Template postgreSqlTemplate = new TemplateLoader(openShiftClient).loadPostgeSqlTemplate();

        PostgreSqlDeploymentBuilder settingsBuilder = new PostgreSqlDeploymentBuilder(postgreSqlTemplate);
        Deployment builtPostgreSqlDeployment = settingsBuilder.build();

        assertThat(builtPostgreSqlDeployment).isNotNull();
        assertThat(builtPostgreSqlDeployment.geTemplate().getMetadata().getName()).contains("-postgresql");
    }

    @Test
    public void testBuildPostgreSqlDeploymentDefaultValues() {
        Template postgreSqlTemplate = new TemplateLoader(openShiftClient).loadPostgeSqlTemplate();

        PostgreSqlDeploymentBuilder settingsBuilder = new PostgreSqlDeploymentBuilder(postgreSqlTemplate);
        Deployment builtPostgreSqlDeployment = settingsBuilder.build();

        assertThat(builtPostgreSqlDeployment).isNotNull();
        assertThat(builtPostgreSqlDeployment.getDeploymentConfigs()).hasSize(1);
        assertThat(builtPostgreSqlDeployment.getDeploymentConfigs().get(0).getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> OpenShiftImageConstants.POSTGRESQL_DATABASE.equals(e.getName()))
                        .isNotEmpty();
        assertThat(builtPostgreSqlDeployment.getDeploymentConfigs().get(0).getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> OpenShiftImageConstants.POSTGRESQL_USER.equals(e.getName()))
                        .isNotEmpty();
        assertThat(builtPostgreSqlDeployment.getDeploymentConfigs().get(0).getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> OpenShiftImageConstants.POSTGRESQL_PASSWORD.equals(e.getName()))
                        .isNotEmpty();
        assertThat(builtPostgreSqlDeployment.getDeploymentConfigs().get(0).getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> OpenShiftImageConstants.POSTGRESQL_MAX_PREPARED_TRANSACTIONS.equals(e.getName()))
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo("100"));
    }

    @Test
    public void testBuildPostgreSqlDeploymentWithPostgreSqlUser() {
        Template postgreSqlTemplate = new TemplateLoader(openShiftClient).loadPostgeSqlTemplate();

        PostgreSqlDeploymentBuilder settingsBuilder = new PostgreSqlDeploymentBuilder(postgreSqlTemplate);
        Deployment builtPostgreSqlDeployment = settingsBuilder.withDatabaseUser("postgreSqlName", "postgreSqlPassword")
                                                              .build();

        assertThat(builtPostgreSqlDeployment).isNotNull();
        assertThat(builtPostgreSqlDeployment.getDeploymentConfigs()).hasSize(1);
        assertThat(builtPostgreSqlDeployment.getDeploymentConfigs().get(0).getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> OpenShiftImageConstants.POSTGRESQL_USER.equals(e.getName()))
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo("postgreSqlName"));
        assertThat(builtPostgreSqlDeployment.getDeploymentConfigs().get(0).getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> OpenShiftImageConstants.POSTGRESQL_PASSWORD.equals(e.getName()))
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo("postgreSqlPassword"));
    }

    @Test
    public void testBuildPostgreSqlDeploymentWithDbName() {
        Template postgreSqlTemplate = new TemplateLoader(openShiftClient).loadPostgeSqlTemplate();

        PostgreSqlDeploymentBuilder settingsBuilder = new PostgreSqlDeploymentBuilder(postgreSqlTemplate);
        Deployment builtPostgreSqlDeployment = settingsBuilder.withDatabaseName("custom-db")
                                                              .build();

        assertThat(builtPostgreSqlDeployment).isNotNull();
        assertThat(builtPostgreSqlDeployment.getDeploymentConfigs()).hasSize(1);
        assertThat(builtPostgreSqlDeployment.getDeploymentConfigs().get(0).getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> OpenShiftImageConstants.POSTGRESQL_DATABASE.equals(e.getName()))
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo("custom-db"));
    }
}
