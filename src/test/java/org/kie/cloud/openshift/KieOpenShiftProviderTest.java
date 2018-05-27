package org.kie.cloud.openshift;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.InputStream;

import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.client.dsl.KubernetesListMixedOperation;
import io.fabric8.openshift.api.model.ProjectRequest;
import io.fabric8.openshift.api.model.Template;
import io.fabric8.openshift.client.OpenShiftClient;
import io.fabric8.openshift.client.dsl.ProjectRequestOperation;
import io.fabric8.openshift.client.dsl.TemplateOperation;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kie.cloud.openshift.deployment.Deployment;
import org.kie.cloud.openshift.scenario.Scenario;
import org.kie.cloud.openshift.settings.builder.KieServerDeploymentBuilder;
import org.kie.cloud.openshift.settings.builder.MySqlDeploymentBuilder;
import org.kie.cloud.openshift.template.TemplateLoader;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class KieOpenShiftProviderTest extends AbstractCloudTest{

    @Test
    public void testCreateKieServerDeploymentBuilder() {
        try (KieOpenShiftProvider kieOpenShiftProvider = new KieOpenShiftProvider(openShiftClient)) {
            KieServerDeploymentBuilder kieServerSettingsBuilder = kieOpenShiftProvider.createKieServerDeploymentBuilder();
            assertThat(kieServerSettingsBuilder).isNotNull();
        }
    }

    @Test
    public void testCreateMySqlDeploymentBuilder() {
        try (KieOpenShiftProvider kieOpenShiftProvider = new KieOpenShiftProvider(openShiftClient)) {
            MySqlDeploymentBuilder mySqlSettingsBuilder = kieOpenShiftProvider.createMySqlDeploymentBuilder();
            assertThat(mySqlSettingsBuilder).isNotNull();
        }
    }

    @Test
    public void testCreateScenario() {
        try (KieOpenShiftProvider kieOpenShiftProvider = new KieOpenShiftProvider(openShiftClient)) {
            Scenario scenario = kieOpenShiftProvider.createScenario();
            assertThat(scenario).isNotNull();
        }
    }

    // Make it IT
    @Test
    @Ignore("Unignore when converted to IT test")
    public void testDeployEmptyScenario() {
        OpenShiftClient client = Mockito.mock(OpenShiftClient.class);
        ProjectRequestOperation projectRequest = Mockito.mock(ProjectRequestOperation.class);
        when(client.projectrequests()).thenReturn(projectRequest);

        try (KieOpenShiftProvider kieOpenShiftProvider = new KieOpenShiftProvider(client)) {
            Scenario scenario = kieOpenShiftProvider.createScenario();
            kieOpenShiftProvider.deployScenario(scenario, "test-project");

            ArgumentCaptor<ProjectRequest> projectRequestCaptor = ArgumentCaptor.forClass(ProjectRequest.class);
            verify(projectRequest).create(projectRequestCaptor.capture());
            assertThat(projectRequestCaptor.getValue().getMetadata().getName()).isEqualTo("test-project");
        }
    }

    // Make it IT
    @Test
    public void testDeployScenarioWithDeployment() {
        OpenShiftClient client = Mockito.mock(OpenShiftClient.class);
        ProjectRequestOperation projectRequest = Mockito.mock(ProjectRequestOperation.class);
        TemplateOperation templates = Mockito.mock(TemplateOperation.class);
        KubernetesList kubernetesList = Mockito.mock(KubernetesList.class);
        KubernetesListMixedOperation kubernetesListMixedOperation = Mockito.mock(KubernetesListMixedOperation.class);

        when(client.projectrequests()).thenReturn(projectRequest);
        when(client.templates()).thenReturn(templates);
        when(client.lists()).thenReturn(kubernetesListMixedOperation);
        when(templates.load(any(InputStream.class))).thenReturn(templates);
        when(templates.processLocally()).thenReturn(kubernetesList);
        when(kubernetesListMixedOperation.inNamespace(any())).thenReturn(kubernetesListMixedOperation);
        when(kubernetesListMixedOperation.create(any())).thenReturn(kubernetesList);

        try (KieOpenShiftProvider kieOpenShiftProvider = new KieOpenShiftProvider(client)) {
            Template kieServerTemplate = new TemplateLoader(openShiftClient).loadKieServerTemplate();
            Deployment kieServerDeployment = new Deployment(kieServerTemplate);

            Scenario scenario = kieOpenShiftProvider.createScenario();
            scenario.addDeployment(kieServerDeployment);
            kieOpenShiftProvider.deployScenario(scenario, "test-project");

            ArgumentCaptor<ProjectRequest> projectRequestCaptor = ArgumentCaptor.forClass(ProjectRequest.class);
            verify(projectRequest).create(projectRequestCaptor.capture());
            assertThat(projectRequestCaptor.getValue().getMetadata().getName()).isEqualTo("test-project");

            verify(kubernetesListMixedOperation).inNamespace("test-project");
            verify(kubernetesListMixedOperation).create(any());
        }
    }
}
