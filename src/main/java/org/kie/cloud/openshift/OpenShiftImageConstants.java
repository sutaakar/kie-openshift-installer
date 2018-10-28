/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/

package org.kie.cloud.openshift;

public class OpenShiftImageConstants {

    public static final String KIE_ADMIN_USER = "KIE_ADMIN_USER";
    public static final String KIE_ADMIN_PWD = "KIE_ADMIN_PWD";

    public static final String KIE_SERVER_USER = "KIE_SERVER_USER";
    public static final String KIE_SERVER_PWD = "KIE_SERVER_PWD";
    public static final String KIE_SERVER_HOST = "KIE_SERVER_HOST";
    public static final String KIE_SERVER_PORT = "KIE_SERVER_PORT";
    public static final String KIE_SERVER_ID = "KIE_SERVER_ID";

    public static final String KIE_SERVER_PERSISTENCE_DIALECT = "KIE_SERVER_PERSISTENCE_DIALECT";
    public static final String KIE_SERVER_PERSISTENCE_DS = "KIE_SERVER_PERSISTENCE_DS";
    public static final String KIE_SERVER_PERSISTENCE_TM = "KIE_SERVER_PERSISTENCE_TM";

    public static final String KIE_SERVER_CONTROLLER_PROTOCOL = "KIE_SERVER_CONTROLLER_PROTOCOL";
    public static final String KIE_SERVER_CONTROLLER_HOST = "KIE_SERVER_CONTROLLER_HOST";
    public static final String KIE_SERVER_CONTROLLER_PORT = "KIE_SERVER_CONTROLLER_PORT";
    public static final String KIE_SERVER_CONTROLLER_USER = "KIE_SERVER_CONTROLLER_USER";
    public static final String KIE_SERVER_CONTROLLER_PWD = "KIE_SERVER_CONTROLLER_PWD";
    public static final String KIE_SERVER_CONTROLLER_SERVICE = "KIE_SERVER_CONTROLLER_SERVICE";

    public static final String KIE_SERVER_ROUTER_ID = "KIE_SERVER_ROUTER_ID";
    public static final String KIE_SERVER_ROUTER_NAME = "KIE_SERVER_ROUTER_NAME";
    public static final String KIE_SERVER_ROUTER_HOST = "KIE_SERVER_ROUTER_HOST";
    public static final String KIE_SERVER_ROUTER_PORT = "KIE_SERVER_ROUTER_PORT";
    public static final String KIE_SERVER_ROUTER_SERVICE = "KIE_SERVER_ROUTER_SERVICE";
    public static final String KIE_SERVER_ROUTER_URL_EXTERNAL = "KIE_SERVER_ROUTER_URL_EXTERNAL";

    public static final String KIE_SERVER_BYPASS_AUTH_USER = "KIE_SERVER_BYPASS_AUTH_USER";

    public static final String KIE_SERVER_HTTPS_SECRET = "KIE_SERVER_HTTPS_SECRET";
    public static final String KIE_SERVER_HTTPS_KEYSTORE = "KIE_SERVER_HTTPS_KEYSTORE";
    public static final String KIE_SERVER_HTTPS_NAME = "KIE_SERVER_HTTPS_NAME";
    public static final String KIE_SERVER_HTTPS_PASSWORD = "KIE_SERVER_HTTPS_PASSWORD";

    public static final String HTTPS_KEYSTORE_DIR = "HTTPS_KEYSTORE_DIR";
    public static final String HTTPS_KEYSTORE = "HTTPS_KEYSTORE";
    public static final String HTTPS_NAME = "HTTPS_NAME";
    public static final String HTTPS_PASSWORD = "HTTPS_PASSWORD";

    public static final String MAVEN_REPO_URL = "MAVEN_REPO_URL";
    public static final String MAVEN_REPO_SERVICE = "MAVEN_REPO_SERVICE";
    public static final String MAVEN_REPO_PATH = "MAVEN_REPO_PATH";
    public static final String MAVEN_REPO_USERNAME = "MAVEN_REPO_USERNAME";
    public static final String MAVEN_REPO_PASSWORD = "MAVEN_REPO_PASSWORD";

    public static final String IMAGE_STREAM_NAMESPACE = "IMAGE_STREAM_NAMESPACE";

