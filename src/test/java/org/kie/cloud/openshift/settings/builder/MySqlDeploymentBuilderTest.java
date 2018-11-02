package org.kie.cloud.openshift.settings.builder;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import io.fabric8.kubernetes.api.model.Probe;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeMount;
import org.junit.Test;
import org.kie.cloud.openshift.AbstractCloudTest;
import org.kie.cloud.openshift.OpenShiftImageConstants;
import org.kie.cloud.openshift.deployment.Deployment;

public class MySqlDeploymentBuilderTest extends AbstractCloudTest{

    @Test
    public void testBuildMySqlDeployment() {
        MySqlDeploymentBuilder settingsBuilder = new MySqlDeploymentBuilder();
        Deployment builtMySqlDeployment = settingsBuilder.build();

        assertThat(builtMySqlDeployment).isNotNull();
        assertThat(builtMySqlDeployment.getObjects()).size().isGreaterThan(0);
    }

    @Test
    public void testBuildMySqlDeploymentCustomDeploymentName() {
        MySqlDeploymentBuilder settingsBuilder = new MySqlDeploymentBuilder("custom-sql");
        Deployment builtMySqlDeployment = settingsBuilder.build();

        assertThat(builtMySqlDeployment).isNotNull();
        assertThat(builtMySqlDeployment.getDeploymentConfig().getMetadata().getName()).isEqualTo("custom-sql");
    }

    @Test
    public void testBuildMySqlDeploymentDeploymentConfig() {
        MySqlDeploymentBuilder settingsBuilder = new MySqlDeploymentBuilder();
        Deployment builtMySqlDeployment = settingsBuilder.build();

        assertThat(builtMySqlDeployment).isNotNull();
        assertThat(builtMySqlDeployment.getDeploymentConfig().getApiVersion()).isEqualTo("v1");
        assertThat(builtMySqlDeployment.getDeploymentConfig().getMetadata().getName()).isEqualTo(builtMySqlDeployment.getDeploymentName());
        assertThat(builtMySqlDeployment.getDeploymentConfig().getSpec().getStrategy().getType()).isEqualTo("Recreate");
        assertThat(builtMySqlDeployment.getDeploymentConfig().getSpec().getTriggers()).hasSize(2);
        assertThat(builtMySqlDeployment.getDeploymentConfig().getSpec().getTriggers())
                    .filteredOn(t -> t.getType().equals("ConfigChange"))
                    .hasSize(1);
        assertThat(builtMySqlDeployment.getDeploymentConfig().getSpec().getTriggers())
                    .filteredOn(t -> t.getType().equals("ImageChange"))
                    .hasOnlyOneElementSatisfying(e -> {
                        assertThat(e.getImageChangeParams().getAutomatic()).isTrue();
                        assertThat(e.getImageChangeParams().getContainerNames()).containsExactly(builtMySqlDeployment.getDeploymentName());
                        assertThat(e.getImageChangeParams().getFrom().getKind()).isEqualTo("ImageStreamTag");
                        assertThat(e.getImageChangeParams().getFrom().getNamespace()).isEqualTo("openshift");
                        assertThat(e.getImageChangeParams().getFrom().getName()).isEqualTo("mysql:5.7");
                    });
        assertThat(builtMySqlDeployment.getDeploymentConfig().getSpec().getReplicas()).isEqualTo(1);
        assertThat(builtMySqlDeployment.getDeploymentConfig().getSpec().getSelector()).containsEntry("deploymentConfig", builtMySqlDeployment.getDeploymentName());
        assertThat(builtMySqlDeployment.getDeploymentConfig().getSpec().getTemplate().getMetadata().getName()).isEqualTo(builtMySqlDeployment.getDeploymentName());
        assertThat(builtMySqlDeployment.getDeploymentConfig().getSpec().getTemplate().getMetadata().getLabels()).containsEntry("deploymentConfig", builtMySqlDeployment.getDeploymentName());
        assertThat(builtMySqlDeployment.getDeploymentConfig().getSpec().getTemplate().getMetadata().getLabels()).containsEntry("service", builtMySqlDeployment.getDeploymentName());
        assertThat(builtMySqlDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getTerminationGracePeriodSeconds()).isEqualTo(60L);
        assertThat(builtMySqlDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers())
                    .hasOnlyOneElementSatisfying(c -> {
                        assertThat(c.getName()).isEqualTo(builtMySqlDeployment.getDeploymentName());
                        assertThat(c.getImage()).isEqualTo("mysql");
                        assertThat(c.getImagePullPolicy()).isEqualTo("Always");
                        assertThat(c.getPorts()).hasSize(1);
                        assertThat(c.getPorts().get(0).getContainerPort()).isEqualTo(3306);
                        assertThat(c.getPorts().get(0).getProtocol()).isEqualTo("TCP");
                    });
        assertThat(builtMySqlDeployment.getDeploymentConfig().getMetadata().getAnnotations()).containsEntry("template.alpha.openshift.io/wait-for-ready", "true");
        assertThat(builtMySqlDeployment.getDeploymentConfig().getMetadata().getLabels()).containsEntry("service", builtMySqlDeployment.getDeploymentName());
    }

