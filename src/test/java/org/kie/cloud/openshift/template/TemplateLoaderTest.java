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

    @Test
    public void testGetMySqlTemplate() {
        TemplateLoader templateLoader = new TemplateLoader(openShiftClient);
        Template mySqlTemplate = templateLoader.loadMySqlTemplate();

        assertThat(mySqlTemplate).isNotNull();
        assertThat(mySqlTemplate.getMetadata().getName()).contains("-mysql");
    }

    @Test
    public void testGetPostgreSqlTemplate() {
        TemplateLoader templateLoader = new TemplateLoader(openShiftClient);
        Template postgreSqlTemplate = templateLoader.loadPostgeSqlTemplate();

        assertThat(postgreSqlTemplate).isNotNull();
        assertThat(postgreSqlTemplate.getMetadata().getName()).contains("-postgresql");
    }
}
