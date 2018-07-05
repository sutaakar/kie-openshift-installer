package org.kie.cloud.openshift;

import java.io.ByteArrayInputStream;
import java.util.Map;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.openshift.api.model.Project;
import io.fabric8.openshift.api.model.ProjectRequest;
import io.fabric8.openshift.api.model.ProjectRequestBuilder;
import io.fabric8.openshift.api.model.Template;
import io.fabric8.openshift.client.OpenShiftClient;
import org.kie.cloud.openshift.scenario.Scenario;
import org.kie.cloud.openshift.settings.builder.KieServerDeploymentBuilder;
import org.kie.cloud.openshift.settings.builder.MySqlDeploymentBuilder;
import org.kie.cloud.openshift.settings.builder.PostgreSqlDeploymentBuilder;
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

    public MySqlDeploymentBuilder createMySqlDeploymentBuilder() {
        Template mySqlTemplate = templateLoader.loadMySqlTemplate();
        return new MySqlDeploymentBuilder(mySqlTemplate);
    }

    public PostgreSqlDeploymentBuilder createPostgreSqlDeploymentBuilder() {
        Template postgreSqlTemplate = templateLoader.loadPostgreSqlTemplate();
        return new PostgreSqlDeploymentBuilder(postgreSqlTemplate);
    }

    public Scenario createScenario() {
        return new Scenario();
    }

    public void deployScenario(Scenario scenario, String projectName, Map<String, String> parameters) {
        createProjectIfNotExists(projectName);
        deployScenarioIntoProject(scenario, projectName, parameters);
    }

    private void createProjectIfNotExists(String projectName) {
        Optional<Project> foundProject = openShiftClient.projects().list().getItems().stream()
                                                                                     .filter(n -> n.getMetadata().getName().equals(projectName))
                                                                                     .findAny();
        if(!foundProject.isPresent()) {
            ProjectRequest projectRequest = new ProjectRequestBuilder().withNewMetadata().withName(projectName).endMetadata().build();
            openShiftClient.projectrequests().create(projectRequest);
        }
    }

    private void deployScenarioIntoProject(Scenario scenario, String projectName, Map<String, String> parameters) {
        String yaml = scenario.getTemplateAsYaml();
        KubernetesList resourceList = openShiftClient.templates().load(new ByteArrayInputStream(yaml.getBytes())).processLocally(parameters);
        openShiftClient.lists().inNamespace(projectName).create(resourceList);
    }

    @Override
    public void close() {
        // Should be closed? Maybe not if passed in constructor.
        openShiftClient.close();
    }
}
