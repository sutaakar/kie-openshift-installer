package org.kie.cloud.openshift.scenario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.kie.cloud.openshift.AbstractCloudTest;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import io.fabric8.openshift.api.model.ProjectRequest;
import io.fabric8.openshift.client.OpenShiftClient;
import io.fabric8.openshift.client.dsl.ProjectRequestOperation;

@RunWith(MockitoJUnitRunner.class)
public class ScenarioTest extends AbstractCloudTest {

    @Test
    public void testDeployEmptyScenario() {
        OpenShiftClient client = Mockito.mock(OpenShiftClient.class);
        ProjectRequestOperation projectRequest = Mockito.mock(ProjectRequestOperation.class);
        when(client.projectrequests()).thenReturn(projectRequest);

        Scenario scenario = new Scenario(client, "test-project");
        scenario.deploy();

        ArgumentCaptor<ProjectRequest> projectRequestCaptor = ArgumentCaptor.forClass(ProjectRequest.class);
        verify(projectRequest).create(projectRequestCaptor.capture());
        assertThat(projectRequestCaptor.getValue().getMetadata().getName()).isEqualTo("test-project");
    }
}
