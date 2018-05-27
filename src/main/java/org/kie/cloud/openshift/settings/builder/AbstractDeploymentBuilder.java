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

    protected void setApplicationName(String applicationName) {
        for (HasMetadata object : getDeployment().geTemplate().getObjects()) {
            String newObjectName = object.getMetadata().getName().replace("${APPLICATION_NAME}", applicationName);
            object.getMetadata().setName(newObjectName);
        }
        // Delete application name property
        List<Parameter> parametersWithoutApplicationName = getDeployment().geTemplate().getParameters().stream()
                                                                                       .filter(p -> !p.getName().equals("APPLICATION_NAME"))
                                                                                       .collect(Collectors.toList());
        getDeployment().geTemplate().setParameters(parametersWithoutApplicationName);
    }
}
