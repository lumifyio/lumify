package com.altamiracorp.lumify.core.bootstrap.lib;

import com.altamiracorp.lumify.core.config.Configuration;
import com.altamiracorp.lumify.core.exception.LumifyException;
import com.altamiracorp.lumify.core.util.LumifyLogger;
import com.altamiracorp.lumify.core.util.LumifyLoggerFactory;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;

public abstract class LibLoader {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(LibLoader.class);

    public abstract void loadLibs(Configuration configuration);

    protected static void addLibDirectory(File directory) {
        if (!directory.exists()) {
            throw new LumifyException(String.format("Could not add lib directory %s. Directory not found.", directory.getAbsolutePath()));
        }
        if (!directory.isDirectory()) {
            throw new LumifyException(String.format("Could not add lib directory %s. Not a directory.", directory.getAbsolutePath()));
        }
        File[] files = directory.listFiles();
        if (files == null) {
            throw new LumifyException(String.format("Could not list files of directory %s", directory.getAbsolutePath()));
        }
        for (File f : files) {
            if (f.getName().startsWith(".") || f.isHidden()) {
                continue;
            }
            if (f.isDirectory()) {
                addLibDirectory(f);
                continue;
            }

            if (f.getName().toLowerCase().endsWith(".jar")) {
                addLibFile(f);
            }
        }
    }

    protected static void addLibFile(File f) {
        if (!f.exists()) {
            throw new LumifyException(String.format("Could not add lib %s. File not found.", f.getAbsolutePath()));
        }
        if (!f.isFile()) {
            throw new LumifyException(String.format("Could not add lib %s. Not a file.", f.getAbsolutePath()));
        }
        LOGGER.info("adding lib: %s", f.getAbsolutePath());

        ClassLoader classLoader = LibLoader.class.getClassLoader();
        while (classLoader != null) {
            if (tryAddUrl(classLoader, f)) {
                return;
            }
            classLoader = classLoader.getParent();
        }
        if (tryAddUrl(ClassLoader.getSystemClassLoader(), f)) {
            return;
        }
        throw new LumifyException("Could not add file to classloader");
    }

    private static boolean tryAddUrl(ClassLoader classLoader, File f) {
        Class<? extends ClassLoader> classLoaderClass = classLoader.getClass();
        try {
            Class[] parameters = new Class[]{URL.class};
            Method method = findMethod(classLoaderClass, "addURL", parameters);
            if (method == null) {
                LOGGER.debug("Could not find addURL on classloader: %s", classLoaderClass.getName());
                return false;
            }
            method.setAccessible(true);
            method.invoke(classLoader, f.toURI().toURL());
            LOGGER.debug("added %s to classloader %s", f.getAbsolutePath(), classLoader.getClass().getName());
            return true;
        } catch (Throwable t) {
            LOGGER.error("Error, could not add URL " + f.getAbsolutePath() + " to classloader: " + classLoaderClass.getName(), t);
            return false;
        }
    }

    private static Method findMethod(Class clazz, String methodName, Class[] parameters) {
        while (clazz != null) {
            try {
                return clazz.getDeclaredMethod(methodName, parameters);
            } catch (NoSuchMethodException e) {
                clazz = clazz.getSuperclass();
            }
        }
        return null;
    }
}
