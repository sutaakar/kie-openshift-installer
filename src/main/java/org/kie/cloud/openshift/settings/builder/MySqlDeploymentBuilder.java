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

/**
 * Cloud settings builder for Kie Server.
 *
 * If any environment variable isn't configured by SettingsBuilder, then default
 * value from application template is used.
 */
public class MySqlDeploymentBuilder extends AbstractDeploymentBuilder {

    public MySqlDeploymentBuilder(Template mySqlTemplate) {
        super(mySqlTemplate);
        // Configure default values as these parameters are mandatory
        EnvVar mySqlUserVar = new EnvVar(OpenShiftImageConstants.MYSQL_USER, "mySqlUser", null);
        EnvVar mySqlPwdVar = new EnvVar(OpenShiftImageConstants.MYSQL_PASSWORD, "mySqlPwd", null);
        EnvVar mySqlDbNameVar = new EnvVar(OpenShiftImageConstants.MYSQL_DATABASE, "mysqlDb", null);
        addOrReplaceEnvVar(mySqlUserVar);
        addOrReplaceEnvVar(mySqlPwdVar);
        addOrReplaceEnvVar(mySqlDbNameVar);
    }

    public MySqlDeploymentBuilder withDatabaseUser(String mySqlUser, String mySqlPwd) {
        EnvVar mySqlUserVar = new EnvVar(OpenShiftImageConstants.MYSQL_USER, mySqlUser, null);
        EnvVar mySqlPwdVar = new EnvVar(OpenShiftImageConstants.MYSQL_PASSWORD, mySqlPwd, null);
        addOrReplaceEnvVar(mySqlUserVar);
        addOrReplaceEnvVar(mySqlPwdVar);
        return this;
    }

    public MySqlDeploymentBuilder withDatabaseName(String dbName) {
        EnvVar mySqlDbNameVar = new EnvVar(OpenShiftImageConstants.MYSQL_DATABASE, dbName, null);
        addOrReplaceEnvVar(mySqlDbNameVar);
        return this;
    }
}
