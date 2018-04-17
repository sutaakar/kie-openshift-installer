package org.kie.cloud.openshift;

import java.net.URL;

import org.kie.cloud.openshift.scenario.Scenario;
import org.kie.cloud.openshift.settings.builder.KieServerDeploymentBuilder;

import io.fabric8.openshift.api.model.Template;
import io.fabric8.openshift.client.OpenShiftClient;

public class KieOpenShiftProvider implements AutoCloseable {

    private OpenShiftClient openShiftClient;

    public KieOpenShiftProvider(OpenShiftClient openShiftClient) {
        this.openShiftClient = openShiftClient;
    }

    public KieServerDeploymentBuilder createKieServerDeploymentBuilder() {
        URL kieServerTemplateUrl = KieOpenShiftProvider.class.getResource("/rhpam70-kieserver.yaml");
        Template kieServerTemplate = openShiftClient.templates().load(kieServerTemplateUrl).get();
        return new KieServerDeploymentBuilder(kieServerTemplate);
    }

    public Scenario createScenario(String projectName) {
        return new Scenario(openShiftClient, projectName);
    }

    @Override
    public void close() {
        openShiftClient.close();
    }
}
