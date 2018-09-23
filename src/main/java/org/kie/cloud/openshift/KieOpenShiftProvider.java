package org.kie.cloud.openshift;

import java.io.ByteArrayInputStream;
import java.util.Map;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.openshift.api.model.Project;
import io.fabric8.openshift.api.model.ProjectRequest;
import io.fabric8.openshift.api.model.ProjectRequestBuilder;
import io.fabric8.openshift.client.OpenShiftClient;
import org.kie.cloud.openshift.scenario.Scenario;
import org.kie.cloud.openshift.scenario.builder.ScenarioBuilder;
import org.kie.cloud.openshift.settings.builder.KieServerDeploymentBuilder;
import org.kie.cloud.openshift.settings.builder.MySqlDeploymentBuilder;
import org.kie.cloud.openshift.settings.builder.PostgreSqlDeploymentBuilder;

public class KieOpenShiftProvider {

    public static KieServerDeploymentBuilder createKieServerDeploymentBuilder() {
        return new KieServerDeploymentBuilder();
    }

    public static KieServerDeploymentBuilder createKieServerDeploymentBuilder(String deploymentName) {
        return new KieServerDeploymentBuilder(deploymentName);
    }

    public static MySqlDeploymentBuilder createMySqlDeploymentBuilder() {
        return new MySqlDeploymentBuilder();
    }

    public static MySqlDeploymentBuilder createMySqlDeploymentBuilder(String deploymentName) {
        return new MySqlDeploymentBuilder(deploymentName);
    }

    public static PostgreSqlDeploymentBuilder createPostgreSqlDeploymentBuilder() {
        return new PostgreSqlDeploymentBuilder();
    }

    public static PostgreSqlDeploymentBuilder createPostgreSqlDeploymentBuilder(String deploymentName) {
        return new PostgreSqlDeploymentBuilder(deploymentName);
    }

    public static ScenarioBuilder createScenarioBuilder() {
        return new ScenarioBuilder();
    }

    public static void deployScenario(OpenShiftClient openShiftClient, Scenario scenario, String projectName, Map<String, String> parameters) {
        createProjectIfNotExists(openShiftClient, projectName);
        deployScenarioIntoProject(openShiftClient, scenario, projectName, parameters);
    }

    private static void createProjectIfNotExists(OpenShiftClient openShiftClient, String projectName) {
        Optional<Project> foundProject = openShiftClient.projects().list().getItems().stream()
                                                                                     .filter(n -> n.getMetadata().getName().equals(projectName))
                                                                                     .findAny();
        if(!foundProject.isPresent()) {
            ProjectRequest projectRequest = new ProjectRequestBuilder().withNewMetadata().withName(projectName).endMetadata().build();
            openShiftClient.projectrequests().create(projectRequest);
        }
    }

    private static void deployScenarioIntoProject(OpenShiftClient openShiftClient, Scenario scenario, String projectName, Map<String, String> parameters) {
        String yaml = scenario.getTemplateAsYaml();
        KubernetesList resourceList = openShiftClient.templates().load(new ByteArrayInputStream(yaml.getBytes())).processLocally(parameters);
        openShiftClient.lists().inNamespace(projectName).create(resourceList);
    }
}
