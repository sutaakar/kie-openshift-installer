/*
 * Copyright 2017 JBoss by Red Hat.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kie.cloud.openshift.settings.builder;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerPort;
import io.fabric8.kubernetes.api.model.ContainerPortBuilder;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.Probe;
import io.fabric8.kubernetes.api.model.ProbeBuilder;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.kubernetes.api.model.ServicePortBuilder;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.fabric8.openshift.api.model.DeploymentConfig;
import io.fabric8.openshift.api.model.DeploymentConfigBuilder;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.api.model.RouteBuilder;
import io.fabric8.openshift.api.model.Template;
import org.kie.cloud.openshift.OpenShiftImageConstants;
import org.kie.cloud.openshift.deployment.Deployment;

/**
 * Cloud settings builder for Kie Server.
 *
 * If any environment variable isn't configured by SettingsBuilder, then default
 * value from application template is used.
 */
public class KieServerDeploymentBuilder extends AbstractDeploymentBuilder {

    public KieServerDeploymentBuilder(Template kieServerTemplate) {
        super(kieServerTemplate, "kieserver");
    }

    @Override
    protected void initDefaultValues() {
        EnvVar kieServerHost = new EnvVarBuilder().withName(OpenShiftImageConstants.KIE_SERVER_HOST)
                                                  .withNewValueFrom()
                                                      .withNewFieldRef()
                                                          .withFieldPath("status.podIP")
                                                      .endFieldRef()
                                                  .endValueFrom()
                                                  .build();
        // TODO this should be deleted
        addOrReplaceEnvVar(kieServerHost);
        addOrReplaceEnvVar(OpenShiftImageConstants.KIE_SERVER_USER, "executionUser");
        addOrReplaceEnvVar(OpenShiftImageConstants.KIE_SERVER_PWD, "executionUser1!");
    }

    @Override
    protected void configureDeploymentConfig() {
        DeploymentConfig deploymentConfig = new DeploymentConfigBuilder().withApiVersion("v1")
                                                                         .withNewMetadata()
                                                                             .withName(getDeployment().getDeploymentName())
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
                                                                                         .withNamespace("${IMAGE_STREAM_NAMESPACE}")
                                                                                         .withName("rhpam70-kieserver-openshift:${IMAGE_STREAM_TAG}")
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
                                                                                     .withLabels(Collections.singletonMap("deploymentConfig", getDeployment().getDeploymentName()))
                                                                                 .endMetadata()
                                                                                 .withNewSpec()
                                                                                     .withTerminationGracePeriodSeconds(60L)
                                                                                     .addNewContainer()
                                                                                         .withName(getDeployment().getDeploymentName())
                                                                                         .withImage("rhpam70-kieserver-openshift")
                                                                                         .withImagePullPolicy("Always")
                                                                                         .withNewResources()
                                                                                             .withLimits(Collections.singletonMap("memory", new Quantity("${EXCECUTION_SERVER_MEMORY_LIMIT}")))
                                                                                         .endResources()
                                                                                         .addNewPort()
                                                                                             .withName("jolokia")
                                                                                             .withContainerPort(8778)
                                                                                             .withProtocol("TCP")
                                                                                         .endPort()
                                                                                         .addNewPort()
                                                                                             .withName("http")
                                                                                             .withContainerPort(8080)
                                                                                             .withProtocol("TCP")
                                                                                         .endPort()
                                                                                     .endContainer()
                                                                                 .endSpec()
                                                                             .endTemplate()
                                                                         .endSpec()
                                                                         .build();

        List<HasMetadata> objects = getDeployment().getTemplate().getObjects();
        objects.add(deploymentConfig);
        getDeployment().getTemplate().setObjects(objects);
    }

    @Override
    protected void configureService() {
        ServicePort httpPort = new ServicePortBuilder().withName("http")
                                                       .withPort(8080)
                                                       .withNewTargetPortLike(new IntOrString(8080, null, null, new HashMap<String, Object>()))
                                                       .endTargetPort()
                                                       .build();
        Service service = new ServiceBuilder().withApiVersion("v1")
                                              .withNewMetadata()
                                                  .withName(getDeployment().getDeploymentName())
                                              .endMetadata()
                                              .withNewSpec()
                                                  .withPorts(httpPort)
                                                  .withSelector(Collections.singletonMap("deploymentConfig", getDeployment().getDeploymentName()))
                                              .endSpec()
                                              .build();
        List<HasMetadata> objects = getDeployment().getTemplate().getObjects();
        objects.add(service);
        getDeployment().getTemplate().setObjects(objects);
    }

    @Override
    protected void configureRoute() {
        Route route = new RouteBuilder().withApiVersion("v1")
                                        .withNewMetadata()
                                            .withName(getDeployment().getDeploymentName())
                                        .endMetadata()
                                        .withNewSpec()
                                            .withNewTo()
                                                .withName(getDeployment().getDeploymentName())
                                            .endTo()
                                            .withNewPort()
                                                .withNewTargetPort("http")
                                            .endPort()
                                        .endSpec()
                                        .build();
        List<HasMetadata> objects = getDeployment().getTemplate().getObjects();
        objects.add(route);
        getDeployment().getTemplate().setObjects(objects);
    }

