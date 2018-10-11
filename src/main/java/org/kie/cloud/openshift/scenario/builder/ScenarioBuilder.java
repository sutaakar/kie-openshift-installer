package org.kie.cloud.openshift.scenario.builder;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.openshift.api.model.DeploymentConfig;
import org.kie.cloud.openshift.deployment.Deployment;
import org.kie.cloud.openshift.scenario.Scenario;

public class ScenarioBuilder {

    private String applicationName;
    private Scenario scenario = new Scenario();

    public ScenarioBuilder withApplicationName(String applicationName) {
        this.applicationName = applicationName;
        return this;
    }

    public ScenarioBuilder withDeployment(Deployment deployment) {
        scenario.addDeployment(deployment);
        return this;
    }

    public Scenario build() {
        if (applicationName != null) {
            applyApplicationName();
        }

        return scenario;
    }

    private void applyApplicationName() {
        for (Deployment deployment : scenario.getDeployments()) {
            for (HasMetadata object : deployment.getObjects()) {
                object.getMetadata().getLabels().put("application", applicationName);

                if (object instanceof DeploymentConfig) {
                    ((DeploymentConfig) object).getSpec().getTemplate().getMetadata().getLabels().put("application", applicationName);
                }
            }
        }
    }
}
