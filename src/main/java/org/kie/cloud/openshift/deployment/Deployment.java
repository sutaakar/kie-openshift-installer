package org.kie.cloud.openshift.deployment;

import java.util.List;
import java.util.stream.Collectors;

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
        return template.getObjects()
                       .stream()
                       .filter(o -> o instanceof Service)
                       .filter(o -> unsecureServiceNames.contains(o.getMetadata().getName()))
                       .map(o -> (Service) o)
                       .collect(Collectors.toList());
    }

    public List<Service> getSecureServices() {
        List<String> secureServiceNames = getSecureRoutes().stream()
                                                            .map(r -> r.getSpec().getTo().getName())
                                                            .collect(Collectors.toList());
        return template.getObjects()
                       .stream()
                       .filter(o -> o instanceof Service)
                       .filter(o -> secureServiceNames.contains(o.getMetadata().getName()))
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

    public List<DeploymentConfig> getDeploymentConfigs() {
        return template.getObjects()
                .stream()
                .filter(o -> o instanceof DeploymentConfig)
                .map(o -> (DeploymentConfig) o)
                .collect(Collectors.toList());
    }
}
