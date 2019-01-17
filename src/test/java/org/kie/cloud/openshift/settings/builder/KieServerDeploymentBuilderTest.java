package org.kie.cloud.openshift.settings.builder;

import java.util.List;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.Probe;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.openshift.api.model.RoleBinding;
import org.junit.Test;
import org.kie.cloud.openshift.AbstractCloudTest;
import org.kie.cloud.openshift.OpenShiftImageConstants;
import org.kie.cloud.openshift.deployment.Deployment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class KieServerDeploymentBuilderTest extends AbstractCloudTest{

    @Test
    public void testBuildKieServerDeployment() {
        KieServerDeploymentBuilder settingsBuilder = new KieServerDeploymentBuilder();
        Deployment builtKieServerDeployment = settingsBuilder.build();

        assertThat(builtKieServerDeployment).isNotNull();
        assertThat(builtKieServerDeployment.getObjects()).size().isGreaterThan(0);
    }

    @Test
    public void testBuildKieServerDeploymentCustomDeploymentName() {
        KieServerDeploymentBuilder settingsBuilder = new KieServerDeploymentBuilder("custom-server");
        Deployment builtKieServerDeployment = settingsBuilder.build();

        assertThat(builtKieServerDeployment).isNotNull();
        assertThat(builtKieServerDeployment.getDeploymentConfig().getMetadata().getName()).isEqualTo("custom-server");
    }

    @Test
    public void testBuildKieServerDeploymentDeploymentConfig() {
        KieServerDeploymentBuilder settingsBuilder = new KieServerDeploymentBuilder();
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
                        assertThat(e.getImageChangeParams().getFrom().getNamespace()).isEqualTo("openshift");
                        // TODO: This config needs to be compared with template compatibility test
                        // assertThat(e.getImageChangeParams().getFrom().getName()).isEqualTo("rhpam71-kieserver-openshift:1.0");
                    });
        assertThat(builtKieServerDeployment.getDeploymentConfig().getSpec().getReplicas()).isEqualTo(1);
        assertThat(builtKieServerDeployment.getDeploymentConfig().getSpec().getSelector()).containsEntry("deploymentConfig", builtKieServerDeployment.getDeploymentName());
        assertThat(builtKieServerDeployment.getDeploymentConfig().getSpec().getTemplate().getMetadata().getName()).isEqualTo(builtKieServerDeployment.getDeploymentName());
        assertThat(builtKieServerDeployment.getDeploymentConfig().getSpec().getTemplate().getMetadata().getLabels()).containsEntry("deploymentConfig", builtKieServerDeployment.getDeploymentName());
        assertThat(builtKieServerDeployment.getDeploymentConfig().getSpec().getTemplate().getMetadata().getLabels()).containsEntry("service", builtKieServerDeployment.getDeploymentName());
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
        assertThat(builtKieServerDeployment.getDeploymentConfig().getMetadata().getAnnotations()).containsEntry("template.alpha.openshift.io/wait-for-ready", "true");
        assertThat(builtKieServerDeployment.getDeploymentConfig().getMetadata().getLabels()).containsEntry("service", builtKieServerDeployment.getDeploymentName());
        assertThat(builtKieServerDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> OpenShiftImageConstants.KIE_SERVER_ROUTE_NAME.equals(e.getName()))
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo(builtKieServerDeployment.getDeploymentName()));
    }

    @Test
    public void testBuildKieServerDeploymentService() {
        KieServerDeploymentBuilder settingsBuilder = new KieServerDeploymentBuilder();
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
        KieServerDeploymentBuilder settingsBuilder = new KieServerDeploymentBuilder();
        Deployment builtKieServerDeployment = settingsBuilder.build();

        assertThat(builtKieServerDeployment).isNotNull();
        assertThat(builtKieServerDeployment.getUnsecureRoutes()).hasSize(1);
        assertThat(builtKieServerDeployment.getUnsecureRoutes().get(0).getApiVersion()).isEqualTo("v1");
        assertThat(builtKieServerDeployment.getUnsecureRoutes().get(0).getMetadata().getName()).isEqualTo(builtKieServerDeployment.getDeploymentName());
        assertThat(builtKieServerDeployment.getUnsecureRoutes().get(0).getSpec().getTo().getName()).isEqualTo(builtKieServerDeployment.getServices().get(0).getMetadata().getName());
        assertThat(builtKieServerDeployment.getUnsecureRoutes().get(0).getSpec().getPort().getTargetPort().getStrVal()).isEqualTo("http");
        assertThat(builtKieServerDeployment.getUnsecureRoutes().get(0).getMetadata().getAnnotations()).containsEntry("description", "Route for KIE server's http service.");
        assertThat(builtKieServerDeployment.getUnsecureRoutes().get(0).getMetadata().getLabels()).containsEntry("service", builtKieServerDeployment.getDeploymentName());
        assertThat(builtKieServerDeployment.getUnsecureRoutes().get(0).getAdditionalProperties()).containsEntry("id", builtKieServerDeployment.getDeploymentName() + "-http");
    }

    @Test
    public void testBuildKieServerDeploymentServiceAccount() {
        KieServerDeploymentBuilder settingsBuilder = new KieServerDeploymentBuilder();
        Deployment builtKieServerDeployment = settingsBuilder.build();
        assertThat(builtKieServerDeployment).isNotNull();

        ServiceAccount serviceAccount = builtKieServerDeployment.getObjects().stream()
                                                                .filter(o -> o instanceof ServiceAccount)
                                                                .map(o -> (ServiceAccount) o)
                                                                .findAny()
                                                                .get();
        RoleBinding roleBinding = builtKieServerDeployment.getObjects().stream()
                                                          .filter(o -> o instanceof RoleBinding)
                                                          .map(o -> (RoleBinding) o)
                                                          .findAny()
                                                          .get();

        assertThat(serviceAccount.getApiVersion()).isEqualTo("v1");
        assertThat(serviceAccount.getMetadata().getName()).isEqualTo(builtKieServerDeployment.getDeploymentName());
        assertThat(roleBinding.getApiVersion()).isEqualTo("v1");
        assertThat(roleBinding.getMetadata().getName()).isEqualTo(builtKieServerDeployment.getDeploymentName() + "-view");
        assertThat(roleBinding.getSubjects()).hasOnlyOneElementSatisfying(s -> {
            assertThat(s.getKind()).isEqualTo("ServiceAccount");
            assertThat(s.getName()).isEqualTo(serviceAccount.getMetadata().getName());
        });
        assertThat(roleBinding.getRoleRef().getName()).isEqualTo("view");
        assertThat(builtKieServerDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getServiceAccountName()).isEqualTo(serviceAccount.getMetadata().getName());
    }

    @Test
    public void testBuildKieServerDeploymentDefaultValues() {
        KieServerDeploymentBuilder settingsBuilder = new KieServerDeploymentBuilder();
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
        KieServerDeploymentBuilder settingsBuilder = new KieServerDeploymentBuilder();
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
        KieServerDeploymentBuilder settingsBuilder = new KieServerDeploymentBuilder();
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
    public void testBuildKieServerDeploymentWithKieServerId() {
        KieServerDeploymentBuilder settingsBuilder = new KieServerDeploymentBuilder();
        Deployment builtKieServerDeployment = settingsBuilder.withKieServerId("custom-id")
                                                             .build();

        assertThat(builtKieServerDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> OpenShiftImageConstants.KIE_SERVER_ID.equals(e.getName()))
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo("custom-id"));
    }

    @Test
    public void testBuildKieServerDeploymentWithKieServerUserFromProperties() {
        KieServerDeploymentBuilder settingsBuilder = new KieServerDeploymentBuilder();
        Deployment builtKieServerDeployment = settingsBuilder.withKieServerUserFromProperties()
                                                             .build();

        assertThat(builtKieServerDeployment).isNotNull();
        assertThat(builtKieServerDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> OpenShiftImageConstants.KIE_SERVER_USER.equals(e.getName()))
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo("${KIE_SERVER_USER}"));
        assertThat(builtKieServerDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> OpenShiftImageConstants.KIE_SERVER_PWD.equals(e.getName()))
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo("${KIE_SERVER_PWD}"));
        assertThat(builtKieServerDeployment.getParameters())
                        .filteredOn(p -> OpenShiftImageConstants.KIE_SERVER_USER.equals(p.getName()))
                        .hasOnlyOneElementSatisfying(p -> {
                            assertThat(p.getDisplayName()).isEqualTo("KIE Server User");
                            assertThat(p.getValue()).isEqualTo("executionUser");
                            assertThat(p.getRequired()).isEqualTo(Boolean.FALSE);
                        });
        assertThat(builtKieServerDeployment.getParameters())
                        .filteredOn(p -> OpenShiftImageConstants.KIE_SERVER_PWD.equals(p.getName()))
                        .hasOnlyOneElementSatisfying(p -> {
                            assertThat(p.getDisplayName()).isEqualTo("KIE Server Password");
                            assertThat(p.getGenerate()).isEqualTo("expression");
                            assertThat(p.getFrom()).isEqualTo("[a-zA-Z]{6}[0-9]{1}!");
                            assertThat(p.getRequired()).isEqualTo(Boolean.FALSE);
                        });
    }

    @Test
    public void testBuildKieServerDeploymentWithKieServerUser() {
        KieServerDeploymentBuilder settingsBuilder = new KieServerDeploymentBuilder();
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
    public void testBuildKieServerDeploymentWithKieServerControllerConnectionFromProperties() {
        KieServerDeploymentBuilder settingsBuilder = new KieServerDeploymentBuilder();
        Deployment builtKieServerDeployment = settingsBuilder.withKieServerControllerConnectionFromProperties()
                                                             .build();

        assertThat(builtKieServerDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> OpenShiftImageConstants.KIE_SERVER_CONTROLLER_USER.equals(e.getName()))
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo("${KIE_SERVER_CONTROLLER_USER}"));
        assertThat(builtKieServerDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> OpenShiftImageConstants.KIE_SERVER_CONTROLLER_PWD.equals(e.getName()))
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo("${KIE_SERVER_CONTROLLER_PWD}"));
        assertThat(builtKieServerDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> OpenShiftImageConstants.KIE_SERVER_CONTROLLER_TOKEN.equals(e.getName()))
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo("${KIE_SERVER_CONTROLLER_TOKEN}"));
        assertThat(builtKieServerDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> OpenShiftImageConstants.KIE_SERVER_CONTROLLER_SERVICE.equals(e.getName()))
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo("${KIE_SERVER_CONTROLLER_SERVICE}"));
        assertThat(builtKieServerDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> OpenShiftImageConstants.KIE_SERVER_CONTROLLER_PROTOCOL.equals(e.getName()))
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo("ws"));
        assertThat(builtKieServerDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> OpenShiftImageConstants.KIE_SERVER_CONTROLLER_HOST.equals(e.getName()))
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo("${KIE_SERVER_CONTROLLER_HOST}"));
        assertThat(builtKieServerDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> OpenShiftImageConstants.KIE_SERVER_CONTROLLER_PORT.equals(e.getName()))
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo("${KIE_SERVER_CONTROLLER_PORT}"));
        assertThat(builtKieServerDeployment.getParameters())
                        .filteredOn(p -> OpenShiftImageConstants.KIE_SERVER_CONTROLLER_USER.equals(p.getName()))
                        .hasOnlyOneElementSatisfying(p -> {
                            assertThat(p.getDisplayName()).isEqualTo("KIE Server Controller User");
                            assertThat(p.getValue()).isEqualTo("controllerUser");
                            assertThat(p.getRequired()).isEqualTo(Boolean.FALSE);
                        });
        assertThat(builtKieServerDeployment.getParameters())
                        .filteredOn(p -> OpenShiftImageConstants.KIE_SERVER_CONTROLLER_PWD.equals(p.getName()))
                        .hasOnlyOneElementSatisfying(p -> {
                            assertThat(p.getDisplayName()).isEqualTo("KIE Server Controller Password");
                            // TODO: Bug?
                            assertThat(p.getRequired()).isEqualTo(Boolean.FALSE);
                        });
        assertThat(builtKieServerDeployment.getParameters())
                        .filteredOn(p -> OpenShiftImageConstants.KIE_SERVER_CONTROLLER_TOKEN.equals(p.getName()))
                        .hasOnlyOneElementSatisfying(p -> {
                            assertThat(p.getDisplayName()).isEqualTo("KIE Server Controller Token");
                            assertThat(p.getRequired()).isEqualTo(Boolean.FALSE);
                        });
        assertThat(builtKieServerDeployment.getParameters())
                        .filteredOn(p -> OpenShiftImageConstants.KIE_SERVER_CONTROLLER_SERVICE.equals(p.getName()))
                        .hasOnlyOneElementSatisfying(p -> {
                            assertThat(p.getDisplayName()).isEqualTo("KIE Server Controller Service");
                            assertThat(p.getRequired()).isEqualTo(Boolean.FALSE);
                        });
        assertThat(builtKieServerDeployment.getParameters())
                        .filteredOn(p -> OpenShiftImageConstants.KIE_SERVER_CONTROLLER_HOST.equals(p.getName()))
                        .hasOnlyOneElementSatisfying(p -> {
                            assertThat(p.getDisplayName()).isEqualTo("KIE Server Controller host");
                            assertThat(p.getAdditionalProperties()).containsEntry("example", "my-app-controller-ocpuser.os.example.com");
                            assertThat(p.getRequired()).isEqualTo(Boolean.FALSE);
                        });
        assertThat(builtKieServerDeployment.getParameters())
                        .filteredOn(p -> OpenShiftImageConstants.KIE_SERVER_CONTROLLER_PORT.equals(p.getName()))
                        .hasOnlyOneElementSatisfying(p -> {
                            assertThat(p.getDisplayName()).isEqualTo("KIE Server Controller port");
                            assertThat(p.getAdditionalProperties()).containsEntry("example", "8080");
                            assertThat(p.getRequired()).isEqualTo(Boolean.FALSE);
                        });
    }

    @Test
    public void testBuildKieServerDeploymentWithHttpsFromProperties() {
        KieServerDeploymentBuilder settingsBuilder = new KieServerDeploymentBuilder();
        Deployment builtKieServerDeployment = settingsBuilder.withHttpsFromProperties()
                                                             .build();

        assertThat(builtKieServerDeployment).isNotNull();

        // Check route
        assertThat(builtKieServerDeployment.getSecureRoutes()).hasSize(1);
        assertThat(builtKieServerDeployment.getSecureRoutes().get(0).getApiVersion()).isEqualTo("v1");
        assertThat(builtKieServerDeployment.getSecureRoutes().get(0).getMetadata().getName()).isEqualTo("secure-" + builtKieServerDeployment.getDeploymentName());
        assertThat(builtKieServerDeployment.getSecureRoutes().get(0).getSpec().getTo().getName()).isEqualTo(builtKieServerDeployment.getServices().get(0).getMetadata().getName());
        assertThat(builtKieServerDeployment.getSecureRoutes().get(0).getSpec().getPort().getTargetPort().getStrVal()).isEqualTo("https");
        assertThat(builtKieServerDeployment.getSecureRoutes().get(0).getSpec().getTls().getTermination()).isEqualTo("passthrough");
        assertThat(builtKieServerDeployment.getSecureRoutes().get(0).getMetadata().getAnnotations()).containsEntry("description", "Route for KIE server's https service.");
        assertThat(builtKieServerDeployment.getSecureRoutes().get(0).getMetadata().getLabels()).containsEntry("service", builtKieServerDeployment.getDeploymentName());
        assertThat(builtKieServerDeployment.getSecureRoutes().get(0).getAdditionalProperties()).containsEntry("id", builtKieServerDeployment.getDeploymentName() + "-https");

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
                            assertThat(v.getSecret().getSecretName()).isEqualTo("${KIE_SERVER_HTTPS_SECRET}");
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
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo("${KIE_SERVER_HTTPS_KEYSTORE}"));
        assertThat(builtKieServerDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> OpenShiftImageConstants.HTTPS_NAME.equals(e.getName()))
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo("${KIE_SERVER_HTTPS_NAME}"));
        assertThat(builtKieServerDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> OpenShiftImageConstants.HTTPS_PASSWORD.equals(e.getName()))
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo("${KIE_SERVER_HTTPS_PASSWORD}"));
        assertThat(builtKieServerDeployment.getParameters())
                        .filteredOn(p -> OpenShiftImageConstants.KIE_SERVER_HTTPS_SECRET.equals(p.getName()))
                        .hasOnlyOneElementSatisfying(p -> {
                            assertThat(p.getDisplayName()).isEqualTo("KIE Server Keystore Secret Name");
                            assertThat(p.getAdditionalProperties()).containsEntry("example", "kieserver-app-secret");
                            assertThat(p.getRequired()).isEqualTo(Boolean.TRUE);
                        });
        assertThat(builtKieServerDeployment.getParameters())
                        .filteredOn(p -> OpenShiftImageConstants.KIE_SERVER_HTTPS_KEYSTORE.equals(p.getName()))
                        .hasOnlyOneElementSatisfying(p -> {
                            assertThat(p.getDisplayName()).isEqualTo("KIE Server Keystore Filename");
                            assertThat(p.getValue()).isEqualTo("keystore.jks");
                            assertThat(p.getRequired()).isEqualTo(Boolean.FALSE);
                        });
        assertThat(builtKieServerDeployment.getParameters())
                        .filteredOn(p -> OpenShiftImageConstants.KIE_SERVER_HTTPS_NAME.equals(p.getName()))
                        .hasOnlyOneElementSatisfying(p -> {
                            assertThat(p.getDisplayName()).isEqualTo("KIE Server Certificate Name");
                            assertThat(p.getValue()).isEqualTo("jboss");
                            assertThat(p.getRequired()).isEqualTo(Boolean.FALSE);
                        });
        assertThat(builtKieServerDeployment.getParameters())
                        .filteredOn(p -> OpenShiftImageConstants.KIE_SERVER_HTTPS_PASSWORD.equals(p.getName()))
                        .hasOnlyOneElementSatisfying(p -> {
                            assertThat(p.getDisplayName()).isEqualTo("KIE Server Keystore Password");
                            assertThat(p.getValue()).isEqualTo("mykeystorepass");
                            assertThat(p.getRequired()).isEqualTo(Boolean.FALSE);
                        });
    }

    @Test
    public void testBuildKieServerDeploymentWithHttps() {
        KieServerDeploymentBuilder settingsBuilder = new KieServerDeploymentBuilder();
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
        assertThat(builtKieServerDeployment.getSecureRoutes().get(0).getMetadata().getAnnotations()).containsEntry("description", "Route for KIE server's https service.");
        assertThat(builtKieServerDeployment.getSecureRoutes().get(0).getMetadata().getLabels()).containsEntry("service", builtKieServerDeployment.getDeploymentName());
        assertThat(builtKieServerDeployment.getSecureRoutes().get(0).getAdditionalProperties()).containsEntry("id", builtKieServerDeployment.getDeploymentName() + "-https");

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
    public void testBuildKieServerDeploymentWithImageStreamNamespaceFromProperties() {
        KieServerDeploymentBuilder settingsBuilder = new KieServerDeploymentBuilder();
        Deployment builtKieServerDeployment = settingsBuilder.withImageStreamNamespaceFromProperties()
                                                             .build();

        assertThat(builtKieServerDeployment.getDeploymentConfig().getSpec().getTriggers())
                    .filteredOn(t -> t.getType().equals("ImageChange"))
                    .hasOnlyOneElementSatisfying(e -> {
                        assertThat(e.getImageChangeParams().getFrom().getNamespace()).isEqualTo("${IMAGE_STREAM_NAMESPACE}");
                    });
        assertThat(builtKieServerDeployment.getParameters())
                    .filteredOn(p -> OpenShiftImageConstants.IMAGE_STREAM_NAMESPACE.equals(p.getName()))
                    .hasOnlyOneElementSatisfying(p -> {
                        assertThat(p.getDisplayName()).isEqualTo("ImageStream Namespace");
                        assertThat(p.getValue()).isEqualTo("openshift");
                        assertThat(p.getRequired()).isEqualTo(Boolean.TRUE);
                    });
    }

    @Test
    public void testBuildKieServerDeploymentWithCustomImageStreamNamespace() {
        KieServerDeploymentBuilder settingsBuilder = new KieServerDeploymentBuilder();
        Deployment builtKieServerDeployment = settingsBuilder.withImageStreamNamespace("custom-namespace")
                                                             .build();

        assertThat(builtKieServerDeployment).isNotNull();
        assertThat(builtKieServerDeployment.getDeploymentConfig().getSpec().getTriggers())
                    .filteredOn(t -> t.getType().equals("ImageChange"))
                    .hasOnlyOneElementSatisfying(e -> {
                        assertThat(e.getImageChangeParams().getFrom().getNamespace()).isEqualTo("custom-namespace");
                    });
    }

    @Test
    public void testBuildKieServerDeploymentWithImageStreamNameFromProperties() {
        KieServerDeploymentBuilder settingsBuilder = new KieServerDeploymentBuilder();
        Deployment builtKieServerDeployment = settingsBuilder.withImageStreamNameFromProperties()
                                                             .build();

        assertThat(builtKieServerDeployment.getDeploymentConfig().getSpec().getTriggers())
                    .filteredOn(t -> t.getType().equals("ImageChange"))
                    .hasOnlyOneElementSatisfying(e -> {
                        assertThat(e.getImageChangeParams().getFrom().getName()).startsWith("${KIE_SERVER_IMAGE_STREAM_NAME}:");
                    });
        assertThat(builtKieServerDeployment.getParameters())
                    .filteredOn(p -> OpenShiftImageConstants.KIE_SERVER_IMAGE_STREAM_NAME.equals(p.getName()))
                    .hasOnlyOneElementSatisfying(p -> {
                        assertThat(p.getDisplayName()).isEqualTo("KIE Server ImageStream Name");
                        assertThat(p.getValue()).isEqualTo("rhpam72-kieserver-openshift");
                        assertThat(p.getRequired()).isEqualTo(Boolean.TRUE);
                    });
    }

    @Test
    public void testBuildKieServerDeploymentWithCustomImageStreamName() {
        KieServerDeploymentBuilder settingsBuilder = new KieServerDeploymentBuilder();
        Deployment builtKieServerDeployment = settingsBuilder.withImageStreamName("custom-name")
                                                             .build();

        assertThat(builtKieServerDeployment).isNotNull();
        assertThat(builtKieServerDeployment.getDeploymentConfig().getSpec().getTriggers())
                    .filteredOn(t -> t.getType().equals("ImageChange"))
                    .hasOnlyOneElementSatisfying(e -> {
                        assertThat(e.getImageChangeParams().getFrom().getName()).startsWith("custom-name:");
                    });
        assertThat(builtKieServerDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0).getImage()).isEqualTo("custom-name");
    }

    @Test
    public void testBuildKieServerDeploymentWithImageStreamTagFromProperties() {
        KieServerDeploymentBuilder settingsBuilder = new KieServerDeploymentBuilder();
        Deployment builtKieServerDeployment = settingsBuilder.withImageStreamTagFromProperties()
                                                             .build();

        assertThat(builtKieServerDeployment.getDeploymentConfig().getSpec().getTriggers())
                    .filteredOn(t -> t.getType().equals("ImageChange"))
                    .hasOnlyOneElementSatisfying(e -> {
                        assertThat(e.getImageChangeParams().getFrom().getName()).endsWith(":${IMAGE_STREAM_TAG}");
                    });
        assertThat(builtKieServerDeployment.getParameters())
                    .filteredOn(p -> OpenShiftImageConstants.IMAGE_STREAM_TAG.equals(p.getName()))
                    .hasOnlyOneElementSatisfying(p -> {
                        assertThat(p.getDisplayName()).isEqualTo("ImageStream Tag");
                        assertThat(p.getValue()).isEqualTo("1.0");
                        assertThat(p.getRequired()).isEqualTo(Boolean.TRUE);
                    });
    }

    @Test
    public void testBuildKieServerDeploymentWithCustomImageStreamTag() {
        KieServerDeploymentBuilder settingsBuilder = new KieServerDeploymentBuilder();
        Deployment builtKieServerDeployment = settingsBuilder.withImageStreamTag("123")
                                                             .build();

        assertThat(builtKieServerDeployment).isNotNull();
        assertThat(builtKieServerDeployment.getDeploymentConfig().getSpec().getTriggers())
                    .filteredOn(t -> t.getType().equals("ImageChange"))
                    .hasOnlyOneElementSatisfying(e -> {
                        assertThat(e.getImageChangeParams().getFrom().getName()).endsWith(":123");
                    });
    }

    @Test
    public void testBuildKieServerDeploymentWithHttpHostnameFromProperties() {
        KieServerDeploymentBuilder settingsBuilder = new KieServerDeploymentBuilder();
        Deployment builtKieServerDeployment = settingsBuilder.withHttpHostnameFromProperties()
                                                             .build();

        assertThat(builtKieServerDeployment).isNotNull();
        assertThat(builtKieServerDeployment.getUnsecureRoutes())
                    .filteredOn(r -> r.getSpec().getPort().getTargetPort().getStrVal().equals("http"))
                    .hasOnlyOneElementSatisfying(r -> {
                        assertThat(r.getSpec().getHost()).isEqualTo("${KIE_SERVER_HOSTNAME_HTTP}");
                    });
        assertThat(builtKieServerDeployment.getParameters())
                    .filteredOn(p -> OpenShiftImageConstants.KIE_SERVER_HOSTNAME_HTTP.equals(p.getName()))
                    .hasOnlyOneElementSatisfying(p -> {
                        assertThat(p.getDisplayName()).isEqualTo("KIE Server Custom http Route Hostname");
                        assertThat(p.getValue()).isEqualTo("");
                        assertThat(p.getRequired()).isEqualTo(Boolean.FALSE);
                    });
    }

    @Test
    public void testBuildKieServerDeploymentWithCustomHttpHostname() {
        KieServerDeploymentBuilder settingsBuilder = new KieServerDeploymentBuilder();
        Deployment builtKieServerDeployment = settingsBuilder.withHttpHostname("custom-hostname")
                                                             .build();

        assertThat(builtKieServerDeployment).isNotNull();
        assertThat(builtKieServerDeployment.getUnsecureRoutes())
                    .filteredOn(r -> r.getSpec().getPort().getTargetPort().getStrVal().equals("http"))
                    .hasOnlyOneElementSatisfying(r -> {
                        assertThat(r.getSpec().getHost()).isEqualTo("custom-hostname");
                    });
    }

    @Test
    public void testBuildKieServerDeploymentWithCustomHttpsHostnameWithoutConfiguredHttps() {
        KieServerDeploymentBuilder settingsBuilder = new KieServerDeploymentBuilder();

        assertThatThrownBy(() -> settingsBuilder.withHttpsHostname("custom-hostname").build()).hasMessage("Cannot set HTTPS hostname, HTTPS route not found.");
    }

    @Test
    public void testBuildKieServerDeploymentWithHttpsHostnameFromProperties() {
        KieServerDeploymentBuilder settingsBuilder = new KieServerDeploymentBuilder();
        Deployment builtKieServerDeployment = settingsBuilder.withHttpsFromProperties()
                                                             .withHttpsHostnameFromProperties()
                                                             .build();

        assertThat(builtKieServerDeployment).isNotNull();
        assertThat(builtKieServerDeployment.getSecureRoutes())
                    .filteredOn(r -> r.getSpec().getPort().getTargetPort().getStrVal().equals("https"))
                    .hasOnlyOneElementSatisfying(r -> {
                        assertThat(r.getSpec().getHost()).isEqualTo("${KIE_SERVER_HOSTNAME_HTTPS}");
                    });
        assertThat(builtKieServerDeployment.getParameters())
                    .filteredOn(p -> OpenShiftImageConstants.KIE_SERVER_HOSTNAME_HTTPS.equals(p.getName()))
                    .hasOnlyOneElementSatisfying(p -> {
                        assertThat(p.getDisplayName()).isEqualTo("KIE Server Custom https Route Hostname");
                        assertThat(p.getValue()).isEqualTo("");
                        assertThat(p.getRequired()).isEqualTo(Boolean.FALSE);
                    });
    }

    @Test
    public void testBuildKieServerDeploymentWithCustomHttpsHostname() {
        KieServerDeploymentBuilder settingsBuilder = new KieServerDeploymentBuilder();
        Deployment builtKieServerDeployment = settingsBuilder.withHttps("secret", "keystore", "name", "pwd")
                                                             .withHttpsHostname("custom-hostname")
                                                             .build();

        assertThat(builtKieServerDeployment).isNotNull();
        assertThat(builtKieServerDeployment.getSecureRoutes())
                    .filteredOn(r -> r.getSpec().getPort().getTargetPort().getStrVal().equals("https"))
                    .hasOnlyOneElementSatisfying(r -> {
                        assertThat(r.getSpec().getHost()).isEqualTo("custom-hostname");
                    });
    }

    @Test
    public void testBuildKieServerDeploymentWithMemoryLimitFromProperties() {
        KieServerDeploymentBuilder settingsBuilder = new KieServerDeploymentBuilder();
        Deployment builtKieServerDeployment = settingsBuilder.withContainerMemoryLimitFromProperties()
                                                             .build();

        assertThat(builtKieServerDeployment).isNotNull();
        assertThat(builtKieServerDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0).getResources().getLimits()).containsEntry("memory", new Quantity("${KIE_SERVER_MEMORY_LIMIT}"));
        assertThat(builtKieServerDeployment.getParameters())
        .filteredOn(p -> OpenShiftImageConstants.KIE_SERVER_MEMORY_LIMIT.equals(p.getName()))
        .hasOnlyOneElementSatisfying(p -> {
            assertThat(p.getDisplayName()).isEqualTo("KIE Server Container Memory Limit");
            assertThat(p.getValue()).isEqualTo("1Gi");
            assertThat(p.getRequired()).isEqualTo(Boolean.FALSE);
        });
    }

    @Test
    public void testBuildKieServerDeploymentWithCustomMemoryLimit() {
        KieServerDeploymentBuilder settingsBuilder = new KieServerDeploymentBuilder();
        Deployment builtKieServerDeployment = settingsBuilder.withContainerMemoryLimit("64Mi")
                                                             .build();

        assertThat(builtKieServerDeployment).isNotNull();
        assertThat(builtKieServerDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0).getResources().getLimits()).containsEntry("memory", new Quantity("64Mi"));
    }

    @Test
    public void testBuildKieServerDeploymentWithKieMbeansFromProperties() {
        KieServerDeploymentBuilder settingsBuilder = new KieServerDeploymentBuilder();
        Deployment builtKieServerDeployment = settingsBuilder.withKieMbeansFromProperties()
                                                             .build();

        assertThat(builtKieServerDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> OpenShiftImageConstants.KIE_MBEANS.equals(e.getName()))
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo("${KIE_MBEANS}"));
        assertThat(builtKieServerDeployment.getParameters())
                        .filteredOn(p -> OpenShiftImageConstants.KIE_MBEANS.equals(p.getName()))
                        .hasOnlyOneElementSatisfying(p -> {
                            assertThat(p.getDisplayName()).isEqualTo("KIE MBeans");
                            assertThat(p.getValue()).isEqualTo("enabled");
                            assertThat(p.getRequired()).isEqualTo(Boolean.FALSE);
                        });
    }

    @Test
    public void testBuildKieServerDeploymentWithKieMbeans() {
        KieServerDeploymentBuilder settingsBuilder = new KieServerDeploymentBuilder();
        Deployment builtKieServerDeployment = settingsBuilder.withKieMbeans("disabled")
                                                             .build();

        assertThat(builtKieServerDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> OpenShiftImageConstants.KIE_MBEANS.equals(e.getName()))
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo("disabled"));
    }

    @Test
    public void testBuildKieServerDeploymentWithKieServerBypassAuthUserFromProperties() {
        KieServerDeploymentBuilder settingsBuilder = new KieServerDeploymentBuilder();
        Deployment builtKieServerDeployment = settingsBuilder.withKieServerBypassAuthUserFromProperties()
                                                             .build();

        assertThat(builtKieServerDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> OpenShiftImageConstants.KIE_SERVER_BYPASS_AUTH_USER.equals(e.getName()))
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo("${KIE_SERVER_BYPASS_AUTH_USER}"));
        assertThat(builtKieServerDeployment.getParameters())
                        .filteredOn(p -> OpenShiftImageConstants.KIE_SERVER_BYPASS_AUTH_USER.equals(p.getName()))
                        .hasOnlyOneElementSatisfying(p -> {
                            assertThat(p.getDisplayName()).isEqualTo("KIE Server Bypass Auth User");
                            assertThat(p.getValue()).isEqualTo("false");
                            assertThat(p.getRequired()).isEqualTo(Boolean.FALSE);
                        });
    }

    @Test
    public void testBuildKieServerDeploymentWithKieServerBypassAuthUser() {
        KieServerDeploymentBuilder settingsBuilder = new KieServerDeploymentBuilder();
        Deployment builtKieServerDeployment = settingsBuilder.withKieServerBypassAuthUser(true)
                                                             .build();

        assertThat(builtKieServerDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> OpenShiftImageConstants.KIE_SERVER_BYPASS_AUTH_USER.equals(e.getName()))
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo("true"));
    }

    @Test
    public void testBuildKieServerDeploymentWithKieServerClassFilteringFromProperties() {
        KieServerDeploymentBuilder settingsBuilder = new KieServerDeploymentBuilder();
        Deployment builtKieServerDeployment = settingsBuilder.withKieServerClassFilteringFromProperties()
                                                             .build();

        assertThat(builtKieServerDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> OpenShiftImageConstants.DROOLS_SERVER_FILTER_CLASSES.equals(e.getName()))
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo("${DROOLS_SERVER_FILTER_CLASSES}"));
        assertThat(builtKieServerDeployment.getParameters())
                        .filteredOn(p -> OpenShiftImageConstants.DROOLS_SERVER_FILTER_CLASSES.equals(p.getName()))
                        .hasOnlyOneElementSatisfying(p -> {
                            assertThat(p.getDisplayName()).isEqualTo("Drools Server Filter Classes");
                            assertThat(p.getValue()).isEqualTo("true");
                            assertThat(p.getRequired()).isEqualTo(Boolean.FALSE);
                        });
    }

    @Test
    public void testBuildKieServerDeploymentWithKieServerClassFiltering() {
        KieServerDeploymentBuilder settingsBuilder = new KieServerDeploymentBuilder();
        Deployment builtKieServerDeployment = settingsBuilder.withKieServerClassFiltering(false)
                                                             .build();

        assertThat(builtKieServerDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> OpenShiftImageConstants.DROOLS_SERVER_FILTER_CLASSES.equals(e.getName()))
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo("false"));
    }

    @Test
    public void testBuildKieServerDeploymentWithClustering() {
        KieServerDeploymentBuilder settingsBuilder = new KieServerDeploymentBuilder();
        Deployment builtKieServerDeployment = settingsBuilder.withClustering()
                                                             .build();

        assertThat(builtKieServerDeployment).isNotNull();
        assertThat(builtKieServerDeployment.getServices())
                    .filteredOn(s -> s.getSpec().getClusterIP() != null && s.getSpec().getClusterIP().equals("None"))
                    .hasOnlyOneElementSatisfying(s -> {
                        assertThat(s.getApiVersion()).isEqualTo("v1");
                        assertThat(s.getSpec().getClusterIP()).isEqualTo("None");
                        assertThat(s.getSpec().getPorts()).hasOnlyOneElementSatisfying(p -> {
                            assertThat(p.getName()).isEqualTo("ping");
                            assertThat(p.getPort()).isEqualTo(8888);
                            assertThat(p.getTargetPort().getIntVal()).isEqualTo(8888);
                        });
                        assertThat(s.getSpec().getSelector()).containsEntry("deploymentConfig", builtKieServerDeployment.getDeploymentName());
                        assertThat(s.getMetadata().getName()).isEqualTo(builtKieServerDeployment.getDeploymentName() + "-ping");
                        assertThat(s.getMetadata().getLabels()).containsEntry("service", builtKieServerDeployment.getDeploymentName());
                        assertThat(s.getMetadata().getAnnotations()).containsEntry("service.alpha.kubernetes.io/tolerate-unready-endpoints", "true");
                        assertThat(s.getMetadata().getAnnotations()).containsEntry("description", "The JGroups ping port for clustering.");
                    });
        // Check ping port
        assertThat(builtKieServerDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0).getPorts())
                        .anySatisfy(p -> {
                            assertThat(p.getName()).isEqualTo("ping");
                            assertThat(p.getContainerPort()).isEqualTo(8888);
                            assertThat(p.getProtocol()).isEqualTo("TCP");
                        });
        assertThat(builtKieServerDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> OpenShiftImageConstants.JGROUPS_PING_PROTOCOL.equals(e.getName()))
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo("openshift.DNS_PING"));
        assertThat(builtKieServerDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> OpenShiftImageConstants.OPENSHIFT_DNS_PING_SERVICE_NAME.equals(e.getName()))
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo(builtKieServerDeployment.getDeploymentName() + "-ping"));
        assertThat(builtKieServerDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> OpenShiftImageConstants.OPENSHIFT_DNS_PING_SERVICE_PORT.equals(e.getName()))
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo("8888"));
    }

    @Test
    public void testBuildKieServerDeploymentWithMavenRepoService() {
        KieServerDeploymentBuilder settingsBuilder = new KieServerDeploymentBuilder();
        Deployment builtKieServerDeployment = settingsBuilder.withMavenRepo()
                                                                 .withService("maven-service", "/maven/path")
                                                             .endMavenRepo()
                                                             .build();

        EnvVar mavenRepoEnvVar = builtKieServerDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0).getEnv().stream()
                        .filter(e -> OpenShiftImageConstants.MAVEN_REPOS.equals(e.getName()))
                        .findAny()
                        .orElseThrow(() -> new RuntimeException("MAVEN_REPOS environment variable not found."));

        assertThat(builtKieServerDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> OpenShiftImageConstants.MAVEN_REPOS.equals(e.getName()))
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isNotEmpty());
        assertThat(builtKieServerDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> e.getName().equals(mavenRepoEnvVar.getValue() + "_" + OpenShiftImageConstants.MAVEN_REPO_SERVICE))
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo("maven-service"));
        assertThat(builtKieServerDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> e.getName().equals(mavenRepoEnvVar.getValue() + "_" + OpenShiftImageConstants.MAVEN_REPO_PATH))
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo("/maven/path"));
    }

    @Test
    public void testBuildKieServerDeploymentWithPrefixedMavenRepoUrl() {
        String mavenRepoUrl = "http://some-nexus.xyz:8081/nexus";

        KieServerDeploymentBuilder settingsBuilder = new KieServerDeploymentBuilder();
        Deployment builtKieServerDeployment = settingsBuilder.withMavenRepo("KIE")
                                                                 .withUrl(mavenRepoUrl)
                                                             .endMavenRepo()
                                                             .build();

        assertThat(builtKieServerDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> OpenShiftImageConstants.MAVEN_REPOS.equals(e.getName()))
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo("KIE"));
        assertThat(builtKieServerDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> e.getName().equals("KIE_" + OpenShiftImageConstants.MAVEN_REPO_URL))
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo(mavenRepoUrl));
    }

    @Test
    public void testBuildKieServerDeploymentWithPrefixedMavenRepoService() {
        KieServerDeploymentBuilder settingsBuilder = new KieServerDeploymentBuilder();
        Deployment builtKieServerDeployment = settingsBuilder.withMavenRepo("KIE")
                                                                 .withService("maven-service", "/maven/path")
                                                             .endMavenRepo()
                                                             .build();

        assertThat(builtKieServerDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> OpenShiftImageConstants.MAVEN_REPOS.equals(e.getName()))
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo("KIE"));
        assertThat(builtKieServerDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> e.getName().equals("KIE_" + OpenShiftImageConstants.MAVEN_REPO_SERVICE))
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo("maven-service"));
        assertThat(builtKieServerDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> e.getName().equals("KIE_" + OpenShiftImageConstants.MAVEN_REPO_PATH))
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo("/maven/path"));
    }

    @Test
    public void testBuildKieServerDeploymentWithPrefixedMavenRepoServiceWithId() {
        KieServerDeploymentBuilder settingsBuilder = new KieServerDeploymentBuilder();
        Deployment builtKieServerDeployment = settingsBuilder.withMavenRepo("KIE")
                                                                 .withId("repo-id")
                                                                 .withService("maven-service", "/maven/path")
                                                             .endMavenRepo()
                                                             .build();

        assertThat(builtKieServerDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> OpenShiftImageConstants.MAVEN_REPOS.equals(e.getName()))
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo("KIE"));
        assertThat(builtKieServerDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> e.getName().equals("KIE_" + OpenShiftImageConstants.MAVEN_REPO_ID))
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo("repo-id"));
        assertThat(builtKieServerDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> e.getName().equals("KIE_" + OpenShiftImageConstants.MAVEN_REPO_SERVICE))
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo("maven-service"));
        assertThat(builtKieServerDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> e.getName().equals("KIE_" + OpenShiftImageConstants.MAVEN_REPO_PATH))
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo("/maven/path"));
    }

    @Test
    public void testBuildKieServerDeploymentWithPrefixedMavenRepoServiceWithAuthentication() {
        KieServerDeploymentBuilder settingsBuilder = new KieServerDeploymentBuilder();
        Deployment builtKieServerDeployment = settingsBuilder.withMavenRepo("KIE")
                                                                 .withService("maven-service", "/maven/path")
                                                                 .withAuthentication("mavenUser", "mavenPassword")
                                                             .endMavenRepo()
                                                             .build();

        assertThat(builtKieServerDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> OpenShiftImageConstants.MAVEN_REPOS.equals(e.getName()))
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo("KIE"));
        assertThat(builtKieServerDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> e.getName().equals("KIE_" + OpenShiftImageConstants.MAVEN_REPO_SERVICE))
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo("maven-service"));
        assertThat(builtKieServerDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> e.getName().equals("KIE_" + OpenShiftImageConstants.MAVEN_REPO_PATH))
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo("/maven/path"));
        assertThat(builtKieServerDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> e.getName().equals("KIE_" + OpenShiftImageConstants.MAVEN_REPO_USERNAME))
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo("mavenUser"));
        assertThat(builtKieServerDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> e.getName().equals("KIE_" + OpenShiftImageConstants.MAVEN_REPO_PASSWORD))
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo("mavenPassword"));
    }

    @Test
    public void testBuildKieServerDeploymentWithMultiplePrefixedMavenRepoService() {
        KieServerDeploymentBuilder settingsBuilder = new KieServerDeploymentBuilder();
        Deployment builtKieServerDeployment = settingsBuilder.withMavenRepo("KIE")
                                                                 .withService("maven-service", "/maven/path")
                                                             .endMavenRepo()
                                                             .withMavenRepo("SECOND_KIE")
                                                                 .withService("maven-second-service", "/maven/path/second")
                                                             .endMavenRepo()
                                                             .build();

        assertThat(builtKieServerDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> OpenShiftImageConstants.MAVEN_REPOS.equals(e.getName()))
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo("KIE,SECOND_KIE"));
        assertThat(builtKieServerDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> e.getName().equals("KIE_" + OpenShiftImageConstants.MAVEN_REPO_SERVICE))
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo("maven-service"));
        assertThat(builtKieServerDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> e.getName().equals("KIE_" + OpenShiftImageConstants.MAVEN_REPO_PATH))
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo("/maven/path"));
        assertThat(builtKieServerDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> e.getName().equals("SECOND_KIE_" + OpenShiftImageConstants.MAVEN_REPO_SERVICE))
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo("maven-second-service"));
        assertThat(builtKieServerDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> e.getName().equals("SECOND_KIE_" + OpenShiftImageConstants.MAVEN_REPO_PATH))
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo("/maven/path/second"));
    }

    @Test(expected = RuntimeException.class)
    public void testBuildKieServerDeploymentWithPrefixedMavenRepoServiceAndUrl() {
        KieServerDeploymentBuilder settingsBuilder = new KieServerDeploymentBuilder();
        settingsBuilder.withMavenRepo("KIE")
                           .withService("maven-service", "/maven/path")
                           .withUrl("someURL")
                       .endMavenRepo()
                       .build();
    }

    @Test(expected = RuntimeException.class)
    public void testBuildKieServerDeploymentWithoutPrefixedMavenRepoServiceOrUrl() {
        KieServerDeploymentBuilder settingsBuilder = new KieServerDeploymentBuilder();
        settingsBuilder.withMavenRepo()
                       .endMavenRepo()
                       .build();
    }

    @Test
    public void testBuildKieServerDeploymentWithMySqlDatabase() {
        MySqlDeploymentBuilder mySqlSettingsBuilder = new MySqlDeploymentBuilder();
        Deployment builtMySqlDeployment = mySqlSettingsBuilder.withDatabaseName("custom-db")
                                                              .withDatabaseUser("mySqlName", "mySqlPassword")
                                                              .build();

        KieServerDeploymentBuilder kieServerSettingsBuilder = new KieServerDeploymentBuilder();
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
        PostgreSqlDeploymentBuilder postgreSqlSettingsBuilder = new PostgreSqlDeploymentBuilder();
        Deployment builtPostgreSqlDeployment = postgreSqlSettingsBuilder.withDatabaseName("custom-db")
                                                                        .withDatabaseUser("postgreSqlName", "postgreSqlPassword")
                                                                        .build();

        KieServerDeploymentBuilder kieServerSettingsBuilder = new KieServerDeploymentBuilder();
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
