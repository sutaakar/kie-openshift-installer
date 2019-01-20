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
}
