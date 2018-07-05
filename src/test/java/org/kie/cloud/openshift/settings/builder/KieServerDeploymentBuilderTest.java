package org.kie.cloud.openshift.settings.builder;

import static org.assertj.core.api.Assertions.assertThat;

import io.fabric8.openshift.api.model.Template;
import org.junit.Test;
import org.kie.cloud.openshift.AbstractCloudTest;
import org.kie.cloud.openshift.OpenShiftImageConstants;
import org.kie.cloud.openshift.deployment.Deployment;
import org.kie.cloud.openshift.template.TemplateLoader;

public class KieServerDeploymentBuilderTest extends AbstractCloudTest{

    @Test
    public void testBuildKieServerDeployment() {
        Template kieServerTemplate = new TemplateLoader(openShiftClient).loadKieServerTemplate();

        KieServerDeploymentBuilder settingsBuilder = new KieServerDeploymentBuilder(kieServerTemplate);
        Deployment builtKieServerDeployment = settingsBuilder.build();

        assertThat(builtKieServerDeployment).isNotNull();
        assertThat(builtKieServerDeployment.geTemplate().getMetadata().getName()).contains("-kieserver");
        assertThat(builtKieServerDeployment.geTemplate().getParameters()).extracting(n -> n.getName()).contains("APPLICATION_NAME");
    }

    @Test
    public void testBuildKieServerDeploymentWithApplicationName() {
        Template kieServerTemplate = new TemplateLoader(openShiftClient).loadKieServerTemplate();

        KieServerDeploymentBuilder settingsBuilder = new KieServerDeploymentBuilder(kieServerTemplate);
        Deployment builtKieServerDeployment = settingsBuilder.withApplicationName("myappname")
                                                             .build();

        assertThat(builtKieServerDeployment).isNotNull();
        assertThat(builtKieServerDeployment.geTemplate().getObjects()).extracting(n -> n.getMetadata().getName()).allMatch(s -> s.startsWith("myappname-"));
        assertThat(builtKieServerDeployment.geTemplate().getParameters()).extracting(n -> n.getName()).doesNotContain("APPLICATION_NAME");
    }

