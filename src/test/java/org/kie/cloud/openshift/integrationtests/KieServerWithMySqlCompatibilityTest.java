package org.kie.cloud.openshift.integrationtests;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.fabric8.openshift.api.model.DeploymentConfig;
import io.fabric8.openshift.api.model.DeploymentStrategy;
import io.fabric8.openshift.api.model.Parameter;
import io.fabric8.openshift.api.model.RoleBinding;
import io.fabric8.openshift.api.model.Template;
import org.junit.After;
import org.junit.Test;
import org.kie.cloud.openshift.AbstractCloudTest;
import org.kie.cloud.openshift.KieOpenShiftProvider;
import org.kie.cloud.openshift.deployment.Deployment;
import org.kie.cloud.openshift.deployment.MySqlDeployment;
import org.kie.cloud.openshift.scenario.Scenario;
import org.kie.cloud.openshift.scenario.builder.ScenarioBuilder;

import static org.assertj.core.api.Assertions.assertThat;

public class KieServerWithMySqlCompatibilityTest extends AbstractCloudTest{

    private static final String KIE_SERVER_TEMPLATE_URL = System.getProperty("template.kieserver-mysql.url");

    @After
    public void tearDown() {
        System.clearProperty("kie.server.datasource.jndi");
    }

    @Test
    public void testKieServerMySqlTemplateCompatibility() throws MalformedURLException {
        System.setProperty("kie.server.datasource.jndi", "${KIE_SERVER_PERSISTENCE_DS}");
        MySqlDeployment mySql = KieOpenShiftProvider.createMySqlDeploymentBuilder("${APPLICATION_NAME}-mysql")
                                                    .withImageStreamNamespaceFromProperties()
                                                    .withImageStreamTagFromProperties()
                                                    .withDatabaseUserFromProperties()
                                                    .withDatabaseNameFromProperties()
                                                    .makePersistentFromProperties()
                                                    .build();
        Deployment kieServer = KieOpenShiftProvider.createKieServerDeploymentBuilder("${APPLICATION_NAME}-kieserver")
                                                   .withImageStreamNamespaceFromProperties()
                                                   .withImageStreamNameFromProperties()
                                                   .withImageStreamTagFromProperties()
                                                   .withHttpHostnameFromProperties()
                                                   .withHttpsFromProperties()
                                                   .withHttpsHostnameFromProperties()
                                                   .withClustering()
                                                   .withKieServerUserFromProperties()
                                                   .withContainerMemoryLimitFromProperties()
                                                   .withKieServerClassFilteringFromProperties()
                                                   .withKieMbeansFromProperties()
                                                   .withKieServerBypassAuthUserFromProperties()
                                                   .withKieServerControllerConnectionFromProperties()
                                                   .withKieServerId("${APPLICATION_NAME}-kieserver")
                                                   .withMavenRepo("RHPAMCENTR")
                                                       .withService("${BUSINESS_CENTRAL_MAVEN_SERVICE}", "/maven2/")
                                                       .withAuthentication("${BUSINESS_CENTRAL_MAVEN_USERNAME}", "${BUSINESS_CENTRAL_MAVEN_PASSWORD}")
                                                   .endMavenRepo()
                                                   .withMavenRepo("EXTERNAL")
                                                       .withId("${MAVEN_REPO_ID}")
                                                       .withUrl("${MAVEN_REPO_URL}")
                                                       .withAuthentication("${MAVEN_REPO_USERNAME}", "${MAVEN_REPO_PASSWORD}")
                                                   .endMavenRepo()
                                                   .connectToDatabase(mySql)
                                                   .withTimerServiceDataStoreRefreshIntervalFromProperties()
                                                   .build();
        Scenario scenario = new ScenarioBuilder().withDeployment(kieServer)
                                                 .withDeployment(mySql)
                                                 .withApplicationName("${APPLICATION_NAME}")
                                                 .build();

        List<HasMetadata> geTemplateObjects = scenario.getDeployments().stream().flatMap(d -> d.getObjects().stream()).collect(Collectors.toList());
        List<Parameter> geTemplateParameters = scenario.getDeployments().stream().flatMap(d -> d.getParameters().stream()).collect(Collectors.toList());

        Template template = openShiftClient.templates().load(new URL(KIE_SERVER_TEMPLATE_URL)).get();

        compareParameters(template.getParameters(), geTemplateParameters);

        assertThat(geTemplateObjects).hasSameSizeAs(template.getObjects());
        for (HasMetadata templateObject : template.getObjects()) {
            if (templateObject instanceof DeploymentConfig) {
                compareDeploymentConfigs((DeploymentConfig) templateObject, geTemplateObjects);
            } else if (templateObject instanceof ServiceAccount) {
                compareServiceAccounts((ServiceAccount) templateObject, geTemplateObjects);
            } else if (templateObject instanceof RoleBinding) {
                compareRoleBindings((RoleBinding) templateObject, geTemplateObjects);
            } else if (templateObject instanceof PersistentVolumeClaim) {
                comparePersistentVolumeClaims((PersistentVolumeClaim) templateObject, geTemplateObjects);
            } else {
                compareObjects(templateObject, geTemplateObjects);
            }
        }
    }

