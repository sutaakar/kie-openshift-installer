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
    public void testBuildMySqlDeploymentService() {
        Template mySqlTemplate = new TemplateLoader(openShiftClient).loadMySqlTemplate();

        MySqlDeploymentBuilder settingsBuilder = new MySqlDeploymentBuilder(mySqlTemplate);
        Deployment builtMySqlDeployment = settingsBuilder.build();

        assertThat(builtMySqlDeployment).isNotNull();
        assertThat(builtMySqlDeployment.getServices()).hasSize(1);
        assertThat(builtMySqlDeployment.getServices().get(0).getApiVersion()).isEqualTo("v1");
        assertThat(builtMySqlDeployment.getServices().get(0).getSpec().getPorts()).hasSize(1);
        assertThat(builtMySqlDeployment.getServices().get(0).getSpec().getPorts().get(0).getPort()).isEqualTo(3306);
        assertThat(builtMySqlDeployment.getServices().get(0).getSpec().getPorts().get(0).getTargetPort().getIntVal()).isEqualTo(3306);
        assertThat(builtMySqlDeployment.getServices().get(0).getSpec().getSelector()).containsEntry("deploymentConfig", "${APPLICATION_NAME}-mysql");
        assertThat(builtMySqlDeployment.getServices().get(0).getMetadata().getName()).isEqualTo("${APPLICATION_NAME}-mysql");
    }

    @Test
    public void testBuildMySqlDeploymentDefaultValues() {
        Template mySqlTemplate = new TemplateLoader(openShiftClient).loadMySqlTemplate();

        MySqlDeploymentBuilder settingsBuilder = new MySqlDeploymentBuilder(mySqlTemplate);
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
    public void testBuildMySqlDeploymentReadinessProbe() {
        Template mySqlTemplate = new TemplateLoader(openShiftClient).loadMySqlTemplate();

        MySqlDeploymentBuilder settingsBuilder = new MySqlDeploymentBuilder(mySqlTemplate);
        Deployment builtMySqlDeployment = settingsBuilder.build();
        Probe readinessProbe = builtMySqlDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0).getReadinessProbe();

        assertThat(readinessProbe).isNotNull();
        assertThat(readinessProbe.getExec().getCommand()).containsExactly("/bin/bash", "-i", "-c", "MYSQL_PWD=\"$MYSQL_PASSWORD\" mysql -h 127.0.0.1 -u $MYSQL_USER -D $MYSQL_DATABASE -e 'SELECT 1'");
        assertThat(readinessProbe.getInitialDelaySeconds()).isEqualTo(5);
        assertThat(readinessProbe.getTimeoutSeconds()).isEqualTo(1);
    }

    @Test
    public void testBuildMySqlDeploymentWithMySqlUser() {
        Template mySqlTemplate = new TemplateLoader(openShiftClient).loadMySqlTemplate();

        MySqlDeploymentBuilder settingsBuilder = new MySqlDeploymentBuilder(mySqlTemplate);
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
    public void testBuildMySqlDeploymentWithDbName() {
        Template mySqlTemplate = new TemplateLoader(openShiftClient).loadMySqlTemplate();

        MySqlDeploymentBuilder settingsBuilder = new MySqlDeploymentBuilder(mySqlTemplate);
        Deployment builtMySqlDeployment = settingsBuilder.withDatabaseName("custom-db")
                                                         .build();

        assertThat(builtMySqlDeployment).isNotNull();
        assertThat(builtMySqlDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> OpenShiftImageConstants.MYSQL_DATABASE.equals(e.getName()))
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo("custom-db"));
    }

    @Test
    public void testBuildMySqlDeploymentWithPersistence() {
        Template mySqlTemplate = new TemplateLoader(openShiftClient).loadMySqlTemplate();

        MySqlDeploymentBuilder settingsBuilder = new MySqlDeploymentBuilder(mySqlTemplate);
        Deployment builtMySqlDeployment = settingsBuilder.makePersistent()
                                                         .build();

        List<VolumeMount> volumeMounts = builtMySqlDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0).getVolumeMounts();
        List<Volume> volumes = builtMySqlDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getVolumes();
        assertThat(volumeMounts)
                        .hasOnlyOneElementSatisfying(m -> {
                            assertThat(m.getName()).contains("-mysql-pvol");
                            assertThat(m.getMountPath()).isEqualTo("/var/lib/mysql/data");
                        });
        assertThat(volumes)
                        .hasOnlyOneElementSatisfying(v -> {
                            assertThat(v.getName()).isEqualTo(volumeMounts.get(0).getName());
                            assertThat(v.getPersistentVolumeClaim().getClaimName()).contains("-mysql-claim");
                        });
        assertThat(builtMySqlDeployment.getPersistentVolumeClaims())
                        .hasOnlyOneElementSatisfying(p -> {
                            assertThat(p.getMetadata().getName()).isEqualTo(volumes.get(0).getPersistentVolumeClaim().getClaimName());
                            assertThat(p.getSpec().getAccessModes()).containsOnlyOnce("ReadWriteOnce");
                            assertThat(p.getSpec().getResources().getRequests().get("storage")).isEqualTo(new Quantity("1Gi"));
                        });
    }
}
