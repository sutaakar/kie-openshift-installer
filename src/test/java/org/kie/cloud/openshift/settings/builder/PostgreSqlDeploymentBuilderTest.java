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

public class PostgreSqlDeploymentBuilderTest extends AbstractCloudTest{

    @Test
    public void testBuildPostgreSqlDeployment() {
        PostgreSqlDeploymentBuilder settingsBuilder = new PostgreSqlDeploymentBuilder();
        Deployment builtPostgreSqlDeployment = settingsBuilder.build();

        assertThat(builtPostgreSqlDeployment).isNotNull();
        assertThat(builtPostgreSqlDeployment.getObjects()).size().isGreaterThan(0);
    }

    @Test
    public void testBuildPostgreSqlDeploymentCustomDeploymentName() {
        PostgreSqlDeploymentBuilder settingsBuilder = new PostgreSqlDeploymentBuilder("custom-sql");
        Deployment builtPostgreSqlDeployment = settingsBuilder.build();

        assertThat(builtPostgreSqlDeployment).isNotNull();
        assertThat(builtPostgreSqlDeployment.getDeploymentConfig().getMetadata().getName()).isEqualTo("custom-sql");
    }

    @Test
    public void testBuildKieServerDeploymentDeploymentConfig() {
        PostgreSqlDeploymentBuilder settingsBuilder = new PostgreSqlDeploymentBuilder();
        Deployment builtPostgreSqlDeployment = settingsBuilder.build();

        assertThat(builtPostgreSqlDeployment).isNotNull();
        assertThat(builtPostgreSqlDeployment.getDeploymentConfig().getApiVersion()).isEqualTo("v1");
        assertThat(builtPostgreSqlDeployment.getDeploymentConfig().getMetadata().getName()).isEqualTo(builtPostgreSqlDeployment.getDeploymentName());
        assertThat(builtPostgreSqlDeployment.getDeploymentConfig().getSpec().getStrategy().getType()).isEqualTo("Recreate");
        assertThat(builtPostgreSqlDeployment.getDeploymentConfig().getSpec().getTriggers()).hasSize(2);
        assertThat(builtPostgreSqlDeployment.getDeploymentConfig().getSpec().getTriggers())
                    .filteredOn(t -> t.getType().equals("ConfigChange"))
                    .hasSize(1);
        assertThat(builtPostgreSqlDeployment.getDeploymentConfig().getSpec().getTriggers())
                    .filteredOn(t -> t.getType().equals("ImageChange"))
                    .hasOnlyOneElementSatisfying(e -> {
                        assertThat(e.getImageChangeParams().getAutomatic()).isTrue();
                        assertThat(e.getImageChangeParams().getContainerNames()).containsExactly(builtPostgreSqlDeployment.getDeploymentName());
                        assertThat(e.getImageChangeParams().getFrom().getKind()).isEqualTo("ImageStreamTag");
                        assertThat(e.getImageChangeParams().getFrom().getNamespace()).isEqualTo("openshift");
                        assertThat(e.getImageChangeParams().getFrom().getName()).isEqualTo("postgresql:10");
                    });
        assertThat(builtPostgreSqlDeployment.getDeploymentConfig().getSpec().getReplicas()).isEqualTo(1);
        assertThat(builtPostgreSqlDeployment.getDeploymentConfig().getSpec().getSelector()).containsEntry("deploymentConfig", builtPostgreSqlDeployment.getDeploymentName());
        assertThat(builtPostgreSqlDeployment.getDeploymentConfig().getSpec().getTemplate().getMetadata().getName()).isEqualTo(builtPostgreSqlDeployment.getDeploymentName());
        assertThat(builtPostgreSqlDeployment.getDeploymentConfig().getSpec().getTemplate().getMetadata().getLabels()).containsEntry("deploymentConfig", builtPostgreSqlDeployment.getDeploymentName());
        assertThat(builtPostgreSqlDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getTerminationGracePeriodSeconds()).isEqualTo(60L);
        assertThat(builtPostgreSqlDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers())
                    .hasOnlyOneElementSatisfying(c -> {
                        assertThat(c.getName()).isEqualTo(builtPostgreSqlDeployment.getDeploymentName());
                        assertThat(c.getImage()).isEqualTo("postgresql");
                        assertThat(c.getImagePullPolicy()).isEqualTo("Always");
                        assertThat(c.getPorts()).hasSize(1);
                        assertThat(c.getPorts().get(0).getContainerPort()).isEqualTo(5432);
                        assertThat(c.getPorts().get(0).getProtocol()).isEqualTo("TCP");
                    });
    }