    @Test
    public void testBuildMySqlDeploymentService() {
        MySqlDeploymentBuilder settingsBuilder = new MySqlDeploymentBuilder();
        Deployment builtMySqlDeployment = settingsBuilder.build();

        assertThat(builtMySqlDeployment).isNotNull();
        assertThat(builtMySqlDeployment.getServices()).hasSize(1);
        assertThat(builtMySqlDeployment.getServices().get(0).getApiVersion()).isEqualTo("v1");
        assertThat(builtMySqlDeployment.getServices().get(0).getSpec().getPorts()).hasSize(1);
        assertThat(builtMySqlDeployment.getServices().get(0).getSpec().getPorts().get(0).getPort()).isEqualTo(3306);
        assertThat(builtMySqlDeployment.getServices().get(0).getSpec().getPorts().get(0).getTargetPort().getIntVal()).isEqualTo(3306);
        assertThat(builtMySqlDeployment.getServices().get(0).getSpec().getSelector()).containsEntry("deploymentConfig", builtMySqlDeployment.getDeploymentName());
        assertThat(builtMySqlDeployment.getServices().get(0).getMetadata().getName()).isEqualTo(builtMySqlDeployment.getDeploymentName());
        assertThat(builtMySqlDeployment.getServices().get(0).getMetadata().getAnnotations()).containsEntry("description", "The database server's port.");
        assertThat(builtMySqlDeployment.getServices().get(0).getMetadata().getLabels()).containsEntry("service", builtMySqlDeployment.getDeploymentName());
    }

    @Test
    public void testBuildMySqlDeploymentDefaultValues() {
        MySqlDeploymentBuilder settingsBuilder = new MySqlDeploymentBuilder();
        Deployment builtMySqlDeployment = settingsBuilder.build();

        assertThat(builtMySqlDeployment).isNotNull();
        assertThat(builtMySqlDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> OpenShiftImageConstants.MYSQL_DATABASE.equals(e.getName()))
                        .isNotEmpty();
        assertThat(builtMySqlDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> OpenShiftImageConstants.MYSQL_USER.equals(e.getName()))
                        .isNotEmpty();
        assertThat(builtMySqlDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> OpenShiftImageConstants.MYSQL_PASSWORD.equals(e.getName()))
                        .isNotEmpty();
    }

    @Test
    public void testBuildMySqlDeploymentLivenessProbe() {
        MySqlDeploymentBuilder settingsBuilder = new MySqlDeploymentBuilder();
        Deployment builtMySqlDeployment = settingsBuilder.build();
        Probe livenessProbe = builtMySqlDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0).getLivenessProbe();

