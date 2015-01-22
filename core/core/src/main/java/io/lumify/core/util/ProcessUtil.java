package io.lumify.core.util;

import java.lang.management.ManagementFactory;

public class ProcessUtil {
    public static String getPid() {
        String name = ManagementFactory.getRuntimeMXBean().getName();
        int i = name.indexOf('@');
        if (i > 0) {
            name = name.substring(0, i);
        }
        return name;
    }

    public static boolean isWindows() {
        String os = System.getProperty("os.name");
        if (os == null) {
            return false;
        }
        if (os.toLowerCase().startsWith("windows")) {
            return true;
        }
        return false;
    }
}
