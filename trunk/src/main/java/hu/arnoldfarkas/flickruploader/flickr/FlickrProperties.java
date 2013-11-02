package hu.arnoldfarkas.flickruploader.flickr;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlickrProperties {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlickrProperties.class);
    public static final String FILENAME = "flickr.properties";
    private static final Properties PROPERTIES = loadProperties();
    public static final String PROP_API_KEY = "flickr.apikey";
    public static final String PROP_API_SECRET = "flickr.apisecret";
    public static final String PROP_REQUEST_TOKEN = "flickr.requesttoken.token";
    public static final String PROP_REQUEST_SECRET = "flickr.requesttoken.secret";

    private static Properties loadProperties() {
        try {
            Properties p = new Properties();
            InputStream is = new FileInputStream(FILENAME);
            p.load(is);
            return p;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getProperty(String key) {
        return PROPERTIES.getProperty(key);
    }

    public static void save(String key, String value) {
        try {
            Properties p = new Properties();
            InputStream is = new FileInputStream(FILENAME);
            p.load(is);
            p.setProperty(key, value);
            p.store(new FileOutputStream(FILENAME), null);

        } catch (IOException e) {
            LOGGER.error("init properties error", e);
        }
    }
}
