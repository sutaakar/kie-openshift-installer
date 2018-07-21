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
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
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
        // TODO: is there any default? If so probably delete.
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
        // TODO: is there any default? If so probably delete.
        addOrReplaceEnvVar(OpenShiftImageConstants.TIMER_SERVICE_DATA_STORE_REFRESH_INTERVAL, "30000");
        return this;
    }
}
