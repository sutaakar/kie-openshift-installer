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
}
