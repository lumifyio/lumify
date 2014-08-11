package io.lumify.core.util;

import io.lumify.core.config.ConfigurationLoader;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class LumifyLoggerFactory {
    private static final Map<String, LumifyLogger> logMap = new HashMap<String, LumifyLogger>();
    private static boolean initialized = false;
    private static boolean initializing = false;

    public static LumifyLogger getLogger(Class clazz) {
        ensureInitialized();
        return getLogger(clazz.getName());
    }

    private static void ensureInitialized() {
        synchronized (logMap) {
            if (!initialized && !initializing) {
                initializing = true;
                if (System.getProperty("logFileSuffix") == null) {
                    System.setProperty("logFileSuffix", "-" + System.getProperty("user.name") + "-" + ProcessUtil.getPid());
                }
                ConfigurationLoader.configureLog4j();
                initialized = true;
                initializing = false;
            }
        }
    }

    public static LumifyLogger getLogger(String name) {
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