    @Test
    public void testBuildKieServerDeploymentWithKieServerUser() {
        Template kieServerTemplate = new TemplateLoader(openShiftClient).loadKieServerTemplate();

        KieServerDeploymentBuilder settingsBuilder = new KieServerDeploymentBuilder(kieServerTemplate);
        Deployment builtKieServerDeployment = settingsBuilder.withKieServerUser("kieServerName", "kieServerPassword")
                                                             .build();

        assertThat(builtKieServerDeployment).isNotNull();
        assertThat(builtKieServerDeployment.getDeploymentConfigs()).hasSize(1);
        assertThat(builtKieServerDeployment.getDeploymentConfigs().get(0).getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> OpenShiftImageConstants.KIE_SERVER_USER.equals(e.getName()))
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo("kieServerName"));
        assertThat(builtKieServerDeployment.getDeploymentConfigs().get(0).getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> OpenShiftImageConstants.KIE_SERVER_PWD.equals(e.getName()))
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo("kieServerPassword"));
    }

    @Test
    public void testBuildKieServerDeploymentWithMySqlDatabase() {
        Template mySqlTemplate = new TemplateLoader(openShiftClient).loadMySqlTemplate();
        Template kieServerTemplate = new TemplateLoader(openShiftClient).loadKieServerTemplate();

        MySqlDeploymentBuilder mySqlSettingsBuilder = new MySqlDeploymentBuilder(mySqlTemplate);
        Deployment builtMySqlDeployment = mySqlSettingsBuilder.withDatabaseName("custom-db")
                                                              .withDatabaseUser("mySqlName", "mySqlPassword")
                                                              .build();

        KieServerDeploymentBuilder kieServerSettingsBuilder = new KieServerDeploymentBuilder(kieServerTemplate);
        Deployment builtKieServerDeployment = kieServerSettingsBuilder.connectToMySqlDatabase(builtMySqlDeployment)
                                                                      .build();

        assertThat(builtKieServerDeployment).isNotNull();
        assertThat(builtKieServerDeployment.getDeploymentConfigs()).hasSize(1);
        assertThat(builtKieServerDeployment.getDeploymentConfigs().get(0).getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> OpenShiftImageConstants.DATASOURCES.equals(e.getName()))
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo(OpenShiftImageConstants.DATASOURCES_KIE));
        assertThat(builtKieServerDeployment.getDeploymentConfigs().get(0).getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> OpenShiftImageConstants.KIE_DATABASE.equals(e.getName()))
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo("custom-db"));
        assertThat(builtKieServerDeployment.getDeploymentConfigs().get(0).getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> OpenShiftImageConstants.KIE_JNDI.equals(e.getName()))
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo("java:/jboss/datasources/kie"));
        assertThat(builtKieServerDeployment.getDeploymentConfigs().get(0).getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> OpenShiftImageConstants.KIE_DRIVER.equals(e.getName()))
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo("mysql"));
        assertThat(builtKieServerDeployment.getDeploymentConfigs().get(0).getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> OpenShiftImageConstants.KIE_JTA.equals(e.getName()))
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo("true"));
        assertThat(builtKieServerDeployment.getDeploymentConfigs().get(0).getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> OpenShiftImageConstants.KIE_TX_ISOLATION.equals(e.getName()))
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo("TRANSACTION_READ_COMMITTED"));
        assertThat(builtKieServerDeployment.getDeploymentConfigs().get(0).getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> OpenShiftImageConstants.KIE_USERNAME.equals(e.getName()))
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo("mySqlName"));
        assertThat(builtKieServerDeployment.getDeploymentConfigs().get(0).getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> OpenShiftImageConstants.KIE_PASSWORD.equals(e.getName()))
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo("mySqlPassword"));
        assertThat(builtKieServerDeployment.getDeploymentConfigs().get(0).getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> OpenShiftImageConstants.KIE_SERVICE_HOST.equals(e.getName()))
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo("${APPLICATION_NAME}-mysql"));
        assertThat(builtKieServerDeployment.getDeploymentConfigs().get(0).getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> OpenShiftImageConstants.KIE_SERVICE_PORT.equals(e.getName()))
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo("3306"));
        assertThat(builtKieServerDeployment.getDeploymentConfigs().get(0).getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> OpenShiftImageConstants.TIMER_SERVICE_DATA_STORE.equals(e.getName()))
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo("${APPLICATION_NAME}-mysql"));
        assertThat(builtKieServerDeployment.getDeploymentConfigs().get(0).getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> OpenShiftImageConstants.TIMER_SERVICE_DATA_STORE_REFRESH_INTERVAL.equals(e.getName()))
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo("30000"));
        assertThat(builtKieServerDeployment.getDeploymentConfigs().get(0).getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> OpenShiftImageConstants.KIE_SERVER_PERSISTENCE_DIALECT.equals(e.getName()))
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo("org.hibernate.dialect.MySQL5Dialect"));
        assertThat(builtKieServerDeployment.getDeploymentConfigs().get(0).getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> OpenShiftImageConstants.KIE_SERVER_PERSISTENCE_DS.equals(e.getName()))
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo("java:/jboss/datasources/kie"));
    }

    @Test
    public void testBuildKieServerDeploymentWithPostgreSqlDatabase() {
        Template postgreSqlTemplate = new TemplateLoader(openShiftClient).loadPostgreSqlTemplate();
        Template kieServerTemplate = new TemplateLoader(openShiftClient).loadKieServerTemplate();

        PostgreSqlDeploymentBuilder postgreSqlSettingsBuilder = new PostgreSqlDeploymentBuilder(postgreSqlTemplate);
        Deployment builtPostgreSqlDeployment = postgreSqlSettingsBuilder.withDatabaseName("custom-db")
                                                                        .withDatabaseUser("postgreSqlName", "postgreSqlPassword")
                                                                        .build();

        KieServerDeploymentBuilder kieServerSettingsBuilder = new KieServerDeploymentBuilder(kieServerTemplate);
        Deployment builtKieServerDeployment = kieServerSettingsBuilder.connectToPostgreSqlDatabase(builtPostgreSqlDeployment)
                                                                      .build();

        assertThat(builtKieServerDeployment).isNotNull();
        assertThat(builtKieServerDeployment.getDeploymentConfigs()).hasSize(1);
        assertThat(builtKieServerDeployment.getDeploymentConfigs().get(0).getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> OpenShiftImageConstants.DATASOURCES.equals(e.getName()))
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo(OpenShiftImageConstants.DATASOURCES_KIE));
        assertThat(builtKieServerDeployment.getDeploymentConfigs().get(0).getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> OpenShiftImageConstants.KIE_DATABASE.equals(e.getName()))
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo("custom-db"));
        assertThat(builtKieServerDeployment.getDeploymentConfigs().get(0).getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> OpenShiftImageConstants.KIE_JNDI.equals(e.getName()))
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo("java:/jboss/datasources/kie"));
        assertThat(builtKieServerDeployment.getDeploymentConfigs().get(0).getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> OpenShiftImageConstants.KIE_DRIVER.equals(e.getName()))
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo("postgresql"));
        assertThat(builtKieServerDeployment.getDeploymentConfigs().get(0).getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> OpenShiftImageConstants.KIE_JTA.equals(e.getName()))
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo("true"));
        assertThat(builtKieServerDeployment.getDeploymentConfigs().get(0).getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> OpenShiftImageConstants.KIE_TX_ISOLATION.equals(e.getName()))
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo("TRANSACTION_READ_COMMITTED"));
        assertThat(builtKieServerDeployment.getDeploymentConfigs().get(0).getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> OpenShiftImageConstants.KIE_USERNAME.equals(e.getName()))
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo("postgreSqlName"));
        assertThat(builtKieServerDeployment.getDeploymentConfigs().get(0).getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> OpenShiftImageConstants.KIE_PASSWORD.equals(e.getName()))
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo("postgreSqlPassword"));
        assertThat(builtKieServerDeployment.getDeploymentConfigs().get(0).getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> OpenShiftImageConstants.KIE_SERVICE_HOST.equals(e.getName()))
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo("${APPLICATION_NAME}-postgresql"));
        assertThat(builtKieServerDeployment.getDeploymentConfigs().get(0).getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> OpenShiftImageConstants.KIE_SERVICE_PORT.equals(e.getName()))
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo("5432"));
        assertThat(builtKieServerDeployment.getDeploymentConfigs().get(0).getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> OpenShiftImageConstants.TIMER_SERVICE_DATA_STORE.equals(e.getName()))
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo("${APPLICATION_NAME}-postgresql"));
        assertThat(builtKieServerDeployment.getDeploymentConfigs().get(0).getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> OpenShiftImageConstants.TIMER_SERVICE_DATA_STORE_REFRESH_INTERVAL.equals(e.getName()))
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo("30000"));
        assertThat(builtKieServerDeployment.getDeploymentConfigs().get(0).getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> OpenShiftImageConstants.KIE_SERVER_PERSISTENCE_DIALECT.equals(e.getName()))
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo("org.hibernate.dialect.PostgreSQLDialect"));
        assertThat(builtKieServerDeployment.getDeploymentConfigs().get(0).getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> OpenShiftImageConstants.KIE_SERVER_PERSISTENCE_DS.equals(e.getName()))
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo("java:/jboss/datasources/kie"));
    }
}
