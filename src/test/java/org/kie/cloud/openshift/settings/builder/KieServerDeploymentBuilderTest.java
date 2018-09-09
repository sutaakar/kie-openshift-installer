package org.kie.cloud.openshift.settings.builder;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import io.fabric8.kubernetes.api.model.Probe;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeMount;
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
        assertThat(builtKieServerDeployment.getTemplate().getMetadata().getName()).contains("-kieserver");
    }

    @Test
    public void testBuildKieServerDeploymentCustomDeploymentName() {
        Template kieServerTemplate = new TemplateLoader(openShiftClient).loadKieServerTemplate();

        KieServerDeploymentBuilder settingsBuilder = new KieServerDeploymentBuilder(kieServerTemplate, "custom-server");
        Deployment builtKieServerDeployment = settingsBuilder.build();

        assertThat(builtKieServerDeployment).isNotNull();
        assertThat(builtKieServerDeployment.getDeploymentConfig().getMetadata().getName()).isEqualTo("custom-server");
    }

    @Test
    public void testBuildKieServerDeploymentDeploymentConfig() {
        Template kieServerTemplate = new TemplateLoader(openShiftClient).loadKieServerTemplate();

        KieServerDeploymentBuilder settingsBuilder = new KieServerDeploymentBuilder(kieServerTemplate);
        Deployment builtKieServerDeployment = settingsBuilder.build();

        assertThat(builtKieServerDeployment).isNotNull();
        assertThat(builtKieServerDeployment.getDeploymentConfig().getApiVersion()).isEqualTo("v1");
        assertThat(builtKieServerDeployment.getDeploymentConfig().getMetadata().getName()).isEqualTo(builtKieServerDeployment.getDeploymentName());
        assertThat(builtKieServerDeployment.getDeploymentConfig().getSpec().getStrategy().getType()).isEqualTo("Recreate");
        assertThat(builtKieServerDeployment.getDeploymentConfig().getSpec().getTriggers()).hasSize(2);
        assertThat(builtKieServerDeployment.getDeploymentConfig().getSpec().getTriggers())
                    .filteredOn(t -> t.getType().equals("ConfigChange"))
                    .hasSize(1);
        assertThat(builtKieServerDeployment.getDeploymentConfig().getSpec().getTriggers())
                    .filteredOn(t -> t.getType().equals("ImageChange"))
                    .hasOnlyOneElementSatisfying(e -> {
                        assertThat(e.getImageChangeParams().getAutomatic()).isTrue();
                        assertThat(e.getImageChangeParams().getContainerNames()).containsExactly(builtKieServerDeployment.getDeploymentName());
                        assertThat(e.getImageChangeParams().getFrom().getKind()).isEqualTo("ImageStreamTag");
                        assertThat(e.getImageChangeParams().getFrom().getNamespace()).isEqualTo("${IMAGE_STREAM_NAMESPACE}");
                        // TODO: This config needs to be compared with template compatibility test
                        // assertThat(e.getImageChangeParams().getFrom().getName()).isEqualTo("rhpam71-kieserver-openshift:1.0");
                    });
        assertThat(builtKieServerDeployment.getDeploymentConfig().getSpec().getReplicas()).isEqualTo(1);
        assertThat(builtKieServerDeployment.getDeploymentConfig().getSpec().getSelector()).containsEntry("deploymentConfig", builtKieServerDeployment.getDeploymentName());
        assertThat(builtKieServerDeployment.getDeploymentConfig().getSpec().getTemplate().getMetadata().getName()).isEqualTo(builtKieServerDeployment.getDeploymentName());
        assertThat(builtKieServerDeployment.getDeploymentConfig().getSpec().getTemplate().getMetadata().getLabels()).containsEntry("deploymentConfig", builtKieServerDeployment.getDeploymentName());
        assertThat(builtKieServerDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getTerminationGracePeriodSeconds()).isEqualTo(60L);
        assertThat(builtKieServerDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers())
                    .hasOnlyOneElementSatisfying(c -> {
                        assertThat(c.getName()).isEqualTo(builtKieServerDeployment.getDeploymentName());
                        // TODO: This config needs to be compared with template compatibility test
                        // assertThat(c.getImage()).isEqualTo("rhpam70-kieserver-openshift");
                        assertThat(c.getImagePullPolicy()).isEqualTo("Always");
                        assertThat(c.getResources().getLimits()).containsEntry("memory", new Quantity("1Gi"));
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
        assertThat(builtKieServerDeployment.getServices().get(0).getSpec().getSelector()).containsEntry("deploymentConfig", builtKieServerDeployment.getDeploymentName());
        assertThat(builtKieServerDeployment.getServices().get(0).getMetadata().getName()).isEqualTo(builtKieServerDeployment.getDeploymentName());
        assertThat(builtKieServerDeployment.getServices().get(0).getMetadata().getAnnotations()).containsEntry("description", "All the KIE server web server's ports.");
        assertThat(builtKieServerDeployment.getServices().get(0).getMetadata().getLabels()).containsEntry("service", builtKieServerDeployment.getDeploymentName());
    }

    @Test
    public void testBuildKieServerDeploymentRoute() {
        Template kieServerTemplate = new TemplateLoader(openShiftClient).loadKieServerTemplate();

        KieServerDeploymentBuilder settingsBuilder = new KieServerDeploymentBuilder(kieServerTemplate);
        Deployment builtKieServerDeployment = settingsBuilder.build();

        assertThat(builtKieServerDeployment).isNotNull();
        assertThat(builtKieServerDeployment.getUnsecureRoutes()).hasSize(1);
        assertThat(builtKieServerDeployment.getUnsecureRoutes().get(0).getApiVersion()).isEqualTo("v1");
        assertThat(builtKieServerDeployment.getUnsecureRoutes().get(0).getMetadata().getName()).isEqualTo(builtKieServerDeployment.getDeploymentName());
        assertThat(builtKieServerDeployment.getUnsecureRoutes().get(0).getSpec().getTo().getName()).isEqualTo(builtKieServerDeployment.getServices().get(0).getMetadata().getName());
        assertThat(builtKieServerDeployment.getUnsecureRoutes().get(0).getSpec().getPort().getTargetPort().getStrVal()).isEqualTo("http");
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
    public void testBuildKieServerDeploymentWithHttps() {
        Template kieServerTemplate = new TemplateLoader(openShiftClient).loadKieServerTemplate();

        KieServerDeploymentBuilder settingsBuilder = new KieServerDeploymentBuilder(kieServerTemplate);
        Deployment builtKieServerDeployment = settingsBuilder.withHttps("custom-secret", "custom-keystore", "custom-name", "custom-password")
                                                             .build();

        assertThat(builtKieServerDeployment).isNotNull();

        // Check route
        assertThat(builtKieServerDeployment.getSecureRoutes()).hasSize(1);
        assertThat(builtKieServerDeployment.getSecureRoutes().get(0).getApiVersion()).isEqualTo("v1");
        assertThat(builtKieServerDeployment.getSecureRoutes().get(0).getMetadata().getName()).isEqualTo("secure-" + builtKieServerDeployment.getDeploymentName());
        assertThat(builtKieServerDeployment.getSecureRoutes().get(0).getSpec().getTo().getName()).isEqualTo(builtKieServerDeployment.getServices().get(0).getMetadata().getName());
        assertThat(builtKieServerDeployment.getSecureRoutes().get(0).getSpec().getPort().getTargetPort().getStrVal()).isEqualTo("https");
        assertThat(builtKieServerDeployment.getSecureRoutes().get(0).getSpec().getTls().getTermination()).isEqualTo("passthrough");

        // Check service
        assertThat(builtKieServerDeployment.getServices()).hasSize(1);
        assertThat(builtKieServerDeployment.getServices().get(0).getSpec().getPorts()).hasSize(2);
        assertThat(builtKieServerDeployment.getServices().get(0).getSpec().getPorts().get(0).getName()).isEqualTo("http");
        assertThat(builtKieServerDeployment.getServices().get(0).getSpec().getPorts().get(0).getPort()).isEqualTo(8080);
        assertThat(builtKieServerDeployment.getServices().get(0).getSpec().getPorts().get(0).getTargetPort().getIntVal()).isEqualTo(8080);
        assertThat(builtKieServerDeployment.getServices().get(0).getSpec().getPorts().get(1).getName()).isEqualTo("https");
        assertThat(builtKieServerDeployment.getServices().get(0).getSpec().getPorts().get(1).getPort()).isEqualTo(8443);
        assertThat(builtKieServerDeployment.getServices().get(0).getSpec().getPorts().get(1).getTargetPort().getIntVal()).isEqualTo(8443);

        // Check volume
        List<VolumeMount> volumeMounts = builtKieServerDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0).getVolumeMounts();
        List<Volume> volumes = builtKieServerDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getVolumes();
        assertThat(volumeMounts)
                        .hasOnlyOneElementSatisfying(m -> {
                            assertThat(m.getName()).isEqualTo("kieserver-keystore-volume");
                            assertThat(m.getMountPath()).isEqualTo("/etc/kieserver-secret-volume");
                            assertThat(m.getReadOnly()).isTrue();
                        });
        assertThat(volumes)
                        .hasOnlyOneElementSatisfying(v -> {
                            assertThat(v.getName()).isEqualTo(volumeMounts.get(0).getName());
                            assertThat(v.getSecret().getSecretName()).isEqualTo("custom-secret");
                        });

        // Check HTTPS ports
        assertThat(builtKieServerDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0).getPorts())
                        .anySatisfy(p -> {
                            assertThat(p.getName()).isEqualTo("https");
                            assertThat(p.getContainerPort()).isEqualTo(8443);
                            assertThat(p.getProtocol()).isEqualTo("TCP");
                        });

        // Check environment variables
        assertThat(builtKieServerDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> OpenShiftImageConstants.HTTPS_KEYSTORE_DIR.equals(e.getName()))
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo("/etc/kieserver-secret-volume"));
        assertThat(builtKieServerDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> OpenShiftImageConstants.HTTPS_KEYSTORE.equals(e.getName()))
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo("custom-keystore"));
        assertThat(builtKieServerDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> OpenShiftImageConstants.HTTPS_NAME.equals(e.getName()))
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo("custom-name"));
        assertThat(builtKieServerDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> OpenShiftImageConstants.HTTPS_PASSWORD.equals(e.getName()))
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo("custom-password"));
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
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo(builtMySqlDeployment.getDeploymentName()));
        assertThat(builtKieServerDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> OpenShiftImageConstants.KIE_SERVICE_PORT.equals(e.getName()))
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo("3306"));
        assertThat(builtKieServerDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> OpenShiftImageConstants.TIMER_SERVICE_DATA_STORE.equals(e.getName()))
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo(builtMySqlDeployment.getDeploymentName()));
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
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo(builtPostgreSqlDeployment.getDeploymentName()));
        assertThat(builtKieServerDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> OpenShiftImageConstants.KIE_SERVICE_PORT.equals(e.getName()))
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo("5432"));
        assertThat(builtKieServerDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> OpenShiftImageConstants.TIMER_SERVICE_DATA_STORE.equals(e.getName()))
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo(builtPostgreSqlDeployment.getDeploymentName()));
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
