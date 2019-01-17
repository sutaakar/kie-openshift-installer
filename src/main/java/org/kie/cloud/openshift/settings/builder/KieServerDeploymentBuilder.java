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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerPort;
import io.fabric8.kubernetes.api.model.ContainerPortBuilder;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.Probe;
import io.fabric8.kubernetes.api.model.ProbeBuilder;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import io.fabric8.kubernetes.api.model.ResourceRequirementsBuilder;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.fabric8.kubernetes.api.model.ServiceAccountBuilder;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.kubernetes.api.model.ServicePortBuilder;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.fabric8.openshift.api.model.RoleBinding;
import io.fabric8.openshift.api.model.RoleBindingBuilder;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.api.model.RouteBuilder;
import org.kie.cloud.openshift.OpenShiftImageConstants;
import org.kie.cloud.openshift.configuration.ConfigurationLoader;
import org.kie.cloud.openshift.deployment.Deployment;
import org.kie.cloud.openshift.util.NameGenerator;

/**
 * Cloud settings builder for Kie Server.
 *
 * If any environment variable isn't configured by SettingsBuilder, then default
 * value from application template is used.
 */
public class KieServerDeploymentBuilder extends AbstractDeploymentBuilder<KieServerDeploymentBuilder> {

    public KieServerDeploymentBuilder() {
        this(NameGenerator.generateDeploymentName("kieserver"));
    }

    public KieServerDeploymentBuilder(String deploymentName) {
        super(deploymentName);
    }

    @Override
    protected void initDefaultValues() {
        addOrReplaceEnvVar(OpenShiftImageConstants.KIE_SERVER_USER, "executionUser");
        addOrReplaceEnvVar(OpenShiftImageConstants.KIE_SERVER_PWD, "executionUser1!");
    }

    @Override
    protected void configureDeploymentConfig() {
        super.configureDeploymentConfig();

        String kieServerMemoryLimit = ConfigurationLoader.getKieServerMemoryLimit();
        ResourceRequirements resources = new ResourceRequirementsBuilder().withLimits(Collections.singletonMap("memory", new Quantity(kieServerMemoryLimit))).build();
        getDeployment().getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0).setResources(resources);

