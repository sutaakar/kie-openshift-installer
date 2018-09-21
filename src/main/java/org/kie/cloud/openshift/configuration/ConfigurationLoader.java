package org.kie.cloud.openshift.configuration;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ConfigurationLoader {

    private static final String INSTALLER_CONFIGURATION_FILE_NAME = "installer-configuration.properties";

    private static final String KIE_SERVER_IMAGE_STREAM_NAME_PROPERTY = "kie.server.image.stream.name";
    private static final String IMAGE_STREAM_TAG_PROPERTY = "image.stream.tag";
    private static final String IMAGE_STREAM_NAMESPACE_DEFAULT_PROPERTY = "image.stream.namespace.default";

    private static final String KIE_SERVER_MEMORY_LIMIT_PROPERTY = "kie.server.memory.limit";

    private static final String MYSQL_IMAGE_STREAM_NAME_PROPERTY = "mysql.image.stream.name";
    private static final String MYSQL_IMAGE_STREAM_TAG_PROPERTY = "mysql.image.stream.tag";
    private static final String POSTGRESQL_IMAGE_STREAM_NAME_PROPERTY = "postgresql.image.stream.name";
    private static final String POSTGRESQL_IMAGE_STREAM_TAG_PROPERTY = "postgresql.image.stream.tag";

    private static Properties properties = new Properties();

    static {
        InputStream is = null;
        try {
            is = ConfigurationLoader.class.getClassLoader().getResourceAsStream(INSTALLER_CONFIGURATION_FILE_NAME);
            properties.load(is);
            is.close();
        } catch ( IOException e ) {
            throw new RuntimeException(e);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    // TODO log exception, let the original exception to be thrown.
                }
            }
        }
    }

    public static String getKieServerImageStreamName() {
        return properties.getProperty(KIE_SERVER_IMAGE_STREAM_NAME_PROPERTY);
    }

    public static String getImageStreamTag() {
        return properties.getProperty(IMAGE_STREAM_TAG_PROPERTY);
    }

    public static String getImageStreamNamespaceDefault() {
        return properties.getProperty(IMAGE_STREAM_NAMESPACE_DEFAULT_PROPERTY);
    }

    public static String getKieServerMemoryLimit() {
        return properties.getProperty(KIE_SERVER_MEMORY_LIMIT_PROPERTY);
    }

    public static String getMySqlImageStreamName() {
        return properties.getProperty(MYSQL_IMAGE_STREAM_NAME_PROPERTY);
    }

    public static String getMySqlImageStreamTag() {
        return properties.getProperty(MYSQL_IMAGE_STREAM_TAG_PROPERTY);
    }

    public static String getPostgreSqlImageStreamName() {
        return properties.getProperty(POSTGRESQL_IMAGE_STREAM_NAME_PROPERTY);
    }

    public static String getPostgreSqlImageStreamTag() {
        return properties.getProperty(POSTGRESQL_IMAGE_STREAM_TAG_PROPERTY);
    }
}
