package io.lumify.core.bootstrap.lib;

import io.lumify.core.config.Configuration;
import io.lumify.core.exception.LumifyException;
import io.lumify.core.util.ClassUtil;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;

import java.io.File;
import java.lang.reflect.Method;
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
        loadedLibFiles.add(f);

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
            Method method = ClassUtil.findMethod(classLoaderClass, "addURL", parameters);
            if (method == null) {
                LOGGER.debug("Could not find addURL on classloader: %s", classLoaderClass.getName());
                return false;
            }
            method.setAccessible(true);
            method.invoke(classLoader, f.toURI().toURL());
            LOGGER.debug("added %s to classLoader %s", f.getAbsolutePath(), classLoader.getClass().getName());
            return true;
        } catch (Throwable t) {
            LOGGER.error("Error, could not add URL " + f.getAbsolutePath() + " to classloader: " + classLoaderClass.getName(), t);
            return false;
        }
    }

    public static List<File> getLoadedLibFiles() {
        return loadedLibFiles;
    }
}