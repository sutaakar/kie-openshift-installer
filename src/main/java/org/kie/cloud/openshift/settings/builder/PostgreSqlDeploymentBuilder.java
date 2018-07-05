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

import io.fabric8.openshift.api.model.Template;
import org.kie.cloud.openshift.OpenShiftImageConstants;

/**
 * Cloud settings builder for Kie Server.
 *
 * If any environment variable isn't configured by SettingsBuilder, then default
 * value from application template is used.
 */
public class PostgreSqlDeploymentBuilder extends AbstractDeploymentBuilder {

    public PostgreSqlDeploymentBuilder(Template postgreSqlTemplate) {
        super(postgreSqlTemplate);
        // Configure default values as these parameters are mandatory
        addOrReplaceEnvVar(OpenShiftImageConstants.POSTGRESQL_USER, "postgreSqlUser");
        addOrReplaceEnvVar(OpenShiftImageConstants.POSTGRESQL_PASSWORD, "postgreSqlPwd");
        addOrReplaceEnvVar(OpenShiftImageConstants.POSTGRESQL_DATABASE, "postgreSqlDb");
        addOrReplaceEnvVar(OpenShiftImageConstants.POSTGRESQL_MAX_PREPARED_TRANSACTIONS, "100");
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
        addPersistence("${APPLICATION_NAME}-postgresql", "/var/lib/postgresql/data", "ReadWriteOnce", "1Gi");
        return this;
    }
}