    @Test
    public void testBuildPostgreSqlDeploymentService() {
        PostgreSqlDeploymentBuilder settingsBuilder = new PostgreSqlDeploymentBuilder();
        Deployment builtPostgreSqlDeployment = settingsBuilder.build();

        assertThat(builtPostgreSqlDeployment).isNotNull();
        assertThat(builtPostgreSqlDeployment.getServices()).hasSize(1);
        assertThat(builtPostgreSqlDeployment.getServices().get(0).getApiVersion()).isEqualTo("v1");
        assertThat(builtPostgreSqlDeployment.getServices().get(0).getSpec().getPorts()).hasSize(1);
        assertThat(builtPostgreSqlDeployment.getServices().get(0).getSpec().getPorts().get(0).getPort()).isEqualTo(5432);
        assertThat(builtPostgreSqlDeployment.getServices().get(0).getSpec().getPorts().get(0).getTargetPort().getIntVal()).isEqualTo(5432);
        assertThat(builtPostgreSqlDeployment.getServices().get(0).getSpec().getSelector()).containsEntry("deploymentConfig", builtPostgreSqlDeployment.getDeploymentName());
        assertThat(builtPostgreSqlDeployment.getServices().get(0).getMetadata().getName()).isEqualTo(builtPostgreSqlDeployment.getDeploymentName());
    }

