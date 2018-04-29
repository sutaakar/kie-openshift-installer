package org.kie.cloud.openshift.template;

import java.net.URL;

import io.fabric8.openshift.api.model.Template;
import io.fabric8.openshift.client.OpenShiftClient;
import org.kie.cloud.openshift.KieOpenShiftProvider;

public class TemplateLoader {

    private static final String KIE_SERVER_TEMPLATE_RESOURCE_PATH = "/rhpam70-kieserver.yaml";

    private OpenShiftClient openShiftClient;

    public TemplateLoader(OpenShiftClient openShiftClient) {
        this.openShiftClient = openShiftClient;
    }

    public Template loadKieServerTemplate() {
        URL kieServerTemplateUrl = KieOpenShiftProvider.class.getResource(KIE_SERVER_TEMPLATE_RESOURCE_PATH);
        return openShiftClient.templates().load(kieServerTemplateUrl).get();
    }
}
