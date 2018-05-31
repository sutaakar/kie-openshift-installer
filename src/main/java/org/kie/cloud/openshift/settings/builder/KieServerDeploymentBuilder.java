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

import io.fabric8.kubernetes.api.model.EnvVar;
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
        super(kieServerTemplate);
    }

    public KieServerDeploymentBuilder withApplicationName(String applicationName) {
        setApplicationName(applicationName);
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
        EnvVar kieServerUserVar = new EnvVar(OpenShiftImageConstants.KIE_SERVER_USER, kieServerUser, null);
        EnvVar kieServerPwdVar = new EnvVar(OpenShiftImageConstants.KIE_SERVER_PWD, kieServerPwd, null);
        addOrReplaceEnvVar(kieServerUserVar);
        addOrReplaceEnvVar(kieServerPwdVar);
        return this;
    }

    public KieServerDeploymentBuilder connectToMySqlDatabase(Deployment databaseDeployment) {
        EnvVar kieServerPersistenceDialect = new EnvVar(OpenShiftImageConstants.KIE_SERVER_PERSISTENCE_DIALECT, "org.hibernate.dialect.MySQL5Dialect", null);
        EnvVar kieServerPersistenceDatasource = new EnvVar(OpenShiftImageConstants.KIE_SERVER_PERSISTENCE_DS, "java:/jboss/datasources/kie", null);
        EnvVar datasourceName = new EnvVar("DATASOURCES", "KIE", null);
        EnvVar datasourceDatabaseName = new EnvVar("KIE_DATABASE", getEnvVarValue(databaseDeployment, OpenShiftImageConstants.MYSQL_DATABASE), null);
        EnvVar datasourceJndi = new EnvVar("KIE_JNDI", "java:/jboss/datasources/kie", null);
        EnvVar datasourceDriver = new EnvVar("KIE_DRIVER", "mysql", null);
        EnvVar datasourceJta = new EnvVar("KIE_JTA", "true", null);
        EnvVar datasourceTxIsolation = new EnvVar("KIE_TX_ISOLATION", "TRANSACTION_READ_COMMITTED", null);
        EnvVar datasourceUsername = new EnvVar("KIE_USERNAME", getEnvVarValue(databaseDeployment, OpenShiftImageConstants.MYSQL_USER), null);
        EnvVar datasourcePassword = new EnvVar("KIE_PASSWORD", getEnvVarValue(databaseDeployment, OpenShiftImageConstants.MYSQL_PASSWORD), null);
        // Set to first unsecure service
        EnvVar datasourceServiceHost = new EnvVar("KIE_SERVICE_HOST", databaseDeployment.getUnsecureServices().get(0).getMetadata().getName(), null);
        EnvVar datasourceServicePort = new EnvVar("KIE_SERVICE_PORT", "3306", null);
        // Same as service host
        EnvVar timerServiceDataStore = new EnvVar("TIMER_SERVICE_DATA_STORE", databaseDeployment.getUnsecureServices().get(0).getMetadata().getName(), null);
        // TODO: is there any default? If so probably delete.
        EnvVar timerServiceDataStoreRefreshInterval = new EnvVar("TIMER_SERVICE_DATA_STORE_REFRESH_INTERVAL", "30000", null);
        return this;
    }
}