        assertThat(livenessProbe).isNotNull();
        assertThat(livenessProbe.getTcpSocket().getPort().getIntVal()).isEqualTo(3306);
        assertThat(livenessProbe.getInitialDelaySeconds()).isEqualTo(30);
        assertThat(livenessProbe.getTimeoutSeconds()).isEqualTo(1);
    }

    @Test
    public void testBuildMySqlDeploymentReadinessProbe() {
        MySqlDeploymentBuilder settingsBuilder = new MySqlDeploymentBuilder();
        Deployment builtMySqlDeployment = settingsBuilder.build();
        Probe readinessProbe = builtMySqlDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0).getReadinessProbe();

        assertThat(readinessProbe).isNotNull();
        assertThat(readinessProbe.getExec().getCommand()).containsExactly("/bin/sh", "-i", "-c", "MYSQL_PWD=\"$MYSQL_PASSWORD\" mysql -h 127.0.0.1 -u $MYSQL_USER -D $MYSQL_DATABASE -e 'SELECT 1'");
        assertThat(readinessProbe.getInitialDelaySeconds()).isEqualTo(5);
        assertThat(readinessProbe.getTimeoutSeconds()).isEqualTo(1);
    }

    @Test
    public void testBuildMySqlDeploymentWithImageStreamNamespaceFromProperties() {
        MySqlDeploymentBuilder settingsBuilder = new MySqlDeploymentBuilder();
        Deployment builtMySqlDeployment = settingsBuilder.withImageStreamNamespaceFromProperties()
                                                         .build();

        assertThat(builtMySqlDeployment.getDeploymentConfig().getSpec().getTriggers())
                    .filteredOn(t -> t.getType().equals("ImageChange"))
                    .hasOnlyOneElementSatisfying(e -> {
                        assertThat(e.getImageChangeParams().getFrom().getNamespace()).isEqualTo("${MYSQL_IMAGE_STREAM_NAMESPACE}");
                    });
        assertThat(builtMySqlDeployment.getParameters())
                    .filteredOn(p -> OpenShiftImageConstants.MYSQL_IMAGE_STREAM_NAMESPACE.equals(p.getName()))
                    .hasOnlyOneElementSatisfying(p -> {
                        assertThat(p.getDisplayName()).isEqualTo("MySQL ImageStream Namespace");
                        assertThat(p.getValue()).isEqualTo("openshift");
                        assertThat(p.getRequired()).isEqualTo(Boolean.FALSE);
                    });
    }

    @Test
    public void testBuildMySqlDeploymentWithCustomImageStreamNamespace() {
        MySqlDeploymentBuilder settingsBuilder = new MySqlDeploymentBuilder();
        Deployment builtMySqlDeployment = settingsBuilder.withImageStreamNamespace("custom-namespace")
                                                         .build();

        assertThat(builtMySqlDeployment).isNotNull();
        assertThat(builtMySqlDeployment.getDeploymentConfig().getSpec().getTriggers())
                    .filteredOn(t -> t.getType().equals("ImageChange"))
                    .hasOnlyOneElementSatisfying(e -> {
                        assertThat(e.getImageChangeParams().getFrom().getNamespace()).isEqualTo("custom-namespace");
                    });
    }

    @Test
    public void testBuildMySqlDeploymentWithImageStreamTagFromProperties() {
        MySqlDeploymentBuilder settingsBuilder = new MySqlDeploymentBuilder();
        Deployment builtMySqlDeployment = settingsBuilder.withImageStreamTagFromProperties()
                                                             .build();

        assertThat(builtMySqlDeployment.getDeploymentConfig().getSpec().getTriggers())
                    .filteredOn(t -> t.getType().equals("ImageChange"))
                    .hasOnlyOneElementSatisfying(e -> {
                        assertThat(e.getImageChangeParams().getFrom().getName()).endsWith(":${MYSQL_IMAGE_STREAM_TAG}");
                    });
        assertThat(builtMySqlDeployment.getParameters())
                    .filteredOn(p -> OpenShiftImageConstants.MYSQL_IMAGE_STREAM_TAG.equals(p.getName()))
                    .hasOnlyOneElementSatisfying(p -> {
                        assertThat(p.getDisplayName()).isEqualTo("MySQL ImageStream Tag");
                        assertThat(p.getValue()).isEqualTo("5.7");
                        assertThat(p.getRequired()).isEqualTo(Boolean.FALSE);
                    });
    }

    @Test
    public void testBuildMySqlDeploymentWithCustomImageStreamTag() {
        MySqlDeploymentBuilder settingsBuilder = new MySqlDeploymentBuilder();
        Deployment builtMySqlDeployment = settingsBuilder.withImageStreamTag("123")
                                                             .build();

        assertThat(builtMySqlDeployment).isNotNull();
        assertThat(builtMySqlDeployment.getDeploymentConfig().getSpec().getTriggers())
                    .filteredOn(t -> t.getType().equals("ImageChange"))
                    .hasOnlyOneElementSatisfying(e -> {
                        assertThat(e.getImageChangeParams().getFrom().getName()).endsWith(":123");
                    });
    }

    @Test
    public void testBuildMySqlDeploymentWithMySqlUserFromProperties() {
        MySqlDeploymentBuilder settingsBuilder = new MySqlDeploymentBuilder();
        Deployment builtMySqlDeployment = settingsBuilder.withDatabaseUserFromProperties()
                                                         .build();

        assertThat(builtMySqlDeployment).isNotNull();
        assertThat(builtMySqlDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> OpenShiftImageConstants.MYSQL_USER.equals(e.getName()))
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo("${KIE_SERVER_MYSQL_USER}"));
        assertThat(builtMySqlDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> OpenShiftImageConstants.MYSQL_PASSWORD.equals(e.getName()))
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo("${KIE_SERVER_MYSQL_PWD}"));
        assertThat(builtMySqlDeployment.getParameters())
                        .filteredOn(p -> OpenShiftImageConstants.KIE_SERVER_MYSQL_USER.equals(p.getName()))
                        .hasOnlyOneElementSatisfying(p -> {
                            assertThat(p.getDisplayName()).isEqualTo("KIE Server MySQL Database User");
                            assertThat(p.getValue()).isEqualTo("rhpam");
                            assertThat(p.getRequired()).isEqualTo(Boolean.FALSE);
                        });
        assertThat(builtMySqlDeployment.getParameters())
                        .filteredOn(p -> OpenShiftImageConstants.KIE_SERVER_MYSQL_PWD.equals(p.getName()))
                        .hasOnlyOneElementSatisfying(p -> {
                            assertThat(p.getDisplayName()).isEqualTo("KIE Server MySQL Database Password");
                            assertThat(p.getGenerate()).isEqualTo("expression");
                            assertThat(p.getFrom()).isEqualTo("[a-zA-Z]{6}[0-9]{1}!");
                            assertThat(p.getRequired()).isEqualTo(Boolean.FALSE);
                        });
    }

    @Test
    public void testBuildMySqlDeploymentWithMySqlUser() {
        MySqlDeploymentBuilder settingsBuilder = new MySqlDeploymentBuilder();
        Deployment builtMySqlDeployment = settingsBuilder.withDatabaseUser("mySqlName", "mySqlPassword")
                                                         .build();

        assertThat(builtMySqlDeployment).isNotNull();
        assertThat(builtMySqlDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> OpenShiftImageConstants.MYSQL_USER.equals(e.getName()))
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo("mySqlName"));
        assertThat(builtMySqlDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> OpenShiftImageConstants.MYSQL_PASSWORD.equals(e.getName()))
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo("mySqlPassword"));
    }

    @Test
    public void testBuildMySqlDeploymentWithDbNameFromProperties() {
        MySqlDeploymentBuilder settingsBuilder = new MySqlDeploymentBuilder();
        Deployment builtMySqlDeployment = settingsBuilder.withDatabaseNameFromProperties()
                                                         .build();

        assertThat(builtMySqlDeployment).isNotNull();
        assertThat(builtMySqlDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> OpenShiftImageConstants.MYSQL_DATABASE.equals(e.getName()))
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo("${KIE_SERVER_MYSQL_DB}"));
        assertThat(builtMySqlDeployment.getParameters())
                        .filteredOn(p -> OpenShiftImageConstants.KIE_SERVER_MYSQL_DB.equals(p.getName()))
                        .hasOnlyOneElementSatisfying(p -> {
                            assertThat(p.getDisplayName()).isEqualTo("KIE Server MySQL Database Name");
                            assertThat(p.getValue()).isEqualTo("rhpam7");
                            assertThat(p.getRequired()).isEqualTo(Boolean.FALSE);
                        });
    }

    @Test
    public void testBuildMySqlDeploymentWithDbName() {
        MySqlDeploymentBuilder settingsBuilder = new MySqlDeploymentBuilder();
        Deployment builtMySqlDeployment = settingsBuilder.withDatabaseName("custom-db")
                                                         .build();

        assertThat(builtMySqlDeployment).isNotNull();
        assertThat(builtMySqlDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> OpenShiftImageConstants.MYSQL_DATABASE.equals(e.getName()))
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo("custom-db"));
    }

    @Test
    public void testBuildMySqlDeploymentWithPersistence() {
        MySqlDeploymentBuilder settingsBuilder = new MySqlDeploymentBuilder();
        Deployment builtMySqlDeployment = settingsBuilder.makePersistent()
                                                         .build();

        List<VolumeMount> volumeMounts = builtMySqlDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0).getVolumeMounts();
        List<Volume> volumes = builtMySqlDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getVolumes();
        assertThat(volumeMounts)
                        .hasOnlyOneElementSatisfying(m -> {
                            assertThat(m.getName()).isEqualTo(builtMySqlDeployment.getDeploymentName() + "-pvol");
                            assertThat(m.getMountPath()).isEqualTo("/var/lib/mysql/data");
                        });
        assertThat(volumes)
                        .hasOnlyOneElementSatisfying(v -> {
                            assertThat(v.getName()).isEqualTo(volumeMounts.get(0).getName());
                            assertThat(v.getPersistentVolumeClaim().getClaimName()).isEqualTo(builtMySqlDeployment.getDeploymentName() + "-claim");
                        });
        assertThat(builtMySqlDeployment.getPersistentVolumeClaims())
                        .hasOnlyOneElementSatisfying(p -> {
                            assertThat(p.getMetadata().getName()).isEqualTo(volumes.get(0).getPersistentVolumeClaim().getClaimName());
                            assertThat(p.getMetadata().getLabels()).containsEntry("service", builtMySqlDeployment.getDeploymentName());
                            assertThat(p.getSpec().getAccessModes()).containsOnlyOnce("ReadWriteOnce");
                            assertThat(p.getSpec().getResources().getRequests().get("storage")).isEqualTo(new Quantity("1Gi"));
                        });
    }

    @Test
    public void testBuildMySqlDeploymentWithCustomPersistence() {
        MySqlDeploymentBuilder settingsBuilder = new MySqlDeploymentBuilder();
        Deployment builtMySqlDeployment = settingsBuilder.makePersistent("64Mi")
                                                         .build();

        List<VolumeMount> volumeMounts = builtMySqlDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0).getVolumeMounts();
        List<Volume> volumes = builtMySqlDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getVolumes();
        assertThat(volumeMounts)
                        .hasOnlyOneElementSatisfying(m -> {
                            assertThat(m.getName()).isEqualTo(builtMySqlDeployment.getDeploymentName() + "-pvol");
                            assertThat(m.getMountPath()).isEqualTo("/var/lib/mysql/data");
                        });
        assertThat(volumes)
                        .hasOnlyOneElementSatisfying(v -> {
                            assertThat(v.getName()).isEqualTo(volumeMounts.get(0).getName());
                            assertThat(v.getPersistentVolumeClaim().getClaimName()).isEqualTo(builtMySqlDeployment.getDeploymentName() + "-claim");
                        });
        assertThat(builtMySqlDeployment.getPersistentVolumeClaims())
                        .hasOnlyOneElementSatisfying(p -> {
                            assertThat(p.getMetadata().getName()).isEqualTo(volumes.get(0).getPersistentVolumeClaim().getClaimName());
                            assertThat(p.getMetadata().getLabels()).containsEntry("service", builtMySqlDeployment.getDeploymentName());
                            assertThat(p.getSpec().getAccessModes()).containsOnlyOnce("ReadWriteOnce");
                            assertThat(p.getSpec().getResources().getRequests().get("storage")).isEqualTo(new Quantity("64Mi"));
                        });
    }

    @Test
    public void testBuildMySqlDeploymentWithPersistenceFromProperties() {
        MySqlDeploymentBuilder settingsBuilder = new MySqlDeploymentBuilder();
        Deployment builtMySqlDeployment = settingsBuilder.makePersistentFromProperties()
                                                         .build();

        List<VolumeMount> volumeMounts = builtMySqlDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0).getVolumeMounts();
        List<Volume> volumes = builtMySqlDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getVolumes();
        assertThat(volumeMounts)
                        .hasOnlyOneElementSatisfying(m -> {
                            assertThat(m.getName()).isEqualTo(builtMySqlDeployment.getDeploymentName() + "-pvol");
                            assertThat(m.getMountPath()).isEqualTo("/var/lib/mysql/data");
                        });
        assertThat(volumes)
                        .hasOnlyOneElementSatisfying(v -> {
                            assertThat(v.getName()).isEqualTo(volumeMounts.get(0).getName());
                            assertThat(v.getPersistentVolumeClaim().getClaimName()).isEqualTo(builtMySqlDeployment.getDeploymentName() + "-claim");
                        });
        assertThat(builtMySqlDeployment.getPersistentVolumeClaims())
                        .hasOnlyOneElementSatisfying(p -> {
                            assertThat(p.getMetadata().getName()).isEqualTo(volumes.get(0).getPersistentVolumeClaim().getClaimName());
                            assertThat(p.getMetadata().getLabels()).containsEntry("service", builtMySqlDeployment.getDeploymentName());
                            assertThat(p.getSpec().getAccessModes()).containsOnlyOnce("ReadWriteOnce");
                            assertThat(p.getSpec().getResources().getRequests().get("storage")).isEqualTo(new Quantity("${DB_VOLUME_CAPACITY}"));
                        });
        assertThat(builtMySqlDeployment.getParameters())
                        .filteredOn(p -> OpenShiftImageConstants.DB_VOLUME_CAPACITY.equals(p.getName()))
                        .hasOnlyOneElementSatisfying(p -> {
                            assertThat(p.getDisplayName()).isEqualTo("Database Volume Capacity");
                            assertThat(p.getValue()).isEqualTo("1Gi");
                            assertThat(p.getRequired()).isEqualTo(Boolean.TRUE);
                        });
    }
}
