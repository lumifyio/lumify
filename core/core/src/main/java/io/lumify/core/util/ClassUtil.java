package io.lumify.core.util;

import io.lumify.core.exception.LumifyException;

import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

public class ClassUtil {
    @SuppressWarnings("unchecked")
    public static <T> Class<? extends T> forName(String className) {
        try {
            return (Class<? extends T>) Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new LumifyException("Could not load class " + className);
        }
    }

    public static Method findMethod(Class clazz, String methodName, Class[] parameters) {
        while (clazz != null) {
            try {
                return getDeclaredMethod(clazz, methodName, parameters);
            } catch (NoSuchMethodException e) {
                clazz = clazz.getSuperclass();
            }
        }
        return null;
    }

    public static void logClasspath(ClassLoader classLoader) {
        logClasspath(classLoader, null);
    }

    public static void logClasspath(ClassLoader classLoader, LumifyLogger lumifyLogger) {
        if (classLoader instanceof URLClassLoader) {
            log(lumifyLogger, classLoader.getClass().getName());
            for (URL url : ((URLClassLoader) classLoader).getURLs()) {
                log(lumifyLogger, url.toString());
            }
        } else {
            log(lumifyLogger, "unable to enumerate entries for " + classLoader.getClass().getName());
        }

        ClassLoader parentClassLoader = classLoader.getParent();
        if (parentClassLoader != null) {
            logClasspath(parentClassLoader, lumifyLogger);
        }
    }

    @SuppressWarnings("unchecked")
    private static Method getDeclaredMethod(Class clazz, String methodName, Class[] parameters) throws NoSuchMethodException {
        return clazz.getDeclaredMethod(methodName, parameters);
    }

    private static void log(LumifyLogger lumifyLogger, String string) {
        if (lumifyLogger != null) {
            lumifyLogger.debug(string);
        } else {
            System.out.println(string);
        }
    }
}
