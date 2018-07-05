package org.kie.cloud.openshift.settings.builder;

import java.util.Collections;
import java.util.List;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimBuilder;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.fabric8.openshift.api.model.DeploymentConfig;
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

    protected void addOrReplaceEnvVar(String environmentVariableName, String environmentVariableValue) {
        EnvVar envVar = new EnvVar(environmentVariableName, environmentVariableValue, null);
        addOrReplaceEnvVar(envVar);
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

    protected void addPersistence(String deploymentName, String mountPath, String accessMode, String persistentVolumeStorageSize) {
        for (DeploymentConfig deploymentConfig : getDeployment().getDeploymentConfigs()) {
            PodSpec podSpec = deploymentConfig.getSpec().getTemplate().getSpec();
            List<Container> containers = podSpec.getContainers();

            if (containers.size() > 1) {
                throw new RuntimeException("Corrent configuration doesn't support multiple containers in Deployment config.");
            }

            VolumeMount mySqlVolumeMount = new VolumeMountBuilder().withName(deploymentName + "-pvol")
                                                                   .withMountPath(mountPath)
                                                                   .build();
            containers.get(0).getVolumeMounts().add(mySqlVolumeMount);

            Volume volume = new VolumeBuilder().withName(deploymentName + "-pvol")
                                                    .withNewPersistentVolumeClaim()
                                                        .withClaimName(deploymentName + "-claim")
                                                    .endPersistentVolumeClaim()
                                                    .build();
            podSpec.getVolumes().add(volume);

            PersistentVolumeClaim persistentVolumeClaim = new PersistentVolumeClaimBuilder().withKind("PersistentVolumeClaim")
                                                                                                 .withNewMetadata()
                                                                                                     .withName(deploymentName + "-claim")
                                                                                                 .endMetadata()
                                                                                                 .withNewSpec()
                                                                                                     .withAccessModes(accessMode)
                                                                                                     .withNewResources()
                                                                                                         .withRequests(Collections.singletonMap("storage", new Quantity(persistentVolumeStorageSize)))
                                                                                                     .endResources()
                                                                                                 .endSpec()
                                                                                                 .build();
            List<HasMetadata> objects = getDeployment().geTemplate().getObjects();
            objects.add(persistentVolumeClaim);
            getDeployment().geTemplate().setObjects(objects);
        }
    }

    // TODO adjust to the Deployment name and filter whole object tree (or create some reference)
    protected void setApplicationName(String applicationName) {
        for (HasMetadata object : getDeployment().geTemplate().getObjects()) {
            String newObjectName = object.getMetadata().getName().replace("${APPLICATION_NAME}", applicationName);
            object.getMetadata().setName(newObjectName);
        }
        // Delete application name property
        getDeployment().geTemplate().getParameters().removeIf(p -> p.getName().equals("APPLICATION_NAME"));
    }
}
