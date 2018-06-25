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
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimBuilder;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.fabric8.openshift.api.model.DeploymentConfig;
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
        addOrReplaceEnvVar(OpenShiftImageConstants.MYSQL_USER, "mySqlUser");
        addOrReplaceEnvVar(OpenShiftImageConstants.MYSQL_PASSWORD, "mySqlPwd");
        addOrReplaceEnvVar(OpenShiftImageConstants.MYSQL_DATABASE, "mysqlDb");
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

    public MySqlDeploymentBuilder makePersistent(String persistentVolumeClaimSize) {
        for (DeploymentConfig mySqlDeploymentConfig : getDeployment().getDeploymentConfigs()) {
            PodSpec mySqlSpec = mySqlDeploymentConfig.getSpec().getTemplate().getSpec();
            List<Container> mySqlContainers = mySqlSpec.getContainers();

            if (mySqlContainers.size() > 1) {
                throw new RuntimeException("Corrent configuration doesn't support multiple containers in Deployment config.");
            }

            // TODO application name just has to be set somehow
            VolumeMount mySqlVolumeMount = new VolumeMountBuilder().withName("${APPLICATION_NAME}-mysql-pvol")
                                                                   .withMountPath("/var/lib/mysql/data")
                                                                   .build();
            mySqlContainers.get(0).getVolumeMounts().add(mySqlVolumeMount);

            Volume mySqlVolume = new VolumeBuilder().withName("${APPLICATION_NAME}-mysql-pvol")
                                                    .withNewPersistentVolumeClaim()
                                                        .withClaimName("${APPLICATION_NAME}-mysql-claim")
                                                    .endPersistentVolumeClaim()
                                                    .build();
            mySqlSpec.getVolumes().add(mySqlVolume);

            PersistentVolumeClaim mySqlPersistentVolumeClaim = new PersistentVolumeClaimBuilder().withKind("PersistentVolumeClaim")
                                                                                                 .withNewMetadata()
                                                                                                     .withName("${APPLICATION_NAME}-mysql-claim")
                                                                                                 .endMetadata()
                                                                                                 .withNewSpec()
                                                                                                     .withAccessModes("ReadWriteOnce")
                                                                                                     .withNewResources()
                                                                                                         .withRequests(Collections.singletonMap("storage", new Quantity(persistentVolumeClaimSize)))
                                                                                                     .endResources()
                                                                                                 .endSpec()
                                                                                                 .build();
            List<HasMetadata> objects = getDeployment().geTemplate().getObjects();
            objects.add(mySqlPersistentVolumeClaim);
            getDeployment().geTemplate().setObjects(objects);
        }
        return this;
    }
}