    public static final String DBE_SERVICE_HOST = "DBE_SERVICE_HOST";
    public static final String DBE_SERVICE_PORT = "DBE_SERVICE_PORT";
    public static final String DBE_DRIVER = "DBE_DRIVER";
    public static final String DBE_DATABASE = "DBE_DATABASE";
    public static final String DBE_USERNAME = "DBE_USERNAME";
    public static final String DBE_PASSWORD = "DBE_PASSWORD";

    public static final String BUSINESS_CENTRAL_HOSTNAME_HTTP = "BUSINESS_CENTRAL_HOSTNAME_HTTP";
    public static final String BUSINESS_CENTRAL_HOSTNAME_HTTPS = "BUSINESS_CENTRAL_HOSTNAME_HTTPS";
    public static final String EXECUTION_SERVER_HOSTNAME_HTTP = "EXECUTION_SERVER_HOSTNAME_HTTP";
    public static final String EXECUTION_SERVER_HOSTNAME_HTTPS = "EXECUTION_SERVER_HOSTNAME_HTTPS";
    public static final String SMART_ROUTER_HOSTNAME_HTTP = "SMART_ROUTER_HOSTNAME_HTTP";

    public static final String KIE_SERVER_CONTAINER_DEPLOYMENT = "KIE_SERVER_CONTAINER_DEPLOYMENT";

    public static final String KIE_SERVER_SYNC_DEPLOY = "KIE_SERVER_SYNC_DEPLOY";

    public static final String DROOLS_SERVER_FILTER_CLASSES = "DROOLS_SERVER_FILTER_CLASSES";

    public static final String SOURCE_REPOSITORY_URL = "SOURCE_REPOSITORY_URL";
    public static final String SOURCE_REPOSITORY_REF = "SOURCE_REPOSITORY_REF";
    public static final String CONTEXT_DIR = "CONTEXT_DIR";
    public static final String ARTIFACT_DIR = "ARTIFACT_DIR";

    public static final String SECRET_NAME = "SECRET_NAME";

    public static final String MYSQL_USER = "MYSQL_USER";
    public static final String MYSQL_PASSWORD = "MYSQL_PASSWORD";
    public static final String MYSQL_DATABASE = "MYSQL_DATABASE";

    public static final String POSTGRESQL_USER = "POSTGRESQL_USER";
    public static final String POSTGRESQL_PASSWORD = "POSTGRESQL_PASSWORD";
    public static final String POSTGRESQL_DATABASE = "POSTGRESQL_DATABASE";
    public static final String POSTGRESQL_MAX_PREPARED_TRANSACTIONS = "POSTGRESQL_MAX_PREPARED_TRANSACTIONS";

    public static final String JGROUPS_PING_PROTOCOL = "JGROUPS_PING_PROTOCOL";
    public static final String OPENSHIFT_DNS_PING_SERVICE_NAME = "OPENSHIFT_DNS_PING_SERVICE_NAME";
    public static final String OPENSHIFT_DNS_PING_SERVICE_PORT = "OPENSHIFT_DNS_PING_SERVICE_PORT";

    public static final String DATASOURCES = "DATASOURCES";
    public static final String DATASOURCES_KIE = "KIE";
    public static final String KIE_DATABASE = "KIE_DATABASE";
    public static final String KIE_JNDI = "KIE_JNDI";
    public static final String KIE_DRIVER = "KIE_DRIVER";
    public static final String KIE_JTA = "KIE_JTA";
    // TODO: delete this?
    public static final String KIE_TX_ISOLATION = "KIE_TX_ISOLATION";
    public static final String KIE_USERNAME = "KIE_USERNAME";
    public static final String KIE_PASSWORD = "KIE_PASSWORD";
    public static final String KIE_SERVICE_HOST = "KIE_SERVICE_HOST";
    public static final String KIE_SERVICE_PORT = "KIE_SERVICE_PORT";
    public static final String TIMER_SERVICE_DATA_STORE = "TIMER_SERVICE_DATA_STORE";
    public static final String TIMER_SERVICE_DATA_STORE_REFRESH_INTERVAL = "TIMER_SERVICE_DATA_STORE_REFRESH_INTERVAL";
}
