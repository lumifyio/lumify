package io.lumify.core.util;

import io.lumify.core.exception.LumifyException;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;

public class ServiceLoaderUtil {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(ServiceLoaderUtil.class);
    private static final String PREFIX = "META-INF/services/";

    public static <T> Iterable<T> load(Class<T> clazz) {
        List<T> services = new ArrayList<T>();
        String fullName = PREFIX + clazz.getName();
        LOGGER.debug("loading services for class %s", fullName);
        try {
            Enumeration<URL> configs = Thread.currentThread().getContextClassLoader().getResources(fullName);
            if (!configs.hasMoreElements()) {
                LOGGER.debug("Could not find any services for %s", fullName);
            } else {
                while (configs.hasMoreElements()) {
                    URL config = configs.nextElement();
                    services.addAll(ServiceLoaderUtil.<T>loadFile(config));
                }
            }

            return services;
        } catch (IOException e) {
            throw new LumifyException("Could not load services for class: " + clazz.getName(), e);
        }
    }

    public static <T> Collection<T> loadFile(URL config) throws IOException {
        List<T> services = new ArrayList<T>();
        InputStream in = config.openStream();
        try {
            String fileContents = IOUtils.toString(in);
            LOGGER.debug("%s:\n%s", config.toString(), fileContents);
            String[] classNames = fileContents.split("\n");
            for (String className : classNames) {
                if (className.trim().length() == 0) {
                    continue;
                }
                services.add(ServiceLoaderUtil.<T>loadClass(config, className));
            }
        } finally {
            in.close();
        }
        return services;
    }

    public static <T> T loadClass(URL config, String className) {
        try {
            LOGGER.debug("Loading %s from %s", className, config.toString());
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
