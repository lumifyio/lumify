package com.altamiracorp.lumify.core.bootstrap;

import com.altamiracorp.lumify.core.bootstrap.lib.LibLoader;
import com.altamiracorp.lumify.core.config.Configuration;
import com.altamiracorp.lumify.core.util.LumifyLogger;
import com.altamiracorp.lumify.core.util.LumifyLoggerFactory;
import com.fasterxml.jackson.module.guice.ObjectMapperModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;

import java.util.ServiceLoader;

public class InjectHelper {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(InjectHelper.class);
    private static Injector injector;

    public static void inject(Object o, ModuleMaker moduleMaker) {
        ensureInjectorCreated(moduleMaker);
        inject(o);
    }

    public static void inject(Object o) {
        if (injector == null) {
            throw new RuntimeException("Could not find injector");
        }
        injector.injectMembers(o);
    }

    public static Injector getInjector() {
        return injector;
    }

    public static <T> T getInstance(Class<T> clazz, ModuleMaker moduleMaker) {
        ensureInjectorCreated(moduleMaker);
        return injector.getInstance(clazz);
    }

    public static <T> T getInstance(Class<? extends T> clazz) {
        if (injector == null) {
            throw new RuntimeException("Could not find injector");
        }
        return injector.getInstance(clazz);
    }

    public static interface ModuleMaker {
        Module createModule();

        Configuration getConfiguration();
    }

    private static void ensureInjectorCreated(ModuleMaker moduleMaker) {
        if (injector == null) {
            for (LibLoader libLoader : ServiceLoader.load(LibLoader.class)) {
                libLoader.loadLibs(moduleMaker.getConfiguration());
            }
            injector = Guice.createInjector(moduleMaker.createModule(), new ObjectMapperModule());
        }
    }
}
