package org.kie.cloud.openshift.integrationtests;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.MalformedURLException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.fabric8.openshift.api.model.DeploymentConfig;
import org.junit.Test;
import org.kie.cloud.openshift.KieOpenShiftProvider;
import org.kie.cloud.openshift.OpenShiftImageConstants;
import org.kie.cloud.openshift.deployment.Deployment;
import org.kie.cloud.openshift.scenario.Scenario;
import org.kie.server.api.model.KieServerInfo;
import org.kie.server.api.model.KieServiceResponse.ResponseType;
import org.kie.server.api.model.ServiceResponse;
import org.kie.server.client.KieServicesClient;
import org.kie.server.client.KieServicesFactory;

public class KieServerIntegrationTest extends AbstractCloudIntegrationTest {

    @Test
    public void testCreateAndDeployKieServerWithMySql() throws MalformedURLException, InterruptedException {
        final String kieServerUsername = "john";
        final String kieServerPassword = "john123";
        final String mySqlUsername = "mysqluser";
        final String mySqlPassword = "mysqlpass";

        try (KieOpenShiftProvider kieOpenShiftProvider = new KieOpenShiftProvider(openShiftClient)) {
            Deployment mySqlDeployment = kieOpenShiftProvider.createMySqlDeploymentBuilder()
                                                             .withDatabaseUser(mySqlUsername, mySqlPassword)
                                                             .withDatabaseName("mydb")
                                                             .build();
            Deployment kieServerDeployment = kieOpenShiftProvider.createKieServerDeploymentBuilder()
                                                                 .withKieServerUser(kieServerUsername, kieServerPassword)
                                                                 .connectToMySqlDatabase(mySqlDeployment)
                                                                 .build();
            Scenario kieServerScenario = kieOpenShiftProvider.createScenario();
            kieServerScenario.addDeployment(kieServerDeployment);
            kieServerScenario.addDeployment(mySqlDeployment);
            kieOpenShiftProvider.deployScenario(kieServerScenario, projectName, Collections.singletonMap(OpenShiftImageConstants.IMAGE_STREAM_NAMESPACE, projectName));

            // Wait until ready
            List<DeploymentConfig> items = openShiftClient.deploymentConfigs().inNamespace(projectName).list().getItems();
            openShiftClient.deploymentConfigs().inNamespace(projectName).withName(items.get(0).getMetadata().getName()).waitUntilReady(5, TimeUnit.MINUTES);
            OpenShiftSynchronizer.waitUntilAllRoutesAreAvailable(openShiftClient, projectName);

            String host = openShiftClient.routes().inNamespace(projectName).list().getItems().get(0).getSpec().getHost();
            KieServicesClient kieServerClient = KieServicesFactory.newKieServicesRestClient("http://" + host + "/services/rest/server", kieServerUsername, kieServerPassword);

            ServiceResponse<KieServerInfo> serverInfo = kieServerClient.getServerInfo();
            assertThat(serverInfo).isNotNull();
            assertThat(serverInfo.getType()).isEqualTo(ResponseType.SUCCESS);
            assertThat(serverInfo.getResult().getCapabilities()).contains("BPM");
        }
    }
}
