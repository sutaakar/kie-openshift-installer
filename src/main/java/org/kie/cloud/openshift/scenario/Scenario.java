package org.kie.cloud.openshift.scenario;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.fabric8.openshift.api.model.Parameter;
import io.fabric8.openshift.api.model.Template;
import io.fabric8.openshift.api.model.TemplateBuilder;
import org.kie.cloud.openshift.deployment.Deployment;

public class Scenario {

    private List<Deployment> deployments = new ArrayList<>();

    public void addDeployment(Deployment deployment) {
        deployments.add(deployment);
    }

    public List<Deployment> getDeployments() {
        return deployments;
    }

    public String getTemplateAsYaml() {
        return Serialization.asYaml(getTemplate());
    }

    private Template getTemplate() {
        List<HasMetadata> objects = new ArrayList<>();
        Map<String, Parameter> parameters = new HashMap<>();

        for (Deployment deployment : deployments) {
            objects.addAll(deployment.geTemplate().getObjects());
            for (Parameter parameter : deployment.geTemplate().getParameters()) {
                parameters.putIfAbsent(parameter.getName(), parameter);
            }
        }

        ObjectMeta metadata = new ObjectMetaBuilder().withName("custom-template").build();
        return new TemplateBuilder().withApiVersion("v1")
                                    .withKind("Template")
                                    .withMetadata(metadata)
                                    .withObjects(objects)
                                    .withParameters(new ArrayList<>(parameters.values()))
                                    .build();
    }
}
