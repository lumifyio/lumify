package io.lumify.core.config;

import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;

public class LumifyResourceBundleManager {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(LumifyResourceBundleManager.class);
    public static final String RESOURCE_BUNDLE_BASE_NAME = "MessageBundle";
    private Properties unlocalizedProperties;
    private Map<Locale, Properties> localizedProperties;

    public LumifyResourceBundleManager() {
        unlocalizedProperties = new Properties();
        localizedProperties = new HashMap<Locale, Properties>();
    }

    public void register(InputStream inputStream) throws IOException {
        unlocalizedProperties.load(new InputStreamReader(inputStream, "UTF-8"));
    }

    public void register(InputStream inputStream, Locale locale) throws IOException {
        Properties properties = localizedProperties.get(locale);
        if (properties == null) {
            properties = new Properties();
            localizedProperties.put(locale, properties);
        }
        properties.load(new InputStreamReader(inputStream, "UTF-8"));
    }

    public ResourceBundle getBundle() {
        Locale defaultLocale = Locale.getDefault();
        LOGGER.info("returning a bundle configured for the default locale: %s ", defaultLocale);
        return createBundle(defaultLocale);
    }

    public ResourceBundle getBundle(Locale locale) {
        LOGGER.info("returning a bundle configured for locale: %s ", locale);
        return createBundle(locale);
    }

    private ResourceBundle createBundle(Locale locale) {
        Properties properties = new Properties();
        properties.putAll(unlocalizedProperties);
        properties.putAll(getLocaleProperties(locale));
        return new LumifyResourceBundle(properties, getRootBundle(locale));
    }

    private Properties getLocaleProperties(Locale locale) {
        Properties properties = new Properties();

        Properties languageProperties = localizedProperties.get(new Locale(locale.getLanguage()));
        if (languageProperties != null) {
            properties.putAll(languageProperties);
        }

        Properties languageCountryProperties = localizedProperties.get(new Locale(locale.getLanguage(), locale.getCountry()));
        if (languageCountryProperties != null) {
            properties.putAll(languageCountryProperties);
        }

        Properties languageCountryVariantProperties = localizedProperties.get(new Locale(locale.getLanguage(), locale.getCountry(), locale.getVariant()));
        if (languageCountryVariantProperties != null) {
            properties.putAll(languageCountryVariantProperties);
        }

        return properties;
    }

    private ResourceBundle getRootBundle(Locale locale) {
        return ResourceBundle.getBundle(RESOURCE_BUNDLE_BASE_NAME, locale, new UTF8PropertiesControl());
    }

    /**
     * use an InputStreamReader to allow for UTF-8 values in property file bundles, otherwise use the base class implementation
     */
    private class UTF8PropertiesControl extends ResourceBundle.Control {
        public ResourceBundle newBundle(String baseName, Locale locale, String format, ClassLoader loader, boolean reload)
                throws IllegalAccessException, InstantiationException, IOException {
            if (format.equals("java.properties")) {
                String resourceName = toResourceName(toBundleName(baseName, locale), "properties");
                InputStream inputStream = null;
                if (reload) {
                    URL url = loader.getResource(resourceName);
                    if (url != null) {
                        URLConnection urlConnection = url.openConnection();
                        if (urlConnection != null) {
                            urlConnection.setUseCaches(false);
                            inputStream = urlConnection.getInputStream();
                        }
                    }
                } else {
                    inputStream = loader.getResourceAsStream(resourceName);
                }

                if (inputStream != null) {
                    try {
                        return new PropertyResourceBundle(new InputStreamReader(inputStream, "UTF-8"));
                    } finally {
                        inputStream.close();
                    }
                }
            }
            return super.newBundle(baseName, locale, format, loader, reload);
        }
    }
}
