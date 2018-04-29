package org.kie.cloud.openshift.template;

import static org.assertj.core.api.Assertions.assertThat;

import io.fabric8.openshift.api.model.Template;
import org.junit.Test;
import org.kie.cloud.openshift.AbstractCloudTest;

public class TemplateLoaderTest extends AbstractCloudTest{

    @Test
    public void testGetKieServerTemplate() {
        TemplateLoader templateLoader = new TemplateLoader(openShiftClient);
        Template kieServerTemplate = templateLoader.loadKieServerTemplate();

        assertThat(kieServerTemplate).isNotNull();
        assertThat(kieServerTemplate.getMetadata().getName()).contains("-kieserver");
    }
}