    private void compareParameters(List<Parameter> templateParameters, List<Parameter> generatedParameters) {
        Map<String, Parameter> templateParametersMap = templateParameters.stream().collect(Collectors.toMap(Parameter::getName, Function.identity()));

        for (Parameter generatedParameter : generatedParameters) {
            assertThat(templateParametersMap).containsKey(generatedParameter.getName());
            assertThat(generatedParameter).isEqualToComparingFieldByFieldRecursively(templateParametersMap.get(generatedParameter.getName()));
        }
    }

    private void compareObjects(HasMetadata templateObject, List<HasMetadata> generatedObjects) {
        HasMetadata generatedObject = findMatchingObject(generatedObjects, templateObject);
        assertThat(generatedObject).isEqualToComparingFieldByFieldRecursively(templateObject);
    }

    private void comparePersistentVolumeClaims(PersistentVolumeClaim templatePersistentVolumeClaim, List<HasMetadata> generatedPersistentVolumeClaims) {
        Map<String, String> annotations = sanitizeMap(templatePersistentVolumeClaim.getMetadata().getAnnotations());
        templatePersistentVolumeClaim.getMetadata().setAnnotations(annotations);

        PersistentVolumeClaim generatedPersistentVolumeClaim = findMatchingObject(generatedPersistentVolumeClaims, templatePersistentVolumeClaim);

        ResourceRequirements templateResources = templatePersistentVolumeClaim.getSpec().getResources();
        if (templateResources != null) {
            Map<String, Quantity> limits = sanitizeMap(templateResources.getLimits());
            templateResources.setLimits(limits);
        }

        assertThat(generatedPersistentVolumeClaim).isEqualToComparingFieldByFieldRecursively(templatePersistentVolumeClaim);
    }

    private void compareServiceAccounts(ServiceAccount templateServiceAccount, List<HasMetadata> generatedObjects) {
        Map<String, String> annotations = sanitizeMap(templateServiceAccount.getMetadata().getAnnotations());
        templateServiceAccount.getMetadata().setAnnotations(annotations);

        HasMetadata generatedServiceAccount = findMatchingObject(generatedObjects, templateServiceAccount);
        assertThat(generatedServiceAccount).isEqualToComparingFieldByFieldRecursively(templateServiceAccount);
    }

    private void compareRoleBindings(RoleBinding templateRoleBinding, List<HasMetadata> generatedRoleBindings) {
        Map<String, String> annotations = sanitizeMap(templateRoleBinding.getMetadata().getAnnotations());
        templateRoleBinding.getMetadata().setAnnotations(annotations);

        HasMetadata generatedRoleBinding = findMatchingObject(generatedRoleBindings, templateRoleBinding);
        assertThat(generatedRoleBinding).isEqualToComparingFieldByFieldRecursively(templateRoleBinding);
    }

    private void compareDeploymentConfigs(DeploymentConfig templateDeploymentConfig, List<HasMetadata> generatedDeploymentConfigs) {
        DeploymentConfig generatedDeploymentConfig = findMatchingObject(generatedDeploymentConfigs, templateDeploymentConfig);

        compareEnvVars(generatedDeploymentConfig.getSpec().getTemplate().getSpec().getContainers().get(0).getEnv(),
                templateDeploymentConfig.getSpec().getTemplate().getSpec().getContainers().get(0).getEnv());
        // TODO delete this
        templateDeploymentConfig.getSpec().getTemplate().getSpec().getContainers().get(0).setEnv(null);
        generatedDeploymentConfig.getSpec().getTemplate().getSpec().getContainers().get(0).setEnv(null);

        // TODO: Probes have different config for Kie server (should be adjusted?)
        templateDeploymentConfig.getSpec().getTemplate().getSpec().getContainers().get(0).setReadinessProbe(null);
        templateDeploymentConfig.getSpec().getTemplate().getSpec().getContainers().get(0).setLivenessProbe(null);
        generatedDeploymentConfig.getSpec().getTemplate().getSpec().getContainers().get(0).setReadinessProbe(null);
        generatedDeploymentConfig.getSpec().getTemplate().getSpec().getContainers().get(0).setLivenessProbe(null);

        ResourceRequirements templateResources = templateDeploymentConfig.getSpec().getTemplate().getSpec().getContainers().get(0).getResources();
        if (templateResources != null) {
            Map<String, Quantity> requests = sanitizeMap(templateResources.getRequests());
            templateResources.setRequests(requests);
        }

        sanitizeDeploymentStrategy(templateDeploymentConfig.getSpec().getStrategy());
        Map<String, String> nodeSelector = sanitizeMap(templateDeploymentConfig.getSpec().getTemplate().getSpec().getNodeSelector());
        templateDeploymentConfig.getSpec().getTemplate().getSpec().setNodeSelector(nodeSelector);

        // Adjust annotations
        Map<String, String> annotations = sanitizeMap(templateDeploymentConfig.getSpec().getTemplate().getMetadata().getAnnotations());
        templateDeploymentConfig.getSpec().getTemplate().getMetadata().setAnnotations(annotations);

        assertThat(generatedDeploymentConfig).isEqualToComparingFieldByFieldRecursively(templateDeploymentConfig);
    }

