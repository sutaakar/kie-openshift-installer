package org.kie.cloud.openshift;

import java.io.ByteArrayInputStream;

import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.fabric8.openshift.api.model.ProjectRequest;
import io.fabric8.openshift.api.model.ProjectRequestBuilder;
import io.fabric8.openshift.api.model.Template;
import io.fabric8.openshift.client.OpenShiftClient;
import org.kie.cloud.openshift.deployment.Deployment;
import org.kie.cloud.openshift.scenario.Scenario;
import org.kie.cloud.openshift.settings.builder.KieServerDeploymentBuilder;
import org.kie.cloud.openshift.template.TemplateLoader;

public class KieOpenShiftProvider implements AutoCloseable {

    private OpenShiftClient openShiftClient;
    private TemplateLoader templateLoader;

    public KieOpenShiftProvider(OpenShiftClient openShiftClient) {
        this.openShiftClient = openShiftClient;
        templateLoader = new TemplateLoader(openShiftClient);
    }

    public KieServerDeploymentBuilder createKieServerDeploymentBuilder() {
        Template kieServerTemplate = templateLoader.loadKieServerTemplate();
        return new KieServerDeploymentBuilder(kieServerTemplate);
    }

    public Scenario createScenario() {
        return new Scenario();
    }

    public void deployScenario(Scenario scenario, String projectName) {
        createProjectIfNotExists(projectName);
        for (Deployment deployment : scenario.getDeployments()) {
            deployDeployment(projectName, deployment);
        }
    }

    private void createProjectIfNotExists(String projectName) {
        ProjectRequest projectRequest = new ProjectRequestBuilder().withNewMetadata().withName(projectName).endMetadata().build();
        openShiftClient.projectrequests().create(projectRequest);
    }

    private void deployDeployment(String projectName, Deployment deployment) {
        Template template = deployment.geTemplate();
        String yaml = Serialization.asYaml(template);
        KubernetesList resourceList = openShiftClient.templates().load(new ByteArrayInputStream(yaml.getBytes())).processLocally();
        openShiftClient.lists().inNamespace(projectName).create(resourceList);
    }

    @Override
    public void close() {
        // Should be closed? Maybe not if passed in constructor.
        openShiftClient.close();
    }
}
