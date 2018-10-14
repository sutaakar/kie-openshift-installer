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

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerPort;
import io.fabric8.kubernetes.api.model.ContainerPortBuilder;
import io.fabric8.kubernetes.api.model.Probe;
import io.fabric8.kubernetes.api.model.ProbeBuilder;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.kubernetes.api.model.ServicePortBuilder;
import org.kie.cloud.openshift.OpenShiftImageConstants;
import org.kie.cloud.openshift.configuration.ConfigurationLoader;
import org.kie.cloud.openshift.util.NameGenerator;

/**
 * Cloud settings builder for Kie Server.
 *
 * If any environment variable isn't configured by SettingsBuilder, then default
 * value from application template is used.
 */
public class PostgreSqlDeploymentBuilder extends AbstractDeploymentBuilder<PostgreSqlDeploymentBuilder> {

    public PostgreSqlDeploymentBuilder() {
        this(NameGenerator.generateDeploymentName("postgresql"));
    }

    public PostgreSqlDeploymentBuilder(String deploymentName) {
        super(deploymentName);
    }

    @Override
    protected void initDefaultValues() {
        addOrReplaceEnvVar(OpenShiftImageConstants.POSTGRESQL_USER, "postgreSqlUser");
        addOrReplaceEnvVar(OpenShiftImageConstants.POSTGRESQL_PASSWORD, "postgreSqlPwd");
        addOrReplaceEnvVar(OpenShiftImageConstants.POSTGRESQL_DATABASE, "postgreSqlDb");
        addOrReplaceEnvVar(OpenShiftImageConstants.POSTGRESQL_MAX_PREPARED_TRANSACTIONS, "100");
    }

    @Override
    protected void configureDeploymentConfig() {
        super.configureDeploymentConfig();

        ContainerPort postgresqlPort = new ContainerPortBuilder().withContainerPort(5432)
                                                                 .withProtocol("TCP")
                                                                 .build();
        getDeployment().getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0).getPorts().add(postgresqlPort);
    }

    @Override
    protected String getDefaultImageStreamName() {
        return ConfigurationLoader.getPostgreSqlImageStreamName();
    }

    @Override
    protected String getDefaultImageStreamNamespace() {
        return ConfigurationLoader.getImageStreamNamespaceDefault();
    }

    @Override
    protected String getDefaultImageStreamTag() {
        return ConfigurationLoader.getPostgreSqlImageStreamTag();
    }

    @Override
    protected void configureService() {
        ServicePort httpPort = new ServicePortBuilder().withPort(5432)
                                                       .withNewTargetPort(5432)
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
        getDeployment().getObjects().add(service);
    }

    @Override
    protected void configureLivenessProbe() {
        Probe livenessProbe = new ProbeBuilder().withNewExec()
                                                     .withCommand("/usr/libexec/check-container", "--live")
                                                 .endExec()
                                                 .withInitialDelaySeconds(120)
                                                 .withTimeoutSeconds(10)
                                                 .build();
        // Just one container should be available
        Container container = getDeployment().getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0);
        container.setLivenessProbe(livenessProbe);
    }

    @Override
    protected void configureReadinessProbe() {
        Probe readinessProbe = new ProbeBuilder().withNewExec()
                                                     .withCommand("/usr/libexec/check-container")
                                                 .endExec()
                                                 .withInitialDelaySeconds(5)
                                                 .withTimeoutSeconds(1)
                                                 .build();
        // Just one container should be available
        Container container = getDeployment().getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0);
        container.setReadinessProbe(readinessProbe);
    }

    public PostgreSqlDeploymentBuilder withDatabaseUser(String postgreSqlUser, String postgreSqlPwd) {
        addOrReplaceEnvVar(OpenShiftImageConstants.POSTGRESQL_USER, postgreSqlUser);
        addOrReplaceEnvVar(OpenShiftImageConstants.POSTGRESQL_PASSWORD, postgreSqlPwd);
        return this;
    }

    public PostgreSqlDeploymentBuilder withDatabaseName(String dbName) {
        addOrReplaceEnvVar(OpenShiftImageConstants.POSTGRESQL_DATABASE, dbName);
        return this;
    }

    public PostgreSqlDeploymentBuilder makePersistent() {
        addPersistence(getDeployment().getDeploymentName(), "/var/lib/pgsql/data", "ReadWriteOnce", "1Gi");
        return this;
    }
}
