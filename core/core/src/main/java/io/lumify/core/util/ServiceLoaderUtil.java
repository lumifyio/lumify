package io.lumify.core.util;

import io.lumify.core.config.Configuration;
import io.lumify.core.exception.LumifyException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.util.*;

/**
 * This class exists to provide much deeper and extensive debugging and logging as
 * opposed to (@see java.util.ServiceLoader)
 */
public class ServiceLoaderUtil {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(ServiceLoaderUtil.class);
    private static final String PREFIX = "META-INF/services/";
    public static final String CONFIG_DISABLE_PREFIX = "disable.";

    public static <T> Iterable<T> load(Class<T> clazz, Configuration configuration) {
        List<T> services = new ArrayList<>();
        String fullName = PREFIX + clazz.getName();
        LOGGER.debug("loading services for class %s", fullName);
        try {
            Enumeration<URL> serviceFiles = Thread.currentThread().getContextClassLoader().getResources(fullName);
            if (!serviceFiles.hasMoreElements()) {
                LOGGER.debug("Could not find any services for %s", fullName);
            } else {
                Set<URL> serviceFilesSet = new HashSet<>();
                while (serviceFiles.hasMoreElements()) {
                    URL serviceFile = serviceFiles.nextElement();
                    serviceFilesSet.add(serviceFile);
                }

                for (URL serviceFile : serviceFilesSet) {
                    services.addAll(ServiceLoaderUtil.<T>loadFile(serviceFile, configuration));
                }
            }

            return services;
        } catch (IOException e) {
            throw new LumifyException("Could not load services for class: " + clazz.getName(), e);
        }
    }

    public static <T> Collection<T> loadFile(URL serviceFile, Configuration configuration) throws IOException {
        List<T> services = new ArrayList<>();
        LOGGER.debug("loadFile(%s)", serviceFile);

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(serviceFile.openStream()))) {
            String className;
            while ((className = reader.readLine()) != null) {
                className = className.trim();
                if (className.length() == 0) {
                    continue;
                }
                if (configuration.getBoolean(CONFIG_DISABLE_PREFIX + className, false)) {
                    LOGGER.info("ignoring class %s because it is disabled in configuration", className);
                    continue;
                }
                services.add(ServiceLoaderUtil.<T>loadClass(serviceFile, className));
            }
        }

        return services;
    }

    public static <T> T loadClass(URL config, String className) {
        try {
            LOGGER.info("Loading %s from %s", className, config.toString());
            Class<? extends T> serviceClass = ClassUtil.forName(className);
            Constructor<? extends T> constructor = serviceClass.getConstructor();
            return constructor.newInstance();
        } catch (Throwable t) {
            String errorMessage = String.format("Failed to load %s from %s", className, config.toString());
            LOGGER.error("%s", errorMessage, t);
            throw new LumifyException(errorMessage, t);
        }
    }
}
