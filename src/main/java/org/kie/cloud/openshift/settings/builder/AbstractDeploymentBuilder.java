package org.kie.cloud.openshift.settings.builder;

import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimBuilder;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.fabric8.openshift.api.model.DeploymentConfig;
import io.fabric8.openshift.api.model.DeploymentConfigBuilder;
import io.fabric8.openshift.api.model.DeploymentTriggerPolicy;
import io.fabric8.openshift.api.model.Parameter;
import io.fabric8.openshift.api.model.ParameterBuilder;
import org.kie.cloud.openshift.deployment.Deployment;

public abstract class AbstractDeploymentBuilder<T extends DeploymentBuilder> implements DeploymentBuilder {

    private Deployment deployment;

    protected AbstractDeploymentBuilder(String deploymentName) {
        deployment = new Deployment(deploymentName);
        configureDeploymentConfig();
        configureService();
        configureAdditionalObjects();
        initDefaultValues();
    }

    @Override
    public Deployment build() {
        configureLivenessProbe();
        configureReadinessProbe();
        return deployment;
    }

    protected Deployment getDeployment() {
        return deployment;
    }

    protected abstract void initDefaultValues();

    protected void configureDeploymentConfig() {
        DeploymentConfig deploymentConfig = new DeploymentConfigBuilder().withApiVersion("v1")
                                                                         .withNewMetadata()
                                                                             .withName(getDeployment().getDeploymentName())
                                                                             .addToAnnotations("template.alpha.openshift.io/wait-for-ready", "true")
                                                                             .addToLabels("service", getDeployment().getDeploymentName())
                                                                         .endMetadata()
                                                                         .withNewSpec()
                                                                             .withNewStrategy()
                                                                                 .withType("Recreate")
                                                                             .endStrategy()
                                                                             .addNewTrigger()
                                                                                 .withType("ImageChange")
                                                                                 .withNewImageChangeParams()
                                                                                     .withAutomatic(true)
                                                                                     .withContainerNames(getDeployment().getDeploymentName())
                                                                                     .withNewFrom()
                                                                                         .withKind("ImageStreamTag")
                                                                                         .withNamespace(getDefaultImageStreamNamespace())
                                                                                         .withName(getDefaultImageStreamName() + ":" + getDefaultImageStreamTag())
                                                                                     .endFrom()
                                                                                 .endImageChangeParams()
                                                                             .endTrigger()
                                                                             .addNewTrigger()
                                                                                 .withType("ConfigChange")
                                                                             .endTrigger()
                                                                             .withReplicas(1)
                                                                             .withSelector(Collections.singletonMap("deploymentConfig", getDeployment().getDeploymentName()))
                                                                             .withNewTemplate()
                                                                                 .withNewMetadata()
                                                                                     .withName(getDeployment().getDeploymentName())
                                                                                     .addToLabels("deploymentConfig", getDeployment().getDeploymentName())
                                                                                     .addToLabels("service", getDeployment().getDeploymentName())
                                                                                 .endMetadata()
                                                                                 .withNewSpec()
                                                                                     .withTerminationGracePeriodSeconds(60L)
                                                                                     .addNewContainer()
                                                                                         .withName(getDeployment().getDeploymentName())
                                                                                         .withImage(getDefaultImageStreamName())
                                                                                         .withImagePullPolicy("Always")
                                                                                     .endContainer()
                                                                                 .endSpec()
                                                                             .endTemplate()
                                                                         .endSpec()
                                                                         .build();

        getDeployment().getObjects().add(deploymentConfig);
    }

    protected abstract String getDefaultImageStreamNamespace();
    protected abstract String getDefaultImageStreamName();
    protected abstract String getDefaultImageStreamTag();

    protected void configureService() {
        Service service = new ServiceBuilder().withApiVersion("v1")
                                              .withNewMetadata()
                                                  .withName(getDeployment().getDeploymentName())
                                                  .addToLabels("service", getDeployment().getDeploymentName())
                                              .endMetadata()
                                              .withNewSpec()
                                                  .withSelector(Collections.singletonMap("deploymentConfig", getDeployment().getDeploymentName()))
                                              .endSpec()
                                              .build();
        getDeployment().getObjects().add(service);
    }

    protected void configureAdditionalObjects() {};

    protected abstract void configureLivenessProbe();

    protected abstract void configureReadinessProbe();

    // ***** Shared functionality *****

