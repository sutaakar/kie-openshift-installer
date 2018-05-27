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
    public void testBuildKieServerDeploymentApplicationName() {
        Template kieServerTemplate = new TemplateLoader(openShiftClient).loadKieServerTemplate();

        KieServerDeploymentBuilder settingsBuilder = new KieServerDeploymentBuilder(kieServerTemplate);
        Deployment builtKieServerDeployment = settingsBuilder.withApplicationName("myappname")
                                                             .build();

        assertThat(builtKieServerDeployment).isNotNull();
        assertThat(builtKieServerDeployment.geTemplate().getObjects()).extracting(n -> n.getMetadata().getName()).allMatch(s -> s.startsWith("myappname-"));
        assertThat(builtKieServerDeployment.geTemplate().getParameters()).extracting(n -> n.getName()).doesNotContain("APPLICATION_NAME");
    }

    @Test
    public void testBuildKieServerDeploymentKieServerUser() {
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
}
