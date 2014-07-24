package io.lumify.core.config;

import java.util.*;

public class LumifyResourceBundle extends ResourceBundle {
    private Properties properties;
    private ResourceBundle rootResourceBundle;

    public LumifyResourceBundle(Properties properties, ResourceBundle rootResourceBundle) {
        this.properties = properties;
        this.rootResourceBundle = rootResourceBundle;
    }

    @Override
    protected Object handleGetObject(String key) {
        String value = properties.getProperty(key);
        if (value != null) {
            return value;
        }
        return rootResourceBundle.getString(key);
    }

    @Override
    public Enumeration<String> getKeys() {
        Set keys = new HashSet();
        keys.addAll(properties.keySet());
        keys.addAll(rootResourceBundle.keySet());
        return Collections.enumeration(keys);
    }
}
