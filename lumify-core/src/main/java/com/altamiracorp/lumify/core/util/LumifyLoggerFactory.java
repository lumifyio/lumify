package com.altamiracorp.lumify.core.util;

import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class LumifyLoggerFactory {
    private static final Map<String, LumifyLogger> logMap = new HashMap<String, LumifyLogger>();

    public static LumifyLogger getLogger(Class clazz) {
        return getLogger(clazz.getName());
    }

    private static LumifyLogger getLogger(String name) {
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
