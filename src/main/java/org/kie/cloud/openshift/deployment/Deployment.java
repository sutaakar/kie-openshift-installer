package org.kie.cloud.openshift.deployment;

import java.util.List;
import java.util.stream.Collectors;

import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.openshift.api.model.DeploymentConfig;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.api.model.Template;

public class Deployment {

    private Template template;

    public Deployment(Template template) {
        this.template = template;
    }

    public Template geTemplate() {
        return template;
    }

    public List<Service> getUnsecureServices() {
        List<String> unsecureServiceNames = getUnsecureRoutes().stream()
                                                               .map(r -> r.getSpec().getTo().getName())
                                                               .collect(Collectors.toList());
        return getServices().stream()
                            .filter(o -> unsecureServiceNames.contains(o.getMetadata().getName()))
                            .collect(Collectors.toList());
    }

    public List<Service> getSecureServices() {
        List<String> secureServiceNames = getSecureRoutes().stream()
                                                           .map(r -> r.getSpec().getTo().getName())
                                                           .collect(Collectors.toList());
        return getServices().stream()
                            .filter(o -> secureServiceNames.contains(o.getMetadata().getName()))
                            .collect(Collectors.toList());
    }

    public List<Service> getServices() {
        return template.getObjects()
                       .stream()
                       .filter(o -> o instanceof Service)
                       .map(o -> (Service) o)
                       .collect(Collectors.toList());
    }

    public List<Route> getUnsecureRoutes() {
        return template.getObjects()
                       .stream()
                       .filter(o -> o instanceof Route)
                       .map(o -> (Route) o)
                       .filter(r -> r.getSpec().getTls() == null)
                       .collect(Collectors.toList());
    }

    public List<Route> getSecureRoutes() {
        return template.getObjects()
                       .stream()
                       .filter(o -> o instanceof Route)
                       .map(o -> (Route) o)
                       .filter(r -> r.getSpec().getTls() != null)
                       .collect(Collectors.toList());
    }

    public DeploymentConfig getDeploymentConfig() {
        return template.getObjects()
                       .stream()
                       .filter(o -> o instanceof DeploymentConfig)
                       .map(o -> (DeploymentConfig) o)
                       .findAny()
                       .orElseThrow(() -> new RuntimeException("No Deployment config found."));
    }

    public List<PersistentVolumeClaim> getPersistentVolumeClaims() {
        return template.getObjects()
                       .stream()
                       .filter(o -> o instanceof PersistentVolumeClaim)
                       .map(o -> (PersistentVolumeClaim) o)
                       .collect(Collectors.toList());
    }

    public String getEnvironmentVariableValue(String environmentVariableName) {
        return getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().stream()
                                     .flatMap(c -> c.getEnv().stream())
                                     .filter(e -> e.getName().equals(environmentVariableName))
                                     .map(e -> e.getValue())
                                     .findFirst()
                                     .orElseThrow(() -> new RuntimeException("Environment variable with name " + environmentVariableName + " not found."));
    }
}
