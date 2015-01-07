package io.lumify.core.bootstrap;

import com.fasterxml.jackson.module.guice.ObjectMapperModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import io.lumify.core.bootstrap.lib.LibLoader;
import io.lumify.core.config.Configuration;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import io.lumify.core.util.ServiceLoaderUtil;

import java.util.Collection;
import java.util.List;

import static org.securegraph.util.IterableUtils.toList;

public class InjectHelper {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(InjectHelper.class);
    private static Injector injector;

    public static <T> T inject(T o, ModuleMaker moduleMaker) {
        ensureInjectorCreated(moduleMaker);
        inject(o);
        return o;
    }

    public static <T> T inject(T o) {
        if (injector == null) {
            throw new RuntimeException("Could not find injector");
        }
        injector.injectMembers(o);
        return o;
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

    public static <T> Collection<T> getInjectedServices(Class<T> clazz) {
        List<T> workers = toList(ServiceLoaderUtil.load(clazz));
        for (T worker : workers) {
            inject(worker);
        }
        return workers;
    }

    public static void shutdown() {
        injector = null;
    }

    public static boolean hasInjector() {
        return injector != null;
    }

    public static interface ModuleMaker {
        Module createModule();

        Configuration getConfiguration();
    }

    private static void ensureInjectorCreated(ModuleMaker moduleMaker) {
        if (injector == null) {
            LOGGER.info("Loading libs...");
            for (LibLoader libLoader : ServiceLoaderUtil.load(LibLoader.class)) {
                libLoader.loadLibs(moduleMaker.getConfiguration());
            }
            injector = Guice.createInjector(moduleMaker.createModule(), new ObjectMapperModule());
        }
    }
}
