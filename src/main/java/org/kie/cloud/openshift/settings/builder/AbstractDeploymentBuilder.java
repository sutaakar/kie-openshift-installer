package org.kie.cloud.openshift.settings.builder;

import java.util.List;
import java.util.stream.Collectors;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.openshift.api.model.DeploymentConfig;
import io.fabric8.openshift.api.model.Parameter;
import io.fabric8.openshift.api.model.Template;
import org.kie.cloud.openshift.deployment.Deployment;

public abstract class AbstractDeploymentBuilder implements DeploymentBuilder {

    private Deployment deployment;

    public AbstractDeploymentBuilder(Template deploymentTemplate) {
        deployment = new Deployment(deploymentTemplate);
    }

    @Override
    public Deployment build() {
        return deployment;
    }

    protected Deployment getDeployment() {
        return deployment;
    }

    protected void addOrReplaceEnvVar(EnvVar envVar) {
        for (DeploymentConfig deploymentConfig : deployment.getDeploymentConfigs()) {
            List<Container> containers = deploymentConfig.getSpec()
                                                         .getTemplate()
                                                         .getSpec()
                                                         .getContainers();
            for (Container container : containers) {
                container.getEnv().removeIf(n -> n.getName().equals(envVar.getName()));
                container.getEnv().add(envVar);
            }
        }
    }

    //TODO make it util class
    protected String getEnvVarValue(Deployment deployment, String envVarName) {
        return deployment.getDeploymentConfigs().stream()
                                                .flatMap(n -> n.getSpec().getTemplate().getSpec().getContainers().stream())
                                                .flatMap(c -> c.getEnv().stream())
                                                .filter(e -> e.getName().equals(envVarName))
                                                .map(e -> e.getValue())
                                                .findFirst()
                                                .orElseThrow(() -> new RuntimeException("Environment variable with name " + envVarName + " not found."));
    }

    protected void setApplicationName(String applicationName) {
        for (HasMetadata object : getDeployment().geTemplate().getObjects()) {
            String newObjectName = object.getMetadata().getName().replace("${APPLICATION_NAME}", applicationName);
            object.getMetadata().setName(newObjectName);
        }
        // Delete application name property
        getDeployment().geTemplate().getParameters().removeIf(p -> p.getName().equals("APPLICATION_NAME"));
    }
}
