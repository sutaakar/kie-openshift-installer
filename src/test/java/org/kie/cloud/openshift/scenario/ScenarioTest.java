package org.kie.cloud.openshift.scenario;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import io.fabric8.openshift.api.model.DeploymentConfig;
import io.fabric8.openshift.api.model.Parameter;
import io.fabric8.openshift.api.model.ParameterBuilder;
import io.fabric8.openshift.api.model.Template;
import org.junit.Test;
import org.kie.cloud.openshift.AbstractCloudTest;
import org.kie.cloud.openshift.deployment.Deployment;
import org.kie.cloud.openshift.settings.builder.KieServerDeploymentBuilder;
import org.kie.cloud.openshift.settings.builder.MySqlDeploymentBuilder;

public class ScenarioTest extends AbstractCloudTest{

    @Test
    public void testGetScenarioAsYamlTemplate() {
        KieServerDeploymentBuilder kieServersettingsBuilder = new KieServerDeploymentBuilder();
        MySqlDeploymentBuilder mySqlSettingsBuilder = new MySqlDeploymentBuilder();

        Deployment kieServerDeployment = kieServersettingsBuilder.build();
        Deployment mySqlDeployment = mySqlSettingsBuilder.build();

        Scenario scenario = new Scenario();
        scenario.addDeployment(kieServerDeployment);
        scenario.addDeployment(mySqlDeployment);

        String yamlTemplate = scenario.getTemplateAsYaml();
        assertThat(yamlTemplate).isNotNull();

        Template template = openShiftClient.templates().load(new ByteArrayInputStream(yamlTemplate.getBytes())).get();
        assertThat(template.getMetadata().getName()).contains("custom-template");

        List<DeploymentConfig> deploymentConfigs = template.getObjects()
                                                           .stream()
                                                           .filter(o -> o instanceof DeploymentConfig)
                                                           .map(o -> (DeploymentConfig) o)
                                                           .collect(Collectors.toList());
        assertThat(deploymentConfigs).hasSize(2);
        assertThat(deploymentConfigs).extracting(n -> n.getMetadata().getName()).contains(kieServerDeployment.getDeploymentName(), mySqlDeployment.getDeploymentName());
    }

    @Test
    public void testGetScenarioAsYamlTemplateDuplicatedProperties() {
        Parameter templateParameter = new ParameterBuilder().withName("custom-name").withValue("custom-value").build();
        Parameter duplicatedTemplateParameter = new ParameterBuilder().withName("custom-name").withValue("custom-value2").build();

        Deployment customDeployment = new Deployment("custom");
        customDeployment.getParameters().addAll(Arrays.asList(templateParameter, duplicatedTemplateParameter));

        Scenario customScenario = new Scenario();
        customScenario.addDeployment(customDeployment);

        String yamlTemplate = customScenario.getTemplateAsYaml();
        assertThat(yamlTemplate).isNotNull();

        Template template = openShiftClient.templates().load(new ByteArrayInputStream(yamlTemplate.getBytes())).get();
        assertThat(template.getParameters()).hasSize(1);
        assertThat(template.getParameters().get(0).getName()).isEqualTo("custom-name");
        assertThat(template.getParameters().get(0).getValue()).isEqualTo("custom-value");
    }
}
