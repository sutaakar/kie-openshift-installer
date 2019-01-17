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

import java.util.HashMap;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerPort;
import io.fabric8.kubernetes.api.model.ContainerPortBuilder;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.Probe;
import io.fabric8.kubernetes.api.model.ProbeBuilder;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.kubernetes.api.model.ServicePortBuilder;
import org.kie.cloud.openshift.OpenShiftImageConstants;
import org.kie.cloud.openshift.configuration.ConfigurationLoader;
import org.kie.cloud.openshift.deployment.MySqlDeployment;
import org.kie.cloud.openshift.util.NameGenerator;

/**
 * Cloud settings builder for Kie Server.
 *
 * If any environment variable isn't configured by SettingsBuilder, then default
 * value from application template is used.
 */
public class MySqlDeploymentBuilder extends AbstractDeploymentBuilder<MySqlDeploymentBuilder, MySqlDeployment> {

    public MySqlDeploymentBuilder() {
        this(NameGenerator.generateDeploymentName("mysql"));
    }

    public MySqlDeploymentBuilder(String deploymentName) {
        super(new MySqlDeployment(deploymentName));
    }

    @Override
    protected void initDefaultValues() {
        addOrReplaceEnvVar(OpenShiftImageConstants.MYSQL_USER, "mySqlUser");
        addOrReplaceEnvVar(OpenShiftImageConstants.MYSQL_PASSWORD, "mySqlPwd");
        addOrReplaceEnvVar(OpenShiftImageConstants.MYSQL_DATABASE, "mysqlDb");
    }

    @Override
    protected void configureDeploymentConfig() {
        super.configureDeploymentConfig();

        ContainerPort mysqlPort = new ContainerPortBuilder().withContainerPort(3306)
                                                            .withProtocol("TCP")
                                                            .build();
        getDeployment().getDeploymentConfig().getSpec().getTemplate().getSpec().getContainers().get(0).getPorts().add(mysqlPort);
    }

    @Override
    protected String getDefaultImageStreamName() {
        return ConfigurationLoader.getMySqlImageStreamName();
    }

    @Override
    protected String getDefaultImageStreamNamespace() {
        return ConfigurationLoader.getImageStreamNamespaceDefault();
    }

    @Override
    protected String getDefaultImageStreamTag() {
        return ConfigurationLoader.getMySqlImageStreamTag();
    }

    @Override
    protected void configureService() {
        super.configureService();

        ServicePort mysqlPort = new ServicePortBuilder().withPort(3306)
                                                        .withNewTargetPortLike(new IntOrString(3306, null, null, new HashMap<String, Object>()))
                                                        .endTargetPort()
                                                        .build();
        Service service = getDeployment().getServices().get(0);
        service.getMetadata().getAnnotations().put("description", "The database server's port.");
        service.getSpec().getPorts().add(mysqlPort);
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

    public MySqlDeploymentBuilder withImageStreamNamespaceFromProperties() {
        addOrReplaceProperty("MySQL ImageStream Namespace", "Namespace in which the ImageStream for the MySQL image is" +
                " installed. The ImageStream is already installed in the openshift namespace." +
                " You should only need to modify this if you've installed the ImageStream in a" +
                " different namespace/project. Default is \"openshift\".", OpenShiftImageConstants.MYSQL_IMAGE_STREAM_NAMESPACE, getDefaultImageStreamNamespace(), false);

        withImageStreamNamespace("${" + OpenShiftImageConstants.MYSQL_IMAGE_STREAM_NAMESPACE + "}");
        return this;
    }

    public MySqlDeploymentBuilder withImageStreamTagFromProperties() {
        String defaultImageStreamTag = getDefaultImageStreamTag();
        addOrReplaceProperty("MySQL ImageStream Tag", "The MySQL image version, which is intended to correspond to the MySQL version. Default is \"" + defaultImageStreamTag + "\".", OpenShiftImageConstants.MYSQL_IMAGE_STREAM_TAG, defaultImageStreamTag, false);

        withImageStreamTag("${" + OpenShiftImageConstants.MYSQL_IMAGE_STREAM_TAG + "}");
        return this;
    }

    public MySqlDeploymentBuilder withDatabaseUser(String mySqlUser, String mySqlPwd) {
        addOrReplaceEnvVar(OpenShiftImageConstants.MYSQL_USER, mySqlUser);
        addOrReplaceEnvVar(OpenShiftImageConstants.MYSQL_PASSWORD, mySqlPwd);
        return this;
    }

    public MySqlDeploymentBuilder withDatabaseUserFromProperties() {
        addOrReplaceProperty("KIE Server MySQL Database User", "KIE server MySQL database username", OpenShiftImageConstants.KIE_SERVER_MYSQL_USER, "rhpam", false);
        addOrReplaceProperty("KIE Server MySQL Database Password", "KIE server MySQL database password", OpenShiftImageConstants.KIE_SERVER_MYSQL_PWD, "[a-zA-Z]{6}[0-9]{1}!", "expression", false);
        withDatabaseUser("${" + OpenShiftImageConstants.KIE_SERVER_MYSQL_USER + "}", "${" + OpenShiftImageConstants.KIE_SERVER_MYSQL_PWD + "}");
        return this;
    }

    public MySqlDeploymentBuilder withDatabaseName(String dbName) {
        addOrReplaceEnvVar(OpenShiftImageConstants.MYSQL_DATABASE, dbName);
        return this;
    }

    public MySqlDeploymentBuilder withDatabaseNameFromProperties() {
        addOrReplaceProperty("KIE Server MySQL Database Name", "KIE server MySQL database name", OpenShiftImageConstants.KIE_SERVER_MYSQL_DB, "rhpam7", false);
        withDatabaseName("${" + OpenShiftImageConstants.KIE_SERVER_MYSQL_DB + "}");
        return this;
    }

    public MySqlDeploymentBuilder makePersistentFromProperties() {
        addOrReplaceProperty("Database Volume Capacity", "Size of persistent storage for database volume.", OpenShiftImageConstants.DB_VOLUME_CAPACITY, "1Gi", true);
        makePersistent("${" + OpenShiftImageConstants.DB_VOLUME_CAPACITY + "}");
        return this;
    }

    public MySqlDeploymentBuilder makePersistent() {
        makePersistent("1Gi");
        return this;
    }

    public MySqlDeploymentBuilder makePersistent(String persistentVolumeStorageSize) {
        addPersistence(getDeployment().getDeploymentName(), "/var/lib/mysql/data", "ReadWriteOnce", persistentVolumeStorageSize);
        return this;
    }
}
