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

import java.util.List;

import org.kie.cloud.openshift.OpenShiftImageConstants;
import org.kie.cloud.openshift.deployment.Deployment;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.openshift.api.model.DeploymentConfig;
import io.fabric8.openshift.api.model.Template;

/**
 * Cloud settings builder for Kie Server.
 *
 * If any environment variable isn't configured by SettingsBuilder, then default
 * value from application template is used.
 */
public class MySqlDeploymentBuilder implements DeploymentBuilder {

    private Deployment mySqlDeployment;

    public MySqlDeploymentBuilder(Template mySqlTemplate) {
        mySqlDeployment = new Deployment(mySqlTemplate);
    }

    /**
     * Return configured builder with Kie Server user.
     *
     * @param kieServerUser Kie Server username.
     * @param kieServerPwd Kie Server password.
     * @return Builder
     */
    public MySqlDeploymentBuilder withUser(String mySqlUser, String mySqlPwd) {
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

    @Override
    public Deployment build() {
        return mySqlDeployment;
    }

    private void addOrReplaceEnvVar(EnvVar envVar) {
        for (DeploymentConfig deploymentConfig : mySqlDeployment.getDeploymentConfigs()) {
            List<Container> containers = deploymentConfig.getSpec()
                                                         .getTemplate()
                                                         .getSpec()
                                                         .getContainers();
            for (Container container : containers) {
                container.getEnv().removeIf(n -> n.getName().equals(envVar.getName()));
                container.getEnv().add(envVar);
            }
        }
    }
}
