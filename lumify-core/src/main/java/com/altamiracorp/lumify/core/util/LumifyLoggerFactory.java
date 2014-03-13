package com.altamiracorp.lumify.core.util;

import com.altamiracorp.lumify.core.config.Configuration;
import org.apache.log4j.xml.DOMConfigurator;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class LumifyLoggerFactory {
    private static final Map<String, LumifyLogger> logMap = new HashMap<String, LumifyLogger>();
    private static boolean initialized = false;

    public static LumifyLogger getLogger(Class clazz) {
        ensureInitialized();
        return getLogger(clazz.getName());
    }

    private static void ensureInitialized() {
        synchronized (logMap) {
            if (!initialized) {
                String log4jFile = Configuration.CONFIGURATION_LOCATION + "log4j.xml";
                if (!new File(log4jFile).exists()) {
                    throw new RuntimeException("Could not find log4j configuration at \"" + log4jFile + "\". Did you forget to copy \"docs/log4j.xml.sample\" to \"" + log4jFile + "\"");
                }
                DOMConfigurator.configure(log4jFile);
                initialized = true;

                LumifyLogger logger = LumifyLoggerFactory.getLogger(LumifyLoggerFactory.class);
                logger.info("Using log4j.xml: %s", log4jFile);
            }
        }
    }

    private static LumifyLogger getLogger(String name) {
        ensureInitialized();
        synchronized (logMap) {
            LumifyLogger lumifyLogger = logMap.get(name);
            if (lumifyLogger != null) {
                return lumifyLogger;
            }
            lumifyLogger = new LumifyLogger(LoggerFactory.getLogger(name));
            logMap.put(name, lumifyLogger);
            return lumifyLogger;
        }
    }
}
