package org.kie.cloud.openshift.scenario;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import org.kie.cloud.openshift.deployment.Deployment;

import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.fabric8.openshift.api.model.ProjectRequest;
import io.fabric8.openshift.api.model.ProjectRequestBuilder;
import io.fabric8.openshift.api.model.Template;
import io.fabric8.openshift.client.OpenShiftClient;

public class Scenario {

    private OpenShiftClient openShiftClient;
    private String projectName;
    private List<Deployment> deployments = new ArrayList<>();

    public Scenario(OpenShiftClient openShiftClient, String projectName) {
        this.openShiftClient = openShiftClient;
        this.projectName = projectName;
    }

    public void addDeployment(Deployment deployment) {
        deployments.add(deployment);
    }

    public List<Deployment> getDeployments() {
        return deployments;
    }

    public void deploy() {
        createProjectIfNotExists();
        for (Deployment deployment : deployments) {
            deployDeployment(deployment);
        }
    }

    private void createProjectIfNotExists() {
        ProjectRequest projectRequest = new ProjectRequestBuilder().withNewMetadata().withName(projectName).endMetadata().build();
        openShiftClient.projectrequests().create(projectRequest);
    }

    private void deployDeployment(Deployment deployment) {
        Template template = deployment.geTemplate();
        String yaml = Serialization.asYaml(template);
        KubernetesList resourceList = openShiftClient.templates().load(new ByteArrayInputStream(yaml.getBytes())).processLocally();
        openShiftClient.lists().inNamespace(projectName).create(resourceList);
    }
}
