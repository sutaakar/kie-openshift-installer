package org.kie.cloud.openshift.settings.builder;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.kie.cloud.openshift.AbstractCloudTest;
import org.kie.cloud.openshift.KieOpenShiftProvider;
import org.kie.cloud.openshift.OpenShiftImageConstants;
import org.kie.cloud.openshift.deployment.Deployment;

import io.fabric8.openshift.api.model.Template;
import io.fabric8.openshift.client.OpenShiftClient;

public class KieServerDeploymentBuilderTest extends AbstractCloudTest{

    @Test
    public void testBuildKieServerDeployment() {
        try (OpenShiftClient client = getOfflineOpenShiftClient()) {
            Template kieServerTemplate = client.templates().load(KieOpenShiftProvider.class.getResource("/rhpam70-kieserver.yaml")).get();

            KieServerDeploymentBuilder settingsBuilder = new KieServerDeploymentBuilder(kieServerTemplate);
            Deployment builtKieServerDeployment = settingsBuilder.build();

            assertThat(builtKieServerDeployment).isNotNull();
            assertThat(builtKieServerDeployment.geTemplate().getMetadata().getName()).contains("-kieserver");
        }
    }

    @Test
    public void testBuildKieServerDeploymentKieServerUser() {
        try (OpenShiftClient client = getOfflineOpenShiftClient()) {
            Template kieServerTemplate = client.templates().load(KieOpenShiftProvider.class.getResource("/rhpam70-kieserver.yaml")).get();

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
    }
}
