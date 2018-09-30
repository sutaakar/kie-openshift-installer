package org.kie.cloud.openshift.integrationtests;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.fabric8.openshift.api.model.DeploymentConfig;
import org.junit.Test;
import org.kie.cloud.openshift.KieOpenShiftProvider;
import org.kie.cloud.openshift.deployment.Deployment;
import org.kie.cloud.openshift.scenario.Scenario;
import org.kie.server.api.model.KieServerInfo;
import org.kie.server.api.model.KieServiceResponse.ResponseType;
import org.kie.server.api.model.ServiceResponse;
import org.kie.server.client.KieServicesClient;
import org.kie.server.client.KieServicesFactory;

public class KieServerIntegrationTest extends AbstractCloudIntegrationTest {

    @Test
    public void testCreateAndDeployKieServerWithMySql() throws InterruptedException {
        final String kieServerUsername = "john";
        final String kieServerPassword = "john123";
        final String mySqlUsername = "mysqluser";
        final String mySqlPassword = "mysqlpass";

        Deployment mySqlDeployment = KieOpenShiftProvider.createMySqlDeploymentBuilder()
                                                         .withDatabaseUser(mySqlUsername, mySqlPassword)
                                                         .withDatabaseName("mydb")
                                                         .build();
        Deployment kieServerDeployment = KieOpenShiftProvider.createKieServerDeploymentBuilder()
                                                             .withImageStreamNamespace(projectName)
                                                             .withKieServerUser(kieServerUsername, kieServerPassword)
                                                             .connectToMySqlDatabase(mySqlDeployment)
                                                             .build();
        Scenario kieServerScenario = KieOpenShiftProvider.createScenarioBuilder()
                                                         .withDeployment(kieServerDeployment)
                                                         .withDeployment(mySqlDeployment)
                                                         .build();
        KieOpenShiftProvider.deployScenario(openShiftClient, kieServerScenario, projectName, Collections.emptyMap());

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

    @Test
    public void testCreateAndDeployKieServerWithPostgreSql() throws InterruptedException {
        final String kieServerUsername = "john";
        final String kieServerPassword = "john123";
        final String mySqlUsername = "postgresqluser";
        final String mySqlPassword = "postgresqlpass";

        Deployment postgreSqlDeployment = KieOpenShiftProvider.createPostgreSqlDeploymentBuilder()
                                                              .withDatabaseUser(mySqlUsername, mySqlPassword)
                                                              .withDatabaseName("mydb")
                                                              .build();
        Deployment kieServerDeployment = KieOpenShiftProvider.createKieServerDeploymentBuilder()
                                                             .withImageStreamNamespace(projectName)
                                                             .withKieServerUser(kieServerUsername, kieServerPassword)
                                                             .connectToPostgreSqlDatabase(postgreSqlDeployment)
                                                             .build();
        Scenario kieServerScenario = KieOpenShiftProvider.createScenarioBuilder()
                                                         .withDeployment(kieServerDeployment)
                                                         .withDeployment(postgreSqlDeployment)
                                                         .build();
        KieOpenShiftProvider.deployScenario(openShiftClient, kieServerScenario, projectName, Collections.emptyMap());

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