    @SuppressWarnings("unchecked")
    public T withImageStreamNamespace(String imageStreamNamespace) {
        getDeployment().getDeploymentConfig().getSpec().getTriggers().stream()
                                                                     .filter(t -> t.getType().equals("ImageChange"))
                                                                     .findAny()
                                                                     .ifPresent(t -> t.getImageChangeParams().getFrom().setNamespace(imageStreamNamespace));
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T withImageStreamName(String imageStreamName) {
        DeploymentTriggerPolicy deploymentTriggerPolicy = getDeployment().getDeploymentConfig().getSpec().getTriggers().stream()
                                                                                                                       .filter(t -> t.getType().equals("ImageChange"))
                                                                                                                       .findAny()
                                                                                                                       .orElseThrow(() -> new RuntimeException("No ImageChange trigger policy found, cannot set image name."));
        String name = deploymentTriggerPolicy.getImageChangeParams().getFrom().getName();
        String sanitizedImageStreamName = Matcher.quoteReplacement(imageStreamName);
        String newName = name.replaceFirst(".*:", sanitizedImageStreamName + ":");
        deploymentTriggerPolicy.getImageChangeParams().getFrom().setName(newName);

        getDeployment().getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0).setImage(imageStreamName);

        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T withImageStreamTag(String imageStreamTag) {
        DeploymentTriggerPolicy deploymentTriggerPolicy = getDeployment().getDeploymentConfig().getSpec().getTriggers().stream()
                                                                                                                       .filter(t -> t.getType().equals("ImageChange"))
                                                                                                                       .findAny()
                                                                                                                       .orElseThrow(() -> new RuntimeException("No ImageChange trigger policy found, cannot set image name."));
        String name = deploymentTriggerPolicy.getImageChangeParams().getFrom().getName();
        String sanitizedImageStreamTag = Matcher.quoteReplacement(imageStreamTag);
        String newName = name.replaceFirst(":.*", ":" + sanitizedImageStreamTag);
        deploymentTriggerPolicy.getImageChangeParams().getFrom().setName(newName);

        return (T) this;
    }

    // ***** Helper methods *****

    protected void addOrReplaceEnvVar(String environmentVariableName, String environmentVariableValue) {
        EnvVar envVar = new EnvVar(environmentVariableName, environmentVariableValue, null);
        addOrReplaceEnvVar(envVar);
    }

    protected void addOrReplaceEnvVar(EnvVar envVar) {
        List<Container> containers = deployment.getDeploymentConfig()
                                               .getSpec()
                                               .getTemplate()
                                               .getSpec()
                                               .getContainers();
        for (Container container : containers) {
            container.getEnv().removeIf(n -> n.getName().equals(envVar.getName()));
            container.getEnv().add(envVar);
        }
    }

    protected void addOrReplaceProperty(String propertyDisplayName, String propertyDescription, String propertyName, String propertyValue, boolean required) {
        Parameter parameter = new ParameterBuilder().withDisplayName(propertyDisplayName)
                                                    .withDescription(propertyDescription)
                                                    .withName(propertyName)
                                                    .withValue(propertyValue)
                                                    .withRequired(required)
                                                    .build();
        deployment.getParameters().removeIf(p -> p.getName().equals(propertyName));
        deployment.getParameters().add(parameter);
    }

    protected void addOrReplaceProperty(String propertyDisplayName, String propertyDescription, String propertyName, String propertyFrom, String propertyGenerate, boolean required) {
        Parameter parameter = new ParameterBuilder().withDisplayName(propertyDisplayName)
                                                    .withDescription(propertyDescription)
                                                    .withName(propertyName)
                                                    .withFrom(propertyFrom)
                                                    .withGenerate(propertyGenerate)
                                                    .withRequired(required)
                                                    .build();
        deployment.getParameters().removeIf(p -> p.getName().equals(propertyName));
        deployment.getParameters().add(parameter);
    }

    protected void addOrReplacePropertyWithExample(String propertyDisplayName, String propertyDescription, String propertyName, String propertyExample, boolean required) {
        Parameter parameter = new ParameterBuilder().withDisplayName(propertyDisplayName)
                                                    .withDescription(propertyDescription)
                                                    .withName(propertyName)
                                                    .withRequired(required)
                                                    .build();
        parameter.setAdditionalProperty("example", propertyExample);
        deployment.getParameters().removeIf(p -> p.getName().equals(propertyName));
        deployment.getParameters().add(parameter);
    }

    protected void addPersistence(String deploymentName, String mountPath, String accessMode, String persistentVolumeStorageSize) {
        String volumeName = deploymentName + "-pvol";
        String volumeClaimName = deploymentName + "-claim";

        PodSpec podSpec = getDeployment().getDeploymentConfig().getSpec().getTemplate().getSpec();
        List<Container> containers = podSpec.getContainers();

        if (containers.size() > 1) {
            throw new RuntimeException("Corrent configuration doesn't support multiple containers in Deployment config.");
        }

        VolumeMount mySqlVolumeMount = new VolumeMountBuilder().withName(volumeName)
                                                               .withMountPath(mountPath)
                                                               .build();
        containers.get(0).getVolumeMounts().add(mySqlVolumeMount);

        Volume volume = new VolumeBuilder().withName(volumeName)
                                                .withNewPersistentVolumeClaim()
                                                    .withClaimName(volumeClaimName)
                                                .endPersistentVolumeClaim()
                                                .build();
        podSpec.getVolumes().add(volume);

        PersistentVolumeClaim persistentVolumeClaim = new PersistentVolumeClaimBuilder().withKind("PersistentVolumeClaim")
                                                                                             .withNewMetadata()
                                                                                                 .withName(volumeClaimName)
                                                                                                 .addToLabels("service", getDeployment().getDeploymentName())
                                                                                             .endMetadata()
                                                                                             .withNewSpec()
                                                                                                 .withAccessModes(accessMode)
                                                                                                 .withNewResources()
                                                                                                     .withRequests(Collections.singletonMap("storage", new Quantity(persistentVolumeStorageSize)))
                                                                                                 .endResources()
                                                                                             .endSpec()
                                                                                             .build();
        getDeployment().getObjects().add(persistentVolumeClaim);
    }
}
