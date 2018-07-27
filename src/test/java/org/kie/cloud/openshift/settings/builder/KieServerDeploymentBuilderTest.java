package org.kie.cloud.openshift.settings.builder;

import static org.assertj.core.api.Assertions.assertThat;

import io.fabric8.kubernetes.api.model.Probe;
import io.fabric8.kubernetes.api.model.Quantity;
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
    public void testBuildKieServerDeploymentDeploymentConfig() {
        Template kieServerTemplate = new TemplateLoader(openShiftClient).loadKieServerTemplate();

        KieServerDeploymentBuilder settingsBuilder = new KieServerDeploymentBuilder(kieServerTemplate);
        Deployment builtKieServerDeployment = settingsBuilder.build();

        assertThat(builtKieServerDeployment).isNotNull();
        assertThat(builtKieServerDeployment.getDeploymentConfig().getApiVersion()).isEqualTo("v1");
        assertThat(builtKieServerDeployment.getDeploymentConfig().getMetadata().getName()).isEqualTo("${APPLICATION_NAME}-kieserver");
        assertThat(builtKieServerDeployment.getDeploymentConfig().getSpec().getStrategy().getType()).isEqualTo("Recreate");
        assertThat(builtKieServerDeployment.getDeploymentConfig().getSpec().getTriggers()).hasSize(2);
        assertThat(builtKieServerDeployment.getDeploymentConfig().getSpec().getTriggers())
                    .filteredOn(t -> t.getType().equals("ConfigChange"))
                    .hasSize(1);
        assertThat(builtKieServerDeployment.getDeploymentConfig().getSpec().getTriggers())
                    .filteredOn(t -> t.getType().equals("ImageChange"))
                    .hasOnlyOneElementSatisfying(e -> {
                        assertThat(e.getImageChangeParams().getAutomatic()).isTrue();
                        assertThat(e.getImageChangeParams().getContainerNames()).containsExactly("${APPLICATION_NAME}-kieserver");
                        assertThat(e.getImageChangeParams().getFrom().getKind()).isEqualTo("ImageStreamTag");
                        assertThat(e.getImageChangeParams().getFrom().getNamespace()).isEqualTo("${IMAGE_STREAM_NAMESPACE}");
                        assertThat(e.getImageChangeParams().getFrom().getName()).isEqualTo("rhpam70-kieserver-openshift:${IMAGE_STREAM_TAG}");
                    });
        assertThat(builtKieServerDeployment.getDeploymentConfig().getSpec().getReplicas()).isEqualTo(1);
        assertThat(builtKieServerDeployment.getDeploymentConfig().getSpec().getSelector()).containsEntry("deploymentConfig", "${APPLICATION_NAME}-kieserver");
        assertThat(builtKieServerDeployment.getDeploymentConfig().getSpec().getTemplate().getMetadata().getName()).isEqualTo("${APPLICATION_NAME}-kieserver");
        assertThat(builtKieServerDeployment.getDeploymentConfig().getSpec().getTemplate().getMetadata().getLabels()).containsEntry("deploymentConfig", "${APPLICATION_NAME}-kieserver");
        assertThat(builtKieServerDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getTerminationGracePeriodSeconds()).isEqualTo(60L);
        assertThat(builtKieServerDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers())
                    .hasOnlyOneElementSatisfying(c -> {
                        assertThat(c.getName()).isEqualTo("${APPLICATION_NAME}-kieserver");
                        assertThat(c.getImage()).isEqualTo("rhpam70-kieserver-openshift");
                        assertThat(c.getImagePullPolicy()).isEqualTo("Always");
                        assertThat(c.getResources().getLimits()).containsEntry("memory", new Quantity("${EXCECUTION_SERVER_MEMORY_LIMIT}"));
                        assertThat(c.getPorts()).hasSize(2);
                        assertThat(c.getPorts().get(0).getName()).isEqualTo("jolokia");
                        assertThat(c.getPorts().get(0).getContainerPort()).isEqualTo(8778);
                        assertThat(c.getPorts().get(0).getProtocol()).isEqualTo("TCP");
                        assertThat(c.getPorts().get(1).getName()).isEqualTo("http");
                        assertThat(c.getPorts().get(1).getContainerPort()).isEqualTo(8080);
                        assertThat(c.getPorts().get(1).getProtocol()).isEqualTo("TCP");
                    });
    }

    @Test
    public void testBuildKieServerDeploymentService() {
        Template kieServerTemplate = new TemplateLoader(openShiftClient).loadKieServerTemplate();

        KieServerDeploymentBuilder settingsBuilder = new KieServerDeploymentBuilder(kieServerTemplate);
        Deployment builtKieServerDeployment = settingsBuilder.build();

        assertThat(builtKieServerDeployment).isNotNull();
        assertThat(builtKieServerDeployment.getServices()).hasSize(1);
        assertThat(builtKieServerDeployment.getServices().get(0).getApiVersion()).isEqualTo("v1");
        assertThat(builtKieServerDeployment.getServices().get(0).getSpec().getPorts()).hasSize(1);
        assertThat(builtKieServerDeployment.getServices().get(0).getSpec().getPorts().get(0).getName()).isEqualTo("http");
        assertThat(builtKieServerDeployment.getServices().get(0).getSpec().getPorts().get(0).getPort()).isEqualTo(8080);
        assertThat(builtKieServerDeployment.getServices().get(0).getSpec().getPorts().get(0).getTargetPort().getIntVal()).isEqualTo(8080);
        assertThat(builtKieServerDeployment.getServices().get(0).getSpec().getSelector()).containsEntry("deploymentConfig", "${APPLICATION_NAME}-kieserver");
        assertThat(builtKieServerDeployment.getServices().get(0).getMetadata().getName()).isEqualTo("${APPLICATION_NAME}-kieserver");
    }

    @Test
    public void testBuildKieServerDeploymentRoute() {
        Template kieServerTemplate = new TemplateLoader(openShiftClient).loadKieServerTemplate();

        KieServerDeploymentBuilder settingsBuilder = new KieServerDeploymentBuilder(kieServerTemplate);
        Deployment builtKieServerDeployment = settingsBuilder.build();

        assertThat(builtKieServerDeployment).isNotNull();
        assertThat(builtKieServerDeployment.getUnsecureRoutes()).hasSize(1);
        assertThat(builtKieServerDeployment.getUnsecureRoutes().get(0).getApiVersion()).isEqualTo("v1");
        assertThat(builtKieServerDeployment.getUnsecureRoutes().get(0).getMetadata().getName()).isEqualTo("${APPLICATION_NAME}-kieserver");
        assertThat(builtKieServerDeployment.getUnsecureRoutes().get(0).getSpec().getTo().getName()).isEqualTo("${APPLICATION_NAME}-kieserver");
    }

    @Test
    public void testBuildKieServerDeploymentDefaultValues() {
        Template kieServerTemplate = new TemplateLoader(openShiftClient).loadKieServerTemplate();

        KieServerDeploymentBuilder settingsBuilder = new KieServerDeploymentBuilder(kieServerTemplate);
        Deployment builtKieServerDeployment = settingsBuilder.build();

        assertThat(builtKieServerDeployment).isNotNull();
        assertThat(builtKieServerDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> OpenShiftImageConstants.KIE_SERVER_USER.equals(e.getName()))
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo("executionUser"));
        assertThat(builtKieServerDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> OpenShiftImageConstants.KIE_SERVER_PWD.equals(e.getName()))
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo("executionUser1!"));
    }

    @Test
    public void testBuildKieServerDeploymentLivenessProbe() {
        Template kieServerTemplate = new TemplateLoader(openShiftClient).loadKieServerTemplate();

        KieServerDeploymentBuilder settingsBuilder = new KieServerDeploymentBuilder(kieServerTemplate);
        Deployment builtKieServerDeployment = settingsBuilder.withKieServerUser("kieServerName", "kieServerPassword").build();
        Probe livenessProbe = builtKieServerDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0).getLivenessProbe();

        assertThat(livenessProbe).isNotNull();
        assertThat(livenessProbe.getExec().getCommand()).containsExactly("/bin/bash", "-c", "curl --fail --silent -u 'kieServerName:kieServerPassword' http://localhost:8080/services/rest/server/healthcheck");
        assertThat(livenessProbe.getInitialDelaySeconds()).isEqualTo(180);
        assertThat(livenessProbe.getTimeoutSeconds()).isEqualTo(2);
        assertThat(livenessProbe.getPeriodSeconds()).isEqualTo(15);
        assertThat(livenessProbe.getFailureThreshold()).isEqualTo(3);
    }

    @Test
    public void testBuildKieServerDeploymentReadinessProbe() {
        Template kieServerTemplate = new TemplateLoader(openShiftClient).loadKieServerTemplate();

        KieServerDeploymentBuilder settingsBuilder = new KieServerDeploymentBuilder(kieServerTemplate);
        Deployment builtKieServerDeployment = settingsBuilder.withKieServerUser("kieServerName", "kieServerPassword").build();
        Probe readinessProbe = builtKieServerDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0).getReadinessProbe();

        assertThat(readinessProbe).isNotNull();
        assertThat(readinessProbe.getExec().getCommand()).containsExactly("/bin/bash", "-c", "curl --fail --silent -u 'kieServerName:kieServerPassword' http://localhost:8080/services/rest/server/readycheck");
        assertThat(readinessProbe.getInitialDelaySeconds()).isEqualTo(60);
        assertThat(readinessProbe.getTimeoutSeconds()).isEqualTo(2);
        assertThat(readinessProbe.getPeriodSeconds()).isEqualTo(30);
        assertThat(readinessProbe.getFailureThreshold()).isEqualTo(6);
    }

    @Test
    public void testBuildKieServerDeploymentWithKieServerUser() {
        Template kieServerTemplate = new TemplateLoader(openShiftClient).loadKieServerTemplate();

        KieServerDeploymentBuilder settingsBuilder = new KieServerDeploymentBuilder(kieServerTemplate);
        Deployment builtKieServerDeployment = settingsBuilder.withKieServerUser("kieServerName", "kieServerPassword")
                                                             .build();

        assertThat(builtKieServerDeployment).isNotNull();
        assertThat(builtKieServerDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> OpenShiftImageConstants.KIE_SERVER_USER.equals(e.getName()))
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo("kieServerName"));
        assertThat(builtKieServerDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
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
        assertThat(builtKieServerDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> OpenShiftImageConstants.DATASOURCES.equals(e.getName()))
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo(OpenShiftImageConstants.DATASOURCES_KIE));
        assertThat(builtKieServerDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> OpenShiftImageConstants.KIE_DATABASE.equals(e.getName()))
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo("custom-db"));
        assertThat(builtKieServerDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> OpenShiftImageConstants.KIE_JNDI.equals(e.getName()))
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo("java:/jboss/datasources/kie"));
        assertThat(builtKieServerDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> OpenShiftImageConstants.KIE_DRIVER.equals(e.getName()))
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo("mysql"));
        assertThat(builtKieServerDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> OpenShiftImageConstants.KIE_JTA.equals(e.getName()))
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo("true"));
        assertThat(builtKieServerDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> OpenShiftImageConstants.KIE_USERNAME.equals(e.getName()))
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo("mySqlName"));
        assertThat(builtKieServerDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> OpenShiftImageConstants.KIE_PASSWORD.equals(e.getName()))
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo("mySqlPassword"));
        assertThat(builtKieServerDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> OpenShiftImageConstants.KIE_SERVICE_HOST.equals(e.getName()))
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo("${APPLICATION_NAME}-mysql"));
        assertThat(builtKieServerDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> OpenShiftImageConstants.KIE_SERVICE_PORT.equals(e.getName()))
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo("3306"));
        assertThat(builtKieServerDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> OpenShiftImageConstants.TIMER_SERVICE_DATA_STORE.equals(e.getName()))
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo("${APPLICATION_NAME}-mysql"));
        assertThat(builtKieServerDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> OpenShiftImageConstants.TIMER_SERVICE_DATA_STORE_REFRESH_INTERVAL.equals(e.getName()))
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo("30000"));
        assertThat(builtKieServerDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> OpenShiftImageConstants.KIE_SERVER_PERSISTENCE_DIALECT.equals(e.getName()))
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo("org.hibernate.dialect.MySQL5Dialect"));
        assertThat(builtKieServerDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
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
        assertThat(builtKieServerDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> OpenShiftImageConstants.DATASOURCES.equals(e.getName()))
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo(OpenShiftImageConstants.DATASOURCES_KIE));
        assertThat(builtKieServerDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> OpenShiftImageConstants.KIE_DATABASE.equals(e.getName()))
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo("custom-db"));
        assertThat(builtKieServerDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> OpenShiftImageConstants.KIE_JNDI.equals(e.getName()))
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo("java:/jboss/datasources/kie"));
        assertThat(builtKieServerDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> OpenShiftImageConstants.KIE_DRIVER.equals(e.getName()))
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo("postgresql"));
        assertThat(builtKieServerDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> OpenShiftImageConstants.KIE_JTA.equals(e.getName()))
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo("true"));
        assertThat(builtKieServerDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> OpenShiftImageConstants.KIE_USERNAME.equals(e.getName()))
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo("postgreSqlName"));
        assertThat(builtKieServerDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> OpenShiftImageConstants.KIE_PASSWORD.equals(e.getName()))
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo("postgreSqlPassword"));
        assertThat(builtKieServerDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> OpenShiftImageConstants.KIE_SERVICE_HOST.equals(e.getName()))
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo("${APPLICATION_NAME}-postgresql"));
        assertThat(builtKieServerDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> OpenShiftImageConstants.KIE_SERVICE_PORT.equals(e.getName()))
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo("5432"));
        assertThat(builtKieServerDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> OpenShiftImageConstants.TIMER_SERVICE_DATA_STORE.equals(e.getName()))
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo("${APPLICATION_NAME}-postgresql"));
        assertThat(builtKieServerDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> OpenShiftImageConstants.TIMER_SERVICE_DATA_STORE_REFRESH_INTERVAL.equals(e.getName()))
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo("30000"));
        assertThat(builtKieServerDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> OpenShiftImageConstants.KIE_SERVER_PERSISTENCE_DIALECT.equals(e.getName()))
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo("org.hibernate.dialect.PostgreSQLDialect"));
        assertThat(builtKieServerDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> OpenShiftImageConstants.KIE_SERVER_PERSISTENCE_DS.equals(e.getName()))
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo("java:/jboss/datasources/kie"));
    }
}
