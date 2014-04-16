package com.altamiracorp.lumify.core.util;

import com.altamiracorp.lumify.core.exception.LumifyException;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.ServiceLoader;

public class ServiceLoaderUtil {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(ServiceLoaderUtil.class);
    private static final String PREFIX = "META-INF/services/";

    public static <T> ServiceLoader<T> load(Class<T> clazz) {
        String fullName = PREFIX + clazz.getName();
        LOGGER.debug("loading services for class %s", fullName);
        try {
            Enumeration<URL> configs = Thread.currentThread().getContextClassLoader().getResources(fullName);
            if (!configs.hasMoreElements()) {
                LOGGER.debug("Could not find any services for %s", fullName);
            } else {
                while (configs.hasMoreElements()) {
                    URL config = configs.nextElement();
                    InputStream in = config.openStream();
                    try {
                        LOGGER.debug("%s:\n%s", config.toString(), IOUtils.toString(in));
                    } finally {
                        in.close();
                    }
                }
            }

            return ServiceLoader.load(clazz);
        } catch (IOException e) {
            throw new LumifyException("Could not load services for class: " + clazz.getName(), e);
        }
    }
}
