package org.kie.cloud.openshift.scenario.builder;

import static org.assertj.core.api.Assertions.assertThat;

import io.fabric8.openshift.api.model.DeploymentConfig;
import org.junit.Test;
import org.kie.cloud.openshift.AbstractCloudTest;
import org.kie.cloud.openshift.deployment.Deployment;
import org.kie.cloud.openshift.scenario.Scenario;
import org.kie.cloud.openshift.settings.builder.KieServerDeploymentBuilder;
import org.kie.cloud.openshift.settings.builder.MySqlDeploymentBuilder;

public class ScenarioBuilderTest extends AbstractCloudTest {

    @Test
    public void testBuildScenario() {
        Scenario scenario = new ScenarioBuilder().build();

        assertThat(scenario).isNotNull();
        assertThat(scenario.getDeployments()).isEmpty();
    }

    @Test
    public void testBuildScenarioWithDeployments() {
        ScenarioBuilder scenarioBuilder = new ScenarioBuilder();
        Deployment kieServer = new KieServerDeploymentBuilder().build();
        Deployment mySql = new MySqlDeploymentBuilder().build();

        Scenario scenario = scenarioBuilder.withDeployment(kieServer)
                                           .withDeployment(mySql)
                                           .build();

        assertThat(scenario).isNotNull();
        assertThat(scenario.getDeployments()).containsExactlyInAnyOrder(kieServer, mySql);
    }

    @Test
    public void testBuildScenarioWithDeploymentsAndApplicationName() {
        ScenarioBuilder scenarioBuilder = new ScenarioBuilder();
        Deployment kieServer = new KieServerDeploymentBuilder().build();
        Deployment mySql = new MySqlDeploymentBuilder().build();

        Scenario scenario = scenarioBuilder.withDeployment(kieServer)
                                           .withDeployment(mySql)
                                           .withApplicationName("custom-app")
                                           .build();

        assertThat(scenario).isNotNull();
        assertThat(scenario.getDeployments()).containsExactlyInAnyOrder(kieServer, mySql);
        
        assertThat(scenario.getDeployments()).allMatch(d -> d.getObjects().stream()
                                                                          .allMatch(o -> {
                                                                              return o.getMetadata().getLabels().containsKey("application") && 
                                                                                     o.getMetadata().getLabels().get("application").equals("custom-app");
                                                                          }));
        assertThat(scenario.getDeployments()).flatExtracting(d -> d.getObjects())
                                             .filteredOn(d -> d instanceof DeploymentConfig)
                                             .allMatch(o -> {
                                                 return ((DeploymentConfig)o).getSpec().getTemplate().getMetadata().getLabels().containsKey("application") &&
                                                        ((DeploymentConfig)o).getSpec().getTemplate().getMetadata().getLabels().get("application").equals("custom-app");
                                             });
    }
}
