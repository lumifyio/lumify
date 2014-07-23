package io.lumify.core.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class LumifyResourceBundle extends ResourceBundle {
    private Properties properties;
    ResourceBundle parentResourceBundle;

    public LumifyResourceBundle(InputStream inputStream, ResourceBundle parentResourceBundle) throws IOException {
        properties = new Properties();
        properties.load(inputStream);
        this.parentResourceBundle = parentResourceBundle;
    }

    @Override
    protected Object handleGetObject(String key) {
        String value = properties.getProperty(key);
        if (value != null) {
            return value;
        } else {
            return parentResourceBundle.getString(key);
        }
    }

    @Override
    public Enumeration<String> getKeys() {
        List<String> combinedKeys = new ArrayList<String>();

        Enumeration keys = properties.propertyNames();
        while (keys.hasMoreElements()) {
            combinedKeys.add((String) keys.nextElement());
        }

        Enumeration parentKeys = parentResourceBundle.getKeys();
        while (parentKeys.hasMoreElements()) {
            combinedKeys.add((String) parentKeys.nextElement());
        }

        return Collections.enumeration(combinedKeys);
    }
}
