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
import java.util.List;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Probe;
import io.fabric8.kubernetes.api.model.ProbeBuilder;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.kubernetes.api.model.ServicePortBuilder;
import io.fabric8.openshift.api.model.DeploymentConfig;
import io.fabric8.openshift.api.model.DeploymentConfigBuilder;
import io.fabric8.openshift.api.model.Template;
import org.kie.cloud.openshift.OpenShiftImageConstants;
import org.kie.cloud.openshift.configuration.ConfigurationLoader;
import org.kie.cloud.openshift.util.NameGenerator;

/**
 * Cloud settings builder for Kie Server.
 *
 * If any environment variable isn't configured by SettingsBuilder, then default
 * value from application template is used.
 */
public class MySqlDeploymentBuilder extends AbstractDeploymentBuilder {

    public MySqlDeploymentBuilder(Template mySqlTemplate) {
        super(mySqlTemplate, NameGenerator.generateDeploymentName("mysql"));
    }

    public MySqlDeploymentBuilder(Template mySqlTemplate, String deploymentName) {
        super(mySqlTemplate, deploymentName);
    }

    @Override
    protected void initDefaultValues() {
        addOrReplaceEnvVar(OpenShiftImageConstants.MYSQL_USER, "mySqlUser");
        addOrReplaceEnvVar(OpenShiftImageConstants.MYSQL_PASSWORD, "mySqlPwd");
        addOrReplaceEnvVar(OpenShiftImageConstants.MYSQL_DATABASE, "mysqlDb");
    }

    @Override
    protected void configureDeploymentConfig() {
        String mySqlImageStreamName = ConfigurationLoader.getMySqlImageStreamName();
        String mySqlImageStreamTag = ConfigurationLoader.getMySqlImageStreamTag();

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
                                                                                         .withName(mySqlImageStreamName + ":" + mySqlImageStreamTag)
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
                                                                                         .withImage("mysql")
                                                                                         .withImagePullPolicy("Always")
                                                                                         .addNewPort()
                                                                                             .withContainerPort(3306)
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
        ServicePort httpPort = new ServicePortBuilder().withPort(3306)
                                                       .withNewTargetPort(3306)
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
    protected void configureLivenessProbe() {
        Probe livenessProbe = new ProbeBuilder().withNewTcpSocket()
                                                    .withNewPort(3306)
                                                .endTcpSocket()
                                                .withInitialDelaySeconds(30)
                                                .withTimeoutSeconds(1)
                                                .build();
        // Just one container should be available
        Container container = getDeployment().getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0);
        container.setLivenessProbe(livenessProbe);
    }

    @Override
    protected void configureReadinessProbe() {
        // TODO: Should the port be configured too? See original template.
        Probe readinessProbe = new ProbeBuilder().withNewExec()
                                                     .withCommand("/bin/sh", "-i", "-c", "MYSQL_PWD=\"$MYSQL_PASSWORD\" mysql -h 127.0.0.1 -u $MYSQL_USER -D $MYSQL_DATABASE -e 'SELECT 1'")
                                                 .endExec()
                                                 .withInitialDelaySeconds(5)
                                                 .withTimeoutSeconds(1)
                                                 .build();
        // Just one container should be available
        Container container = getDeployment().getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0);
        container.setReadinessProbe(readinessProbe);
    }

    public MySqlDeploymentBuilder withDatabaseUser(String mySqlUser, String mySqlPwd) {
        addOrReplaceEnvVar(OpenShiftImageConstants.MYSQL_USER, mySqlUser);
        addOrReplaceEnvVar(OpenShiftImageConstants.MYSQL_PASSWORD, mySqlPwd);
        return this;
    }

    public MySqlDeploymentBuilder withDatabaseName(String dbName) {
        addOrReplaceEnvVar(OpenShiftImageConstants.MYSQL_DATABASE, dbName);
        return this;
    }

    public MySqlDeploymentBuilder makePersistent() {
        addPersistence(getDeployment().getDeploymentName(), "/var/lib/mysql/data", "ReadWriteOnce", "1Gi");
        return this;
    }
}
