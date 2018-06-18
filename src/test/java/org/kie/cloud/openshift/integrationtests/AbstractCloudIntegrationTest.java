package org.kie.cloud.openshift.integrationtests;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.UUID;

import io.fabric8.kubernetes.api.model.KubernetesList;
import org.junit.After;
import org.junit.Before;
import org.kie.cloud.openshift.AbstractCloudTest;

public class AbstractCloudIntegrationTest extends AbstractCloudTest{

    protected String projectName = "test-project-" + UUID.randomUUID().toString().substring(0, 4);

    @Before
    public void createProjectWithImageStreams() {
        try {
            URL imageStreamsUrl = new URL("https://raw.githubusercontent.com/jboss-container-images/rhpam-7-openshift-image/rhpam70-dev/rhpam70-image-streams.yaml");
            openShiftClient.projectrequests().createNew().withNewMetadata().withName(projectName).endMetadata().done();
            KubernetesList resourceList = openShiftClient.templates().inNamespace(projectName).load(imageStreamsUrl).process();
            openShiftClient.lists().inNamespace(projectName).create(resourceList);
            imageStreamsUrl = new URL("https://raw.githubusercontent.com/openshift/library/master/community/mysql/imagestreams/mysql-centos7.json");
            resourceList = openShiftClient.templates().inNamespace(projectName).load(imageStreamsUrl).process();
            openShiftClient.lists().inNamespace(projectName).create(resourceList);
        } catch (MalformedURLException e) {
            throw new RuntimeException("Error while parsing image stream URL.", e);
        }
    }

    @After
    public void deleteProject() {
        openShiftClient.projects().withName(projectName).delete();
    }
}
