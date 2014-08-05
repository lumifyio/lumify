package io.lumify.core.bootstrap.lib;

import io.lumify.core.config.Configuration;
import io.lumify.core.exception.LumifyException;
import io.lumify.core.util.ClassUtil;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;

import java.io.File;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public abstract class LibLoader {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(LibLoader.class);
    private static List<File> loadedLibFiles = new ArrayList<File>();

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

        // TODO: Make this work for JAR files loaded by remote URL
        loadedLibFiles.add(f);

        try {
            addLibFile(f.toURI().toURL());
        } catch (MalformedURLException e) {
            throw new LumifyException("Could not add file to classloader");
        }
    }

    protected static void addLibFile(URL url) {
        LOGGER.info("adding lib: %s", url);

        ClassLoader classLoader = LibLoader.class.getClassLoader();
        while (classLoader != null) {
            if (tryAddUrl(classLoader, url)) {
                return;
            }
            classLoader = classLoader.getParent();
        }
        if (tryAddUrl(ClassLoader.getSystemClassLoader(), url)) {
            return;
        }

        throw new LumifyException("Could not add URL to classloader");
    }

    private static boolean tryAddUrl(ClassLoader classLoader, URL url) {
        Class<? extends ClassLoader> classLoaderClass = classLoader.getClass();
        try {
            Class[] parameters = new Class[]{URL.class};
            Method method = ClassUtil.findMethod(classLoaderClass, "addURL", parameters);
            if (method == null) {
                LOGGER.debug("Could not find addURL on classloader: %s", classLoaderClass.getName());
                return false;
            }
            method.setAccessible(true);
            method.invoke(classLoader, url);
            LOGGER.debug("added %s to classLoader %s", url, classLoader.getClass().getName());
            return true;
        } catch (Throwable t) {
            LOGGER.error("Error, could not add URL " + url + " to classloader: " + classLoaderClass.getName(), t);
            return false;
        }
    }

    public static List<File> getLoadedLibFiles() {
        return loadedLibFiles;
    }
}
