package org.kie.cloud.openshift.scenario;

import java.util.ArrayList;
import java.util.List;

import org.kie.cloud.openshift.deployment.Deployment;

public class Scenario {

    private List<Deployment> deployments = new ArrayList<>();

    public void addDeployment(Deployment deployment) {
        deployments.add(deployment);
    }

    public List<Deployment> getDeployments() {
        return deployments;
    }
}