        ContainerPort jolokiaPort = new ContainerPortBuilder().withName("jolokia")
                                                              .withContainerPort(8778)
                                                              .withProtocol("TCP")
                                                              .build();
        ContainerPort httpPort = new ContainerPortBuilder().withName("http")
                                                           .withContainerPort(8080)
                                                           .withProtocol("TCP")
                                                           .build();
        getDeployment().getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0).getPorts().addAll(Arrays.asList(jolokiaPort, httpPort));

        // Cross reference, implication is that route name has same value as deployment name
        addOrReplaceEnvVar(OpenShiftImageConstants.KIE_SERVER_ROUTE_NAME, getDeployment().getDeploymentName());
    }

    @Override
    protected String getDefaultImageStreamName() {
        return ConfigurationLoader.getKieServerImageStreamName();
    }

    @Override
    protected String getDefaultImageStreamNamespace() {
        return ConfigurationLoader.getImageStreamNamespaceDefault();
    }

    @Override
    protected String getDefaultImageStreamTag() {
        return ConfigurationLoader.getImageStreamTag();
    }

    @Override
    protected void configureService() {
        super.configureService();

        ServicePort httpPort = new ServicePortBuilder().withName("http")
                                                       .withPort(8080)
                                                       .withNewTargetPortLike(new IntOrString(8080, null, null, new HashMap<String, Object>()))
                                                       .endTargetPort()
                                                       .build();
        Service service = getDeployment().getServices().get(0);
        service.getMetadata().getAnnotations().put("description", "All the KIE server web server's ports.");
        service.getSpec().getPorts().add(httpPort);
    }

    @Override
    protected void configureAdditionalObjects() {
        Route route = new RouteBuilder().withApiVersion("v1")
                                        .withNewMetadata()
                                            .withName(getDeployment().getDeploymentName())
                                            .addToAnnotations("description", "Route for KIE server's http service.")
                                            .addToLabels("service", getDeployment().getDeploymentName())
                                        .endMetadata()
                                        .withNewSpec()
                                            .withNewTo()
                                                .withName(getDeployment().getDeploymentName())
                                            .endTo()
                                            .withNewPort()
                                                .withNewTargetPortLike(new IntOrString(null, null, "http", new HashMap<String, Object>()))
                                                .endTargetPort()
                                            .endPort()
                                        .endSpec()
                                        .build();
        route.setAdditionalProperty("id", getDeployment().getDeploymentName() + "-http");
        getDeployment().getObjects().add(route);

        ServiceAccount serviceAccount = new ServiceAccountBuilder().withApiVersion("v1")
                                                                   .withNewMetadata()
                                                                       .withName(getDeployment().getDeploymentName())
                                                                   .endMetadata()
                                                                   .build();
        getDeployment().getObjects().add(serviceAccount);

        RoleBinding roleBinding = new RoleBindingBuilder().withApiVersion("v1")
                                                          .withNewMetadata()
                                                              .withName(getDeployment().getDeploymentName() + "-view")
                                                          .endMetadata()
                                                          .addNewSubject()
                                                              .withKind("ServiceAccount")
                                                              .withName(serviceAccount.getMetadata().getName())
                                                          .endSubject()
                                                          .withNewRoleRef()
                                                              .withName("view")
                                                          .endRoleRef()
                                                          .build();
        getDeployment().getObjects().add(roleBinding);

        getDeployment().getDeploymentConfig().getSpec().getTemplate().getSpec().setServiceAccountName(serviceAccount.getMetadata().getName());
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

    public KieServerDeploymentBuilder withImageStreamNamespaceFromProperties() {
        addOrReplaceProperty("ImageStream Namespace", "Namespace in which the ImageStreams for Red Hat Middleware images are" +
                " installed. These ImageStreams are normally installed in the openshift namespace." +
                " You should only need to modify this if you've installed the ImageStreams in a" +
                " different namespace/project.", OpenShiftImageConstants.IMAGE_STREAM_NAMESPACE, getDefaultImageStreamNamespace(), true);

        withImageStreamNamespace("${" + OpenShiftImageConstants.IMAGE_STREAM_NAMESPACE + "}");
        return this;
    }

    public KieServerDeploymentBuilder withImageStreamNameFromProperties() {
        String defaultImageStreamName = getDefaultImageStreamName();
        addOrReplaceProperty("KIE Server ImageStream Name", "The name of the image stream to use for KIE server. Default is \"" + defaultImageStreamName + "\".", OpenShiftImageConstants.KIE_SERVER_IMAGE_STREAM_NAME, defaultImageStreamName, true);

        withImageStreamName("${" + OpenShiftImageConstants.KIE_SERVER_IMAGE_STREAM_NAME + "}");
        return this;
    }

    public KieServerDeploymentBuilder withImageStreamTagFromProperties() {
        String defaultImageStreamTag = getDefaultImageStreamTag();
        addOrReplaceProperty("ImageStream Tag", "A named pointer to an image in an image stream. Default is \"" + defaultImageStreamTag + "\".", OpenShiftImageConstants.IMAGE_STREAM_TAG, defaultImageStreamTag, true);

        withImageStreamTag("${" + OpenShiftImageConstants.IMAGE_STREAM_TAG + "}");
        return this;
    }

    public KieServerDeploymentBuilder withKieServerId(String kieServerId) {
        addOrReplaceEnvVar(OpenShiftImageConstants.KIE_SERVER_ID, kieServerId);
        return this;
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

    /**
     * Return configured builder with Kie Server user from template properties.
     *
     * @return Builder
     */
    public KieServerDeploymentBuilder withKieServerUserFromProperties() {
        addOrReplaceProperty("KIE Server User", "KIE server username (Sets the org.kie.server.user system property)", OpenShiftImageConstants.KIE_SERVER_USER, "executionUser", false);
        addOrReplaceProperty("KIE Server Password", "KIE server password (Sets the org.kie.server.pwd system property)", OpenShiftImageConstants.KIE_SERVER_PWD, "[a-zA-Z]{6}[0-9]{1}!", "expression", false);
        withKieServerUser("${" + OpenShiftImageConstants.KIE_SERVER_USER + "}", "${" + OpenShiftImageConstants.KIE_SERVER_PWD + "}");
        return this;
    }

    public KieServerDeploymentBuilder withKieServerControllerConnectionFromProperties() {
        addOrReplaceProperty("KIE Server Controller User", "KIE server controller username (Sets the org.kie.server.controller.user system property)", OpenShiftImageConstants.KIE_SERVER_CONTROLLER_USER, "controllerUser", false);
        // TODO: Bug - missing value
        addOrReplaceProperty("KIE Server Controller Password", "KIE server controller password (Sets the org.kie.server.controller.pwd system property)", OpenShiftImageConstants.KIE_SERVER_CONTROLLER_PWD, false);
        addOrReplaceProperty("KIE Server Controller Token", "KIE server controller token for bearer authentication (Sets the org.kie.server.controller.token system property)", OpenShiftImageConstants.KIE_SERVER_CONTROLLER_TOKEN, false);
        addOrReplaceProperty("KIE Server Controller Service", "The service name for the optional business central monitor, where it can be reached to allow service lookup, and registered with to allow monitoring console functionality (If set, will be used to discover host and port)", OpenShiftImageConstants.KIE_SERVER_CONTROLLER_SERVICE, false);
        addOrReplacePropertyWithExample("KIE Server Controller host", "KIE server controller host (Used to set the org.kie.server.controller system property)", OpenShiftImageConstants.KIE_SERVER_CONTROLLER_HOST, "my-app-controller-ocpuser.os.example.com", false);
        addOrReplacePropertyWithExample("KIE Server Controller port", "KIE server controller port (Used to set the org.kie.server.controller system property)", OpenShiftImageConstants.KIE_SERVER_CONTROLLER_PORT, "8080", false);
        addOrReplaceEnvVar(OpenShiftImageConstants.KIE_SERVER_CONTROLLER_USER, "${" + OpenShiftImageConstants.KIE_SERVER_CONTROLLER_USER + "}");
        addOrReplaceEnvVar(OpenShiftImageConstants.KIE_SERVER_CONTROLLER_PWD, "${" + OpenShiftImageConstants.KIE_SERVER_CONTROLLER_PWD + "}");
        addOrReplaceEnvVar(OpenShiftImageConstants.KIE_SERVER_CONTROLLER_TOKEN, "${" + OpenShiftImageConstants.KIE_SERVER_CONTROLLER_TOKEN + "}");
        addOrReplaceEnvVar(OpenShiftImageConstants.KIE_SERVER_CONTROLLER_SERVICE, "${" + OpenShiftImageConstants.KIE_SERVER_CONTROLLER_SERVICE + "}");
        addOrReplaceEnvVar(OpenShiftImageConstants.KIE_SERVER_CONTROLLER_PROTOCOL, "ws");
        addOrReplaceEnvVar(OpenShiftImageConstants.KIE_SERVER_CONTROLLER_HOST, "${" + OpenShiftImageConstants.KIE_SERVER_CONTROLLER_HOST + "}");
        addOrReplaceEnvVar(OpenShiftImageConstants.KIE_SERVER_CONTROLLER_PORT, "${" + OpenShiftImageConstants.KIE_SERVER_CONTROLLER_PORT + "}");
        return this;
    }

    public KieServerDeploymentBuilder withHttpsFromProperties() {
        addOrReplacePropertyWithExample("KIE Server Keystore Secret Name", "The name of the secret containing the keystore file", OpenShiftImageConstants.KIE_SERVER_HTTPS_SECRET, "kieserver-app-secret", true);
        addOrReplaceProperty("KIE Server Keystore Filename", "The name of the keystore file within the secret", OpenShiftImageConstants.KIE_SERVER_HTTPS_KEYSTORE, "keystore.jks", false);
        addOrReplaceProperty("KIE Server Certificate Name", "The name associated with the server certificate", OpenShiftImageConstants.KIE_SERVER_HTTPS_NAME, "jboss", false);
        addOrReplaceProperty("KIE Server Keystore Password", "The password for the keystore and certificate", OpenShiftImageConstants.KIE_SERVER_HTTPS_PASSWORD, "mykeystorepass", false);
        withHttps("${" + OpenShiftImageConstants.KIE_SERVER_HTTPS_SECRET + "}", "${" + OpenShiftImageConstants.KIE_SERVER_HTTPS_KEYSTORE + "}", "${" + OpenShiftImageConstants.KIE_SERVER_HTTPS_NAME + "}", "${" + OpenShiftImageConstants.KIE_SERVER_HTTPS_PASSWORD + "}");
        return this;
    }

    public KieServerDeploymentBuilder withHttps(String secretName, String keystore, String name, String password) {
        String volumeName = "kieserver-keystore-volume";
        String volumeDir = "/etc/kieserver-secret-volume";

        // Create route
        Route httpsRoute = new RouteBuilder().withApiVersion("v1")
                                             .withNewMetadata()
                                                 .withName("secure-" + getDeployment().getDeploymentName())
                                                 .addToAnnotations("description", "Route for KIE server's https service.")
                                                 .addToLabels("service", getDeployment().getDeploymentName())
                                             .endMetadata()
                                             .withNewSpec()
                                                 .withNewTo()
                                                     .withName(getDeployment().getDeploymentName())
                                                 .endTo()
                                                 .withNewPort()
                                                     .withNewTargetPortLike(new IntOrString(null, null, "https", new HashMap<String, Object>()))
                                                     .endTargetPort()
                                                 .endPort()
                                                 .withNewTls()
                                                     .withTermination("passthrough")
                                                 .endTls()
                                             .endSpec()
                                             .build();
        httpsRoute.setAdditionalProperty("id", getDeployment().getDeploymentName() + "-https");
        getDeployment().getObjects().add(httpsRoute);

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

    public KieServerDeploymentBuilder withClustering() {
        int pingPort = 8888;
        String pingServiceName = getDeployment().getDeploymentName() + "-ping";
        Service clusteringService = new ServiceBuilder().withApiVersion("v1")
                                                        .withNewMetadata()
                                                            .withName(pingServiceName)
                                                            .addToAnnotations("description", "The JGroups ping port for clustering.")
                                                            .addToAnnotations("service.alpha.kubernetes.io/tolerate-unready-endpoints", "true")
                                                            .addToLabels("service", getDeployment().getDeploymentName())
                                                        .endMetadata()
                                                        .withNewSpec()
                                                            .withClusterIP("None")
                                                            .addNewPort()
                                                                .withName("ping")
                                                                .withPort(pingPort)
                                                                .withNewTargetPortLike(new IntOrString(pingPort, null, null, new HashMap<String, Object>()))
                                                                .endTargetPort()
                                                            .endPort()
                                                            .withSelector(Collections.singletonMap("deploymentConfig", getDeployment().getDeploymentName()))
                                                        .endSpec()
                                                        .build();
        getDeployment().getObjects().add(clusteringService);

        // Set container port
        ContainerPort pingContainerPort = new ContainerPortBuilder().withName("ping")
                                                                    .withContainerPort(pingPort)
                                                                    .withProtocol("TCP")
                                                                    .build();
        getDeployment().getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0).getPorts().add(pingContainerPort);

        addOrReplaceEnvVar(OpenShiftImageConstants.JGROUPS_PING_PROTOCOL, "openshift.DNS_PING");
        addOrReplaceEnvVar(OpenShiftImageConstants.OPENSHIFT_DNS_PING_SERVICE_NAME, pingServiceName);
        addOrReplaceEnvVar(OpenShiftImageConstants.OPENSHIFT_DNS_PING_SERVICE_PORT, "" + pingPort);
        return this;
    }

    public KieServerDeploymentBuilder withHttpHostnameFromProperties() {
        addOrReplaceProperty("KIE Server Custom http Route Hostname", "Custom hostname for http service route. Leave blank for default hostname, e.g.: <application-name>-kieserver-<project>.<default-domain-suffix>", OpenShiftImageConstants.KIE_SERVER_HOSTNAME_HTTP, "", false);
        withHttpHostname("${" + OpenShiftImageConstants.KIE_SERVER_HOSTNAME_HTTP + "}");
        return this;
    }

    public KieServerDeploymentBuilder withHttpHostname(String hostname) {
        getDeployment().getUnsecureRoutes().stream()
                                           .filter(r -> r.getSpec().getPort().getTargetPort().getStrVal().equals("http"))
                                           .findAny()
                                           .ifPresent(r -> r.getSpec().setHost(hostname));
        return this;
    }

    public KieServerDeploymentBuilder withHttpsHostnameFromProperties() {
        addOrReplaceProperty("KIE Server Custom https Route Hostname", "Custom hostname for https service route.  Leave blank for default hostname, e.g.: secure-<application-name>-kieserver-<project>.<default-domain-suffix>", OpenShiftImageConstants.KIE_SERVER_HOSTNAME_HTTPS, "", false);
        withHttpsHostname("${" + OpenShiftImageConstants.KIE_SERVER_HOSTNAME_HTTPS + "}");
        return this;
    }

    public KieServerDeploymentBuilder withHttpsHostname(String hostname) {
        Route httpsRoute = getDeployment().getSecureRoutes().stream()
                                                            .filter(r -> r.getSpec().getPort().getTargetPort().getStrVal().equals("https"))
                                                            .findAny()
                                                            .orElseThrow(() -> new RuntimeException("Cannot set HTTPS hostname, HTTPS route not found."));
        httpsRoute.getSpec().setHost(hostname);
        return this;
    }

    public KieServerDeploymentBuilder withContainerMemoryLimitFromProperties() {
        addOrReplaceProperty("KIE Server Container Memory Limit", "KIE server Container memory limit", OpenShiftImageConstants.KIE_SERVER_MEMORY_LIMIT, "1Gi", false);
        withContainerMemoryLimit("${" + OpenShiftImageConstants.KIE_SERVER_MEMORY_LIMIT + "}");
        return this;
    }

    public KieServerDeploymentBuilder withContainerMemoryLimit(String kieServerMemoryLimit) {
        getDeployment().getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0).getResources().getLimits().put("memory", new Quantity(kieServerMemoryLimit));
        return this;
    }

    public KieServerDeploymentBuilder withKieMbeansFromProperties() {
        addOrReplaceProperty("KIE MBeans", "KIE server mbeans enabled/disabled (Sets the kie.mbeans and kie.scanner.mbeans system properties)", OpenShiftImageConstants.KIE_MBEANS, "enabled", false);
        withKieMbeans("${" + OpenShiftImageConstants.KIE_MBEANS + "}");
        return this;
    }

    public KieServerDeploymentBuilder withKieMbeans(String kieMbeans) {
        addOrReplaceEnvVar(OpenShiftImageConstants.KIE_MBEANS, kieMbeans);
        return this;
    }

    public KieServerDeploymentBuilder withKieServerBypassAuthUserFromProperties() {
        addOrReplaceProperty("KIE Server Bypass Auth User", "KIE server bypass auth user (Sets the org.kie.server.bypass.auth.user system property)", OpenShiftImageConstants.KIE_SERVER_BYPASS_AUTH_USER, "false", false);
        addOrReplaceEnvVar(OpenShiftImageConstants.KIE_SERVER_BYPASS_AUTH_USER, "${" + OpenShiftImageConstants.KIE_SERVER_BYPASS_AUTH_USER + "}");
        return this;
    }

    public KieServerDeploymentBuilder withKieServerBypassAuthUser(boolean kieServerBypassAuthUser) {
        addOrReplaceEnvVar(OpenShiftImageConstants.KIE_SERVER_BYPASS_AUTH_USER, Boolean.valueOf(kieServerBypassAuthUser).toString());
        return this;
    }

    public KieServerDeploymentBuilder withKieServerClassFilteringFromProperties() {
        addOrReplaceProperty("Drools Server Filter Classes", "KIE server class filtering (Sets the org.drools.server.filter.classes system property)", OpenShiftImageConstants.DROOLS_SERVER_FILTER_CLASSES, "true", false);
        addOrReplaceEnvVar(OpenShiftImageConstants.DROOLS_SERVER_FILTER_CLASSES, "${" + OpenShiftImageConstants.DROOLS_SERVER_FILTER_CLASSES + "}");
        return this;
    }

    public KieServerDeploymentBuilder withKieServerClassFiltering(boolean kieServerClassFiltering) {
        addOrReplaceEnvVar(OpenShiftImageConstants.DROOLS_SERVER_FILTER_CLASSES, Boolean.valueOf(kieServerClassFiltering).toString());
        return this;
    }

    public MavenRepoBuilder withMavenRepo() {
        return withMavenRepo(NameGenerator.generateRandomNameUpperCase());
    }

    public MavenRepoBuilder withMavenRepo(String mavenRepoPrefix) {
        return new MavenRepoBuilder(mavenRepoPrefix);
    }

    public class MavenRepoBuilder {

        private String mavenRepoPrefix;

        public MavenRepoBuilder(String mavenRepoPrefix) {
            this.mavenRepoPrefix = mavenRepoPrefix;
            addOrAppendEnvVar(OpenShiftImageConstants.MAVEN_REPOS, mavenRepoPrefix);
        }

        public MavenRepoBuilder withId(String mavenRepoId) {
            addOrReplaceEnvVar(mavenRepoPrefix + "_" + OpenShiftImageConstants.MAVEN_REPO_ID, mavenRepoId);
            return this;
        }

        public MavenRepoBuilder withService(String mavenRepoService, String mavenRepoPath) {
            addOrReplaceEnvVar(mavenRepoPrefix + "_" + OpenShiftImageConstants.MAVEN_REPO_SERVICE, mavenRepoService);
            addOrReplaceEnvVar(mavenRepoPrefix + "_" + OpenShiftImageConstants.MAVEN_REPO_PATH, mavenRepoPath);
            return this;
        }

        public MavenRepoBuilder withUrl(String mavenRepoUrl) {
            addOrReplaceEnvVar(mavenRepoPrefix + "_" + OpenShiftImageConstants.MAVEN_REPO_URL, mavenRepoUrl);
            return this;
        }

        public MavenRepoBuilder withAuthentication(String mavenRepoUsername, String mavenRepoPassword) {
            addOrReplaceEnvVar(mavenRepoPrefix + "_" + OpenShiftImageConstants.MAVEN_REPO_USERNAME, mavenRepoUsername);
            addOrReplaceEnvVar(mavenRepoPrefix + "_" + OpenShiftImageConstants.MAVEN_REPO_PASSWORD, mavenRepoPassword);
            return this;
        }

        public KieServerDeploymentBuilder endMavenRepo() {
            Optional<String> repoUrl = getDeployment().getOptionalEnvironmentVariableValue(mavenRepoPrefix + "_" + OpenShiftImageConstants.MAVEN_REPO_URL);
            Optional<String> repoService = getDeployment().getOptionalEnvironmentVariableValue(mavenRepoPrefix + "_" + OpenShiftImageConstants.MAVEN_REPO_SERVICE);

            if (repoUrl.isPresent() && repoService.isPresent()) {
                throw new RuntimeException("Maven repo URL and Maven repo service cannot be defined in the same time.");
            }
            if (!repoUrl.isPresent() && !repoService.isPresent()) {
                throw new RuntimeException("Maven repo URL or Maven repo service must be defined.");
            }

            return KieServerDeploymentBuilder.this;
        }
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