    @Test
    public void testBuildPostgreSqlDeploymentDefaultValues() {
        PostgreSqlDeploymentBuilder settingsBuilder = new PostgreSqlDeploymentBuilder();
        Deployment builtPostgreSqlDeployment = settingsBuilder.build();

        assertThat(builtPostgreSqlDeployment).isNotNull();
        assertThat(builtPostgreSqlDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> OpenShiftImageConstants.POSTGRESQL_DATABASE.equals(e.getName()))
                        .isNotEmpty();
        assertThat(builtPostgreSqlDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> OpenShiftImageConstants.POSTGRESQL_USER.equals(e.getName()))
                        .isNotEmpty();
        assertThat(builtPostgreSqlDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> OpenShiftImageConstants.POSTGRESQL_PASSWORD.equals(e.getName()))
                        .isNotEmpty();
        assertThat(builtPostgreSqlDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> OpenShiftImageConstants.POSTGRESQL_MAX_PREPARED_TRANSACTIONS.equals(e.getName()))
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo("100"));
    }

    @Test
    public void testBuildPostgreSqlDeploymentLivenessProbe() {
        PostgreSqlDeploymentBuilder settingsBuilder = new PostgreSqlDeploymentBuilder();
        Deployment builtPostgreSqlDeployment = settingsBuilder.build();
        Probe livenessProbe = builtPostgreSqlDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0).getLivenessProbe();

        assertThat(livenessProbe).isNotNull();
        assertThat(livenessProbe.getExec().getCommand()).containsExactly("/usr/libexec/check-container", "--live");
        assertThat(livenessProbe.getInitialDelaySeconds()).isEqualTo(120);
        assertThat(livenessProbe.getTimeoutSeconds()).isEqualTo(10);
    }

    @Test
    public void testBuildPostgreSqlDeploymentReadinessProbe() {
        PostgreSqlDeploymentBuilder settingsBuilder = new PostgreSqlDeploymentBuilder();
        Deployment builtPostgreSqlDeployment = settingsBuilder.build();
        Probe readinessProbe = builtPostgreSqlDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0).getReadinessProbe();

        assertThat(readinessProbe).isNotNull();
        assertThat(readinessProbe.getExec().getCommand()).containsExactly("/usr/libexec/check-container");
        assertThat(readinessProbe.getInitialDelaySeconds()).isEqualTo(5);
        assertThat(readinessProbe.getTimeoutSeconds()).isEqualTo(1);
    }

    @Test
    public void testBuildPostgreSqlDeploymentWithImageStreamNamespaceFromProperties() {
        PostgreSqlDeploymentBuilder settingsBuilder = new PostgreSqlDeploymentBuilder();
        Deployment builtPostgreSqlDeployment = settingsBuilder.withImageStreamNamespaceFromProperties()
                                                              .build();

        assertThat(builtPostgreSqlDeployment.getDeploymentConfig().getSpec().getTriggers())
                    .filteredOn(t -> t.getType().equals("ImageChange"))
                    .hasOnlyOneElementSatisfying(e -> {
                        assertThat(e.getImageChangeParams().getFrom().getNamespace()).isEqualTo("${POSTGRESQL_IMAGE_STREAM_NAMESPACE}");
                    });
        assertThat(builtPostgreSqlDeployment.getParameters())
                    .filteredOn(p -> OpenShiftImageConstants.POSTGRESQL_IMAGE_STREAM_NAMESPACE.equals(p.getName()))
                    .hasOnlyOneElementSatisfying(p -> {
                        assertThat(p.getDisplayName()).isEqualTo("PostgreSQL ImageStream Namespace");
                        assertThat(p.getValue()).isEqualTo("openshift");
                        assertThat(p.getRequired()).isEqualTo(Boolean.FALSE);
                    });
    }

    @Test
    public void testBuildPostgreSqlDeploymentWithCustomImageStreamNamespace() {
        PostgreSqlDeploymentBuilder settingsBuilder = new PostgreSqlDeploymentBuilder();
        Deployment builtPostgreSqlDeployment = settingsBuilder.withImageStreamNamespace("custom-namespace")
                                                              .build();

        assertThat(builtPostgreSqlDeployment).isNotNull();
        assertThat(builtPostgreSqlDeployment.getDeploymentConfig().getSpec().getTriggers())
                    .filteredOn(t -> t.getType().equals("ImageChange"))
                    .hasOnlyOneElementSatisfying(e -> {
                        assertThat(e.getImageChangeParams().getFrom().getNamespace()).isEqualTo("custom-namespace");
                    });
    }

    @Test
    public void testBuildPostgreSqlDeploymentWithImageStreamTagFromProperties() {
        PostgreSqlDeploymentBuilder settingsBuilder = new PostgreSqlDeploymentBuilder();
        Deployment builtPostgreSqlDeployment = settingsBuilder.withImageStreamTagFromProperties()
                                                              .build();

        assertThat(builtPostgreSqlDeployment.getDeploymentConfig().getSpec().getTriggers())
                    .filteredOn(t -> t.getType().equals("ImageChange"))
                    .hasOnlyOneElementSatisfying(e -> {
                        assertThat(e.getImageChangeParams().getFrom().getName()).endsWith(":${POSTGRESQL_IMAGE_STREAM_TAG}");
                    });
        assertThat(builtPostgreSqlDeployment.getParameters())
                    .filteredOn(p -> OpenShiftImageConstants.POSTGRESQL_IMAGE_STREAM_TAG.equals(p.getName()))
                    .hasOnlyOneElementSatisfying(p -> {
                        assertThat(p.getDisplayName()).isEqualTo("PostgreSQL ImageStream Tag");
                        assertThat(p.getValue()).isEqualTo("10");
                        assertThat(p.getRequired()).isEqualTo(Boolean.FALSE);
                    });
    }

    @Test
    public void testBuildPostgreSqlDeploymentWithCustomImageStreamTag() {
        PostgreSqlDeploymentBuilder settingsBuilder = new PostgreSqlDeploymentBuilder();
        Deployment builtPostgreSqlDeployment = settingsBuilder.withImageStreamTag("123")
                                                              .build();

        assertThat(builtPostgreSqlDeployment).isNotNull();
        assertThat(builtPostgreSqlDeployment.getDeploymentConfig().getSpec().getTriggers())
                    .filteredOn(t -> t.getType().equals("ImageChange"))
                    .hasOnlyOneElementSatisfying(e -> {
                        assertThat(e.getImageChangeParams().getFrom().getName()).endsWith(":123");
                    });
    }

    @Test
    public void testBuildMySqlDeploymentWithPostgreSqlUserFromProperties() {
        PostgreSqlDeploymentBuilder settingsBuilder = new PostgreSqlDeploymentBuilder();
        Deployment builtPostgreSqlDeployment = settingsBuilder.withDatabaseUserFromProperties()
                                                              .build();

        assertThat(builtPostgreSqlDeployment).isNotNull();
        assertThat(builtPostgreSqlDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> OpenShiftImageConstants.POSTGRESQL_USER.equals(e.getName()))
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo("${KIE_SERVER_POSTGRESQL_USER}"));
        assertThat(builtPostgreSqlDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> OpenShiftImageConstants.POSTGRESQL_PASSWORD.equals(e.getName()))
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo("${KIE_SERVER_POSTGRESQL_PWD}"));
        assertThat(builtPostgreSqlDeployment.getParameters())
                        .filteredOn(p -> OpenShiftImageConstants.KIE_SERVER_POSTGRESQL_USER.equals(p.getName()))
                        .hasOnlyOneElementSatisfying(p -> {
                            assertThat(p.getDisplayName()).isEqualTo("KIE Server PostgreSQL Database User");
                            assertThat(p.getValue()).isEqualTo("rhpam");
                            assertThat(p.getRequired()).isEqualTo(Boolean.FALSE);
                        });
        assertThat(builtPostgreSqlDeployment.getParameters())
                        .filteredOn(p -> OpenShiftImageConstants.KIE_SERVER_POSTGRESQL_PWD.equals(p.getName()))
                        .hasOnlyOneElementSatisfying(p -> {
                            assertThat(p.getDisplayName()).isEqualTo("KIE Server PostgreSQL Database Password");
                            assertThat(p.getGenerate()).isEqualTo("expression");
                            assertThat(p.getFrom()).isEqualTo("[a-zA-Z]{6}[0-9]{1}!");
                            assertThat(p.getRequired()).isEqualTo(Boolean.FALSE);
                        });
    }

    @Test
    public void testBuildPostgreSqlDeploymentWithPostgreSqlUser() {
        PostgreSqlDeploymentBuilder settingsBuilder = new PostgreSqlDeploymentBuilder();
        Deployment builtPostgreSqlDeployment = settingsBuilder.withDatabaseUser("postgreSqlName", "postgreSqlPassword")
                                                              .build();

        assertThat(builtPostgreSqlDeployment).isNotNull();
        assertThat(builtPostgreSqlDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> OpenShiftImageConstants.POSTGRESQL_USER.equals(e.getName()))
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo("postgreSqlName"));
        assertThat(builtPostgreSqlDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> OpenShiftImageConstants.POSTGRESQL_PASSWORD.equals(e.getName()))
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo("postgreSqlPassword"));
    }

    @Test
    public void testBuildPostgreSqlDeploymentWithDbNameFromProperties() {
        PostgreSqlDeploymentBuilder settingsBuilder = new PostgreSqlDeploymentBuilder();
        Deployment builtPostgreSqlDeployment = settingsBuilder.withDatabaseNameFromProperties()
                                                              .build();

        assertThat(builtPostgreSqlDeployment).isNotNull();
        assertThat(builtPostgreSqlDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> OpenShiftImageConstants.POSTGRESQL_DATABASE.equals(e.getName()))
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo("${KIE_SERVER_POSTGRESQL_DB}"));
        assertThat(builtPostgreSqlDeployment.getParameters())
                        .filteredOn(p -> OpenShiftImageConstants.KIE_SERVER_POSTGRESQL_DB.equals(p.getName()))
                        .hasOnlyOneElementSatisfying(p -> {
                            assertThat(p.getDisplayName()).isEqualTo("KIE Server PostgreSQL Database Name");
                            assertThat(p.getValue()).isEqualTo("rhpam7");
                            assertThat(p.getRequired()).isEqualTo(Boolean.FALSE);
                        });
    }

    @Test
    public void testBuildPostgreSqlDeploymentWithDbName() {
        PostgreSqlDeploymentBuilder settingsBuilder = new PostgreSqlDeploymentBuilder();
        Deployment builtPostgreSqlDeployment = settingsBuilder.withDatabaseName("custom-db")
                                                              .build();

        assertThat(builtPostgreSqlDeployment).isNotNull();
        assertThat(builtPostgreSqlDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> OpenShiftImageConstants.POSTGRESQL_DATABASE.equals(e.getName()))
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo("custom-db"));
    }

    @Test
    public void testBuildPostgreSqlDeploymentWithPersistence() {
        PostgreSqlDeploymentBuilder settingsBuilder = new PostgreSqlDeploymentBuilder();
        Deployment builtPostgreSqlDeployment = settingsBuilder.makePersistent()
                                                              .build();

        List<VolumeMount> volumeMounts = builtPostgreSqlDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0).getVolumeMounts();
        List<Volume> volumes = builtPostgreSqlDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getVolumes();
        assertThat(volumeMounts)
                        .hasOnlyOneElementSatisfying(m -> {
                            assertThat(m.getName()).isEqualTo(builtPostgreSqlDeployment.getDeploymentName() + "-pvol");
                            assertThat(m.getMountPath()).isEqualTo("/var/lib/pgsql/data");
                        });
        assertThat(volumes)
                        .hasOnlyOneElementSatisfying(v -> {
                            assertThat(v.getName()).isEqualTo(volumeMounts.get(0).getName());
                            assertThat(v.getPersistentVolumeClaim().getClaimName()).isEqualTo(builtPostgreSqlDeployment.getDeploymentName() + "-claim");
                        });
        assertThat(builtPostgreSqlDeployment.getPersistentVolumeClaims())
                        .hasOnlyOneElementSatisfying(p -> {
                            assertThat(p.getMetadata().getName()).isEqualTo(volumes.get(0).getPersistentVolumeClaim().getClaimName());
                            assertThat(p.getSpec().getAccessModes()).containsOnlyOnce("ReadWriteOnce");
                            assertThat(p.getSpec().getResources().getRequests().get("storage")).isEqualTo(new Quantity("1Gi"));
                        });
    }

    @Test
    public void testBuildPostgreSqlDeploymentWithCustomPersistence() {
        PostgreSqlDeploymentBuilder settingsBuilder = new PostgreSqlDeploymentBuilder();
        Deployment builtPostgreSqlDeployment = settingsBuilder.makePersistent("64Mi")
                                                              .build();

        List<VolumeMount> volumeMounts = builtPostgreSqlDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0).getVolumeMounts();
        List<Volume> volumes = builtPostgreSqlDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getVolumes();
        assertThat(volumeMounts)
                        .hasOnlyOneElementSatisfying(m -> {
                            assertThat(m.getName()).isEqualTo(builtPostgreSqlDeployment.getDeploymentName() + "-pvol");
                            assertThat(m.getMountPath()).isEqualTo("/var/lib/pgsql/data");
                        });
        assertThat(volumes)
                        .hasOnlyOneElementSatisfying(v -> {
                            assertThat(v.getName()).isEqualTo(volumeMounts.get(0).getName());
                            assertThat(v.getPersistentVolumeClaim().getClaimName()).isEqualTo(builtPostgreSqlDeployment.getDeploymentName() + "-claim");
                        });
        assertThat(builtPostgreSqlDeployment.getPersistentVolumeClaims())
                        .hasOnlyOneElementSatisfying(p -> {
                            assertThat(p.getMetadata().getName()).isEqualTo(volumes.get(0).getPersistentVolumeClaim().getClaimName());
                            assertThat(p.getMetadata().getLabels()).containsEntry("service", builtPostgreSqlDeployment.getDeploymentName());
                            assertThat(p.getSpec().getAccessModes()).containsOnlyOnce("ReadWriteOnce");
                            assertThat(p.getSpec().getResources().getRequests().get("storage")).isEqualTo(new Quantity("64Mi"));
                        });
    }

    @Test
    public void testBuildPostgreSqlDeploymentWithPersistenceFromProperties() {
        PostgreSqlDeploymentBuilder settingsBuilder = new PostgreSqlDeploymentBuilder();
        Deployment builtPostgreSqlDeployment = settingsBuilder.makePersistentFromProperties()
                                                              .build();

        List<VolumeMount> volumeMounts = builtPostgreSqlDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0).getVolumeMounts();
        List<Volume> volumes = builtPostgreSqlDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getVolumes();
        assertThat(volumeMounts)
                        .hasOnlyOneElementSatisfying(m -> {
                            assertThat(m.getName()).isEqualTo(builtPostgreSqlDeployment.getDeploymentName() + "-pvol");
                            assertThat(m.getMountPath()).isEqualTo("/var/lib/pgsql/data");
                        });
        assertThat(volumes)
                        .hasOnlyOneElementSatisfying(v -> {
                            assertThat(v.getName()).isEqualTo(volumeMounts.get(0).getName());
                            assertThat(v.getPersistentVolumeClaim().getClaimName()).isEqualTo(builtPostgreSqlDeployment.getDeploymentName() + "-claim");
                        });
        assertThat(builtPostgreSqlDeployment.getPersistentVolumeClaims())
                        .hasOnlyOneElementSatisfying(p -> {
                            assertThat(p.getMetadata().getName()).isEqualTo(volumes.get(0).getPersistentVolumeClaim().getClaimName());
                            assertThat(p.getMetadata().getLabels()).containsEntry("service", builtPostgreSqlDeployment.getDeploymentName());
                            assertThat(p.getSpec().getAccessModes()).containsOnlyOnce("ReadWriteOnce");
                            assertThat(p.getSpec().getResources().getRequests().get("storage")).isEqualTo(new Quantity("${DB_VOLUME_CAPACITY}"));
                        });
        assertThat(builtPostgreSqlDeployment.getParameters())
                        .filteredOn(p -> OpenShiftImageConstants.DB_VOLUME_CAPACITY.equals(p.getName()))
                        .hasOnlyOneElementSatisfying(p -> {
                            assertThat(p.getDisplayName()).isEqualTo("Database Volume Capacity");
                            assertThat(p.getValue()).isEqualTo("1Gi");
                            assertThat(p.getRequired()).isEqualTo(Boolean.TRUE);
                        });
    }
}
