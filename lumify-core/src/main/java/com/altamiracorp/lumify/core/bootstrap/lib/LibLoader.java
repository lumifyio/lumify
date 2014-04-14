package com.altamiracorp.lumify.core.bootstrap.lib;

import com.altamiracorp.lumify.core.config.Configuration;
import com.altamiracorp.lumify.core.exception.LumifyException;
import com.altamiracorp.lumify.core.util.LumifyLogger;
import com.altamiracorp.lumify.core.util.LumifyLoggerFactory;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

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

        ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
        if (!(systemClassLoader instanceof URLClassLoader)) {
            throw new LumifyException("Could not modify system class loader. Expected " + URLClassLoader.class.getName() + " found " + systemClassLoader.getClass().getName());
        }
        Class urlClassLoaderClass = URLClassLoader.class;
        try {
            Class[] parameters = new Class[]{URL.class};
            Method method = urlClassLoaderClass.getDeclaredMethod("addURL", parameters);
            method.setAccessible(true);
            method.invoke(systemClassLoader, f.toURI().toURL());
        } catch (Throwable t) {
            throw new LumifyException("Error, could not add URL " + f.getAbsolutePath() + " to system classloader", t);
        }
    }
}
