package org.kie.cloud.openshift.settings.builder;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.openshift.api.model.Template;
import org.junit.Test;
import org.kie.cloud.openshift.AbstractCloudTest;
import org.kie.cloud.openshift.OpenShiftImageConstants;
import org.kie.cloud.openshift.deployment.Deployment;
import org.kie.cloud.openshift.template.TemplateLoader;

public class PostgreSqlDeploymentBuilderTest extends AbstractCloudTest{

    @Test
    public void testBuildPostgreSqlDeployment() {
        Template postgreSqlTemplate = new TemplateLoader(openShiftClient).loadPostgreSqlTemplate();

        PostgreSqlDeploymentBuilder settingsBuilder = new PostgreSqlDeploymentBuilder(postgreSqlTemplate);
        Deployment builtPostgreSqlDeployment = settingsBuilder.build();

        assertThat(builtPostgreSqlDeployment).isNotNull();
        assertThat(builtPostgreSqlDeployment.geTemplate().getMetadata().getName()).contains("-postgresql");
    }

    @Test
    public void testBuildPostgreSqlDeploymentDefaultValues() {
        Template postgreSqlTemplate = new TemplateLoader(openShiftClient).loadPostgreSqlTemplate();

        PostgreSqlDeploymentBuilder settingsBuilder = new PostgreSqlDeploymentBuilder(postgreSqlTemplate);
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
    public void testBuildPostgreSqlDeploymentWithPostgreSqlUser() {
        Template postgreSqlTemplate = new TemplateLoader(openShiftClient).loadPostgreSqlTemplate();

        PostgreSqlDeploymentBuilder settingsBuilder = new PostgreSqlDeploymentBuilder(postgreSqlTemplate);
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
        Template postgreSqlTemplate = new TemplateLoader(openShiftClient).loadPostgreSqlTemplate();

        PostgreSqlDeploymentBuilder settingsBuilder = new PostgreSqlDeploymentBuilder(postgreSqlTemplate);
        Deployment builtPostgreSqlDeployment = settingsBuilder.withDatabaseName("custom-db")
                                                              .build();

        assertThat(builtPostgreSqlDeployment).isNotNull();
        assertThat(builtPostgreSqlDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                        .filteredOn(e -> OpenShiftImageConstants.POSTGRESQL_DATABASE.equals(e.getName()))
                        .hasOnlyOneElementSatisfying(e -> assertThat(e.getValue()).isEqualTo("custom-db"));
    }

    @Test
    public void testBuildPostgreSqlDeploymentWithPersistence() {
        Template postgreSqlTemplate = new TemplateLoader(openShiftClient).loadPostgreSqlTemplate();

        PostgreSqlDeploymentBuilder settingsBuilder = new PostgreSqlDeploymentBuilder(postgreSqlTemplate);
        Deployment builtPostgreSqlDeployment = settingsBuilder.makePersistent()
                                                         .build();

        List<VolumeMount> volumeMounts = builtPostgreSqlDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0).getVolumeMounts();
        List<Volume> volumes = builtPostgreSqlDeployment.getDeploymentConfig().getSpec().getTemplate().getSpec().getVolumes();
        assertThat(volumeMounts)
                        .hasOnlyOneElementSatisfying(m -> {
                            assertThat(m.getName()).contains("-postgresql-pvol");
                            assertThat(m.getMountPath()).isEqualTo("/var/lib/postgresql/data");
                        });
        assertThat(volumes)
                        .hasOnlyOneElementSatisfying(v -> {
                            assertThat(v.getName()).isEqualTo(volumeMounts.get(0).getName());
                            assertThat(v.getPersistentVolumeClaim().getClaimName()).contains("-postgresql-claim");
                        });
        assertThat(builtPostgreSqlDeployment.getPersistentVolumeClaims())
                        .hasOnlyOneElementSatisfying(p -> {
                            assertThat(p.getMetadata().getName()).isEqualTo(volumes.get(0).getPersistentVolumeClaim().getClaimName());
                            assertThat(p.getSpec().getAccessModes()).containsOnlyOnce("ReadWriteOnce");
                            assertThat(p.getSpec().getResources().getRequests().get("storage")).isEqualTo(new Quantity("1Gi"));
                        });
    }
}