    private void compareEnvVars(List<EnvVar> generatedEnvs, List<EnvVar> templateEnvs) {
        Map<String, EnvVar> templateMap = templateEnvs.stream().collect(Collectors.toMap(e -> e.getName(), Function.identity()));

        for (EnvVar generatedEnv : generatedEnvs) {
            assertThat(templateMap).containsKey(generatedEnv.getName());
            assertThat(generatedEnv).isEqualToComparingFieldByFieldRecursively(templateMap.get(generatedEnv.getName()));
        }

        // TODO adjust this or delete?
        Map<String, EnvVar> generatedMap = generatedEnvs.stream().collect(Collectors.toMap(e -> e.getName(), Function.identity()));
        for (EnvVar templateEnv : templateEnvs) {
            // Skip admin user, as it doesn't have much sense for template with Kie server and DB
            if (templateEnv.getName().startsWith("KIE_ADMIN_")) {
                continue;
            }
            // Skip this for now as it isn't a critical functionality, will be implemented later
            if (templateEnv.getName().equals("KIE_SERVER_USE_SECURE_ROUTE_NAME")) {
                continue;
            }
            // Skip this for now
            if (templateEnv.getName().equals("KIE_SERVER_CONTAINER_DEPLOYMENT")) {
                continue;
            }
            // Skip this for now
            if (templateEnv.getName().startsWith("KIE_SERVER_ROUTER_")) {
                continue;
            }
            // Skip this for now
            if (templateEnv.getName().equals("KIE_SERVER_MGMT_DISABLED")) {
                continue;
            }
            // Skip this for now
            if (templateEnv.getName().equals("KIE_SERVER_STARTUP_STRATEGY")) {
                continue;
            }
            // Skip this for now
            if (templateEnv.getName().startsWith("SSO_")) {
                continue;
            }
            // Skip this for now
            if (templateEnv.getName().equals("HOSTNAME_HTTP") || templateEnv.getName().equals("HOSTNAME_HTTPS")) {
                continue;
            }
            // Skip this for now
            if (templateEnv.getName().startsWith("AUTH_LDAP_")) {
                continue;
            }
            // Skip this for now
            if (templateEnv.getName().startsWith("AUTH_ROLE_MAPPER_")) {
                continue;
            }
            assertThat(generatedMap).containsKey(templateEnv.getName());
            assertThat(templateEnv).isEqualToComparingFieldByFieldRecursively(generatedMap.get(templateEnv.getName()));
        }
    }

    @SuppressWarnings("unchecked")
    private <T extends HasMetadata, U extends HasMetadata> T findMatchingObject(List<U> generatedObjects, T templateObject) {
        return generatedObjects.stream()
                               .filter(o -> o.getMetadata().getName().equals(templateObject.getMetadata().getName()))
                               .filter(o -> o.getClass().isInstance(templateObject))
                               .map(o -> (T) o)
                               .findAny()
                               .orElseThrow(() -> new RuntimeException("Object with name " + templateObject.getMetadata().getName() + " and class " + templateObject.getClass().getName() + " not found among generated objects."));
    }

    private void sanitizeDeploymentStrategy(DeploymentStrategy deploymentStrategy) {
        deploymentStrategy.setAnnotations(sanitizeMap(deploymentStrategy.getAnnotations()));
        deploymentStrategy.setLabels(sanitizeMap(deploymentStrategy.getLabels()));
    }

    private <T> Map<String, T> sanitizeMap(Map<String, T> input) {
        if (input == null) {
            return new HashMap<>();
        }
        return input;
    }
}