    @Override
    protected void configureLivenessProbe() {
        String kieServerUser = getDeployment().getEnvironmentVariableValue(OpenShiftImageConstants.KIE_SERVER_USER);
        String kieServerPassword = getDeployment().getEnvironmentVariableValue(OpenShiftImageConstants.KIE_SERVER_PWD);
        Probe livenessProbe = new ProbeBuilder().withNewExec()
                                                    .withCommand("/bin/bash", "-c", "curl --fail --silent -u '" + kieServerUser + ":" + kieServerPassword + "' http://localhost:8080/services/rest/server/healthcheck")
                                                .endExec()
                                                .withInitialDelaySeconds(180)
                                                .withTimeoutSeconds(2)
                                                .withPeriodSeconds(15)
                                                .withFailureThreshold(3)
                                                .build();
        // Just one container should be available
        Container container = getDeployment().getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0);
        container.setLivenessProbe(livenessProbe);
    }

    @Override
    protected void configureReadinessProbe() {
        String kieServerUser = getDeployment().getEnvironmentVariableValue(OpenShiftImageConstants.KIE_SERVER_USER);
        String kieServerPassword = getDeployment().getEnvironmentVariableValue(OpenShiftImageConstants.KIE_SERVER_PWD);
        Probe readinessProbe = new ProbeBuilder().withNewExec()
                                                     .withCommand("/bin/bash", "-c", "curl --fail --silent -u '" + kieServerUser + ":" + kieServerPassword + "' http://localhost:8080/services/rest/server/readycheck")
                                                 .endExec()
                                                 .withInitialDelaySeconds(60)
                                                 .withTimeoutSeconds(2)
                                                 .withPeriodSeconds(30)
                                                 .withFailureThreshold(6)
                                                 .build();
        // Just one container should be available
        Container container = getDeployment().getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0);
        container.setReadinessProbe(readinessProbe);
    }

    /**
     * Return configured builder with Kie Server user.
     *
     * @param kieServerUser Kie Server username.
     * @param kieServerPwd Kie Server password.
     * @return Builder
     */
    public KieServerDeploymentBuilder withKieServerUser(String kieServerUser, String kieServerPwd) {
        addOrReplaceEnvVar(OpenShiftImageConstants.KIE_SERVER_USER, kieServerUser);
        addOrReplaceEnvVar(OpenShiftImageConstants.KIE_SERVER_PWD, kieServerPwd);
        return this;
    }

    public KieServerDeploymentBuilder withHttps(String secretName, String keystore, String name, String password) {
        String volumeName = "kieserver-keystore-volume";
        String volumeDir = "/etc/kieserver-secret-volume";

        // Create route
        Route httpsRoute = new RouteBuilder().withApiVersion("v1")
                                             .withNewMetadata()
                                                 .withName("secure-" + getDeployment().getDeploymentName())
                                             .endMetadata()
                                             .withNewSpec()
                                                 .withNewTo()
                                                     .withName(getDeployment().getDeploymentName())
                                                 .endTo()
                                                 .withNewPort()
                                                     .withNewTargetPort("https")
                                                 .endPort()
                                                 .withNewTls()
                                                     .withTermination("passthrough")
                                                 .endTls()
                                             .endSpec()
                                             .build();
        List<HasMetadata> objects = getDeployment().getTemplate().getObjects();
        objects.add(httpsRoute);
        getDeployment().getTemplate().setObjects(objects);

        // Adjust service ports
        ServicePort httpsServicePort = new ServicePortBuilder().withName("https")
                                                               .withPort(8443)
                                                               .withNewTargetPortLike(new IntOrString(8443, null, null, new HashMap<String, Object>()))
                                                               .endTargetPort()
                                                               .build();
        getDeployment().getServices().get(0).getSpec().getPorts().add(httpsServicePort);

        // Create volumes
        PodSpec podSpec = getDeployment().getDeploymentConfig().getSpec().getTemplate().getSpec();
        Container container = podSpec.getContainers().get(0);
        VolumeMount secretVolumeMount = new VolumeMountBuilder().withName(volumeName)
                                                               .withMountPath(volumeDir)
                                                               .withReadOnly(Boolean.TRUE)
                                                               .build();
        container.getVolumeMounts().add(secretVolumeMount);

        Volume volume = new VolumeBuilder().withName(volumeName)
                                                .withNewSecret()
                                                    .withSecretName(secretName)
                                                .endSecret()
                                                .build();
        podSpec.getVolumes().add(volume);

        // Set container port
        ContainerPort httpsContainerPort = new ContainerPortBuilder().withName("https")
                                                                     .withContainerPort(8443)
                                                                     .withProtocol("TCP")
                                                                     .build();
        container.getPorts().add(httpsContainerPort);

        // Configure environment variables
        addOrReplaceEnvVar(OpenShiftImageConstants.HTTPS_KEYSTORE_DIR, volumeDir);
        addOrReplaceEnvVar(OpenShiftImageConstants.HTTPS_KEYSTORE, keystore);
        addOrReplaceEnvVar(OpenShiftImageConstants.HTTPS_NAME, name);
        addOrReplaceEnvVar(OpenShiftImageConstants.HTTPS_PASSWORD, password);
        return this;
    }

    public KieServerDeploymentBuilder connectToMySqlDatabase(Deployment databaseDeployment) {
        addOrReplaceEnvVar(OpenShiftImageConstants.KIE_SERVER_PERSISTENCE_DIALECT, "org.hibernate.dialect.MySQL5Dialect");
        addOrReplaceEnvVar(OpenShiftImageConstants.KIE_SERVER_PERSISTENCE_DS, "java:/jboss/datasources/kie");
        addOrReplaceEnvVar(OpenShiftImageConstants.DATASOURCES, OpenShiftImageConstants.DATASOURCES_KIE);
        addOrReplaceEnvVar(OpenShiftImageConstants.KIE_DATABASE, databaseDeployment.getEnvironmentVariableValue(OpenShiftImageConstants.MYSQL_DATABASE));
        addOrReplaceEnvVar(OpenShiftImageConstants.KIE_JNDI, "java:/jboss/datasources/kie");
        addOrReplaceEnvVar(OpenShiftImageConstants.KIE_DRIVER, "mysql");
        addOrReplaceEnvVar(OpenShiftImageConstants.KIE_JTA, "true");
        addOrReplaceEnvVar(OpenShiftImageConstants.KIE_USERNAME, databaseDeployment.getEnvironmentVariableValue(OpenShiftImageConstants.MYSQL_USER));
        addOrReplaceEnvVar(OpenShiftImageConstants.KIE_PASSWORD, databaseDeployment.getEnvironmentVariableValue(OpenShiftImageConstants.MYSQL_PASSWORD));
        // Set to first service
        addOrReplaceEnvVar(OpenShiftImageConstants.KIE_SERVICE_HOST, databaseDeployment.getServices().get(0).getMetadata().getName());
        addOrReplaceEnvVar(OpenShiftImageConstants.KIE_SERVICE_PORT, "3306");
        // Same as service host
        addOrReplaceEnvVar(OpenShiftImageConstants.TIMER_SERVICE_DATA_STORE, databaseDeployment.getServices().get(0).getMetadata().getName());
        addOrReplaceEnvVar(OpenShiftImageConstants.TIMER_SERVICE_DATA_STORE_REFRESH_INTERVAL, "30000");
        return this;
    }

    public KieServerDeploymentBuilder connectToPostgreSqlDatabase(Deployment databaseDeployment) {
        addOrReplaceEnvVar(OpenShiftImageConstants.KIE_SERVER_PERSISTENCE_DIALECT, "org.hibernate.dialect.PostgreSQLDialect");
        addOrReplaceEnvVar(OpenShiftImageConstants.KIE_SERVER_PERSISTENCE_DS, "java:/jboss/datasources/kie");
        addOrReplaceEnvVar(OpenShiftImageConstants.DATASOURCES, OpenShiftImageConstants.DATASOURCES_KIE);
        addOrReplaceEnvVar(OpenShiftImageConstants.KIE_DATABASE, databaseDeployment.getEnvironmentVariableValue(OpenShiftImageConstants.POSTGRESQL_DATABASE));
        addOrReplaceEnvVar(OpenShiftImageConstants.KIE_JNDI, "java:/jboss/datasources/kie");
        addOrReplaceEnvVar(OpenShiftImageConstants.KIE_DRIVER, "postgresql");
        addOrReplaceEnvVar(OpenShiftImageConstants.KIE_JTA, "true");
        addOrReplaceEnvVar(OpenShiftImageConstants.KIE_USERNAME, databaseDeployment.getEnvironmentVariableValue(OpenShiftImageConstants.POSTGRESQL_USER));
        addOrReplaceEnvVar(OpenShiftImageConstants.KIE_PASSWORD, databaseDeployment.getEnvironmentVariableValue(OpenShiftImageConstants.POSTGRESQL_PASSWORD));
        // Set to first service
        addOrReplaceEnvVar(OpenShiftImageConstants.KIE_SERVICE_HOST, databaseDeployment.getServices().get(0).getMetadata().getName());
        addOrReplaceEnvVar(OpenShiftImageConstants.KIE_SERVICE_PORT, "5432");
        // Same as service host
        addOrReplaceEnvVar(OpenShiftImageConstants.TIMER_SERVICE_DATA_STORE, databaseDeployment.getServices().get(0).getMetadata().getName());
        addOrReplaceEnvVar(OpenShiftImageConstants.TIMER_SERVICE_DATA_STORE_REFRESH_INTERVAL, "30000");
        return this;
    }
}
