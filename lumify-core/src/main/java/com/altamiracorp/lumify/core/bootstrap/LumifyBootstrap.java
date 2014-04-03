/*
 * Copyright 2014 Altamira Corporation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.altamiracorp.lumify.core.bootstrap;

import com.altamiracorp.bigtable.model.ModelSession;
import com.altamiracorp.lumify.core.config.Configuration;
import com.altamiracorp.lumify.core.contentType.MimeTypeMapper;
import com.altamiracorp.lumify.core.exception.LumifyException;
import com.altamiracorp.lumify.core.fs.FileSystemSession;
import com.altamiracorp.lumify.core.metrics.JmxMetricsManager;
import com.altamiracorp.lumify.core.metrics.MetricsManager;
import com.altamiracorp.lumify.core.model.user.AccumuloAuthorizationRepository;
import com.altamiracorp.lumify.core.model.user.AuthorizationRepository;
import com.altamiracorp.lumify.core.model.user.UserRepository;
import com.altamiracorp.lumify.core.model.workQueue.WorkQueueRepository;
import com.altamiracorp.lumify.core.model.workspace.WorkspaceRepository;
import com.altamiracorp.lumify.core.security.VisibilityTranslator;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.core.util.LumifyLogger;
import com.altamiracorp.lumify.core.util.LumifyLoggerFactory;
import com.altamiracorp.lumify.core.version.VersionService;
import com.altamiracorp.lumify.core.version.VersionServiceMXBean;
import com.altamiracorp.securegraph.Graph;
import com.google.inject.*;
import com.netflix.curator.RetryPolicy;
import com.netflix.curator.framework.CuratorFramework;
import com.netflix.curator.framework.CuratorFrameworkFactory;
import com.netflix.curator.retry.ExponentialBackoffRetry;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

/**
 * The LumifyBootstrap is a Guice Module that configures itself by
 * discovering all available implementations of BootstrapBindingProvider
 * and invoking the addBindings() method.  If any discovered provider
 * cannot be instantiated, configuration of the Bootstrap Module will
 * fail and halt application initialization by throwing a BootstrapException.
 */
public class LumifyBootstrap extends AbstractModule {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(LumifyBootstrap.class);

    private static LumifyBootstrap lumifyBootstrap;

    public synchronized static LumifyBootstrap bootstrap(final Configuration configuration) {
        if (lumifyBootstrap == null) {
            LOGGER.debug("Initializing LumifyBootstrap with Configuration:\n%s", configuration);
            lumifyBootstrap = new LumifyBootstrap(configuration);
        }
        return lumifyBootstrap;
    }

    /**
     * Get a ModuleMaker that will return the LumifyBootstrap, initializing it with
     * the provided Configuration if it has not already been created.
     *
     * @param configuration the Lumify configuration
     * @return a ModuleMaker for use with the InjectHelper
     */
    public static InjectHelper.ModuleMaker bootstrapModuleMaker(final Configuration configuration) {
        return new InjectHelper.ModuleMaker() {
            @Override
            public Module createModule() {
                return LumifyBootstrap.bootstrap(configuration);
            }
        };
    }

    /**
     * The Lumify Configuration.
     */
    private final Configuration configuration;

    /**
     * Create a LumifyBootstrap with the provided Configuration.
     *
     * @param config the configuration for this bootstrap
     */
    private LumifyBootstrap(final Configuration config) {
        this.configuration = config;
    }

    @Override
    protected void configure() {
        LOGGER.info("Configuring LumifyBootstrap.");

        MetricsManager metricsManager = new JmxMetricsManager();

        bind(AuthorizationRepository.class).to(AccumuloAuthorizationRepository.class);
        bind(Configuration.class).toInstance(configuration);
        bind(MetricsManager.class).toInstance(metricsManager);
        bind(VersionServiceMXBean.class).to(VersionService.class);

        bind(CuratorFramework.class)
                .toProvider(new CuratorFrameworkProvider(configuration))
                .in(Scopes.SINGLETON);

        bind(ModelSession.class)
                .toProvider(getConfigurableProvider(ModelSession.class, configuration, Configuration.MODEL_PROVIDER, true))
                .in(Scopes.SINGLETON);
        bind(FileSystemSession.class)
                .toProvider(getConfigurableProvider(FileSystemSession.class, configuration, Configuration.FILESYSTEM_PROVIDER, true))
                .in(Scopes.SINGLETON);
        bind(Graph.class)
                .toProvider(getGraphProvider(configuration, Configuration.GRAPH_PROVIDER))
                .in(Scopes.SINGLETON);
        bind(WorkQueueRepository.class)
                .toProvider(getConfigurableProvider(WorkQueueRepository.class, configuration, Configuration.WORK_QUEUE_REPOSITORY, true))
                .in(Scopes.SINGLETON);
        bind(VisibilityTranslator.class)
                .toProvider(getConfigurableProvider(VisibilityTranslator.class, configuration, Configuration.VISIBILITY_TRANSLATOR, true))
                .in(Scopes.SINGLETON);
        bind(MimeTypeMapper.class)
                .toProvider(getConfigurableProvider(MimeTypeMapper.class, configuration, Configuration.MIME_TYPE_MAPPER, false))
                .in(Scopes.SINGLETON);
        bind(UserRepository.class)
                .toProvider(getConfigurableProvider(UserRepository.class, configuration, Configuration.USER_REPOSITORY, true))
                .in(Scopes.SINGLETON);
        bind(WorkspaceRepository.class)
                .toProvider(getConfigurableProvider(WorkspaceRepository.class, configuration, Configuration.WORKSPACE_REPOSITORY, true))
                .in(Scopes.SINGLETON);

        injectProviders();
    }

    private Provider<? extends Graph> getGraphProvider(Configuration configuration, String configurationPrefix) {
        // TODO change to use com.altamiracorp.securegraph.GraphFactory
        String graphClassName = configuration.get(configurationPrefix);
        final Map<String, String> configurationSubset = configuration.getSubset(configurationPrefix);

        final Class<?> graphClass;
        try {
            graphClass = Class.forName(graphClassName);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Could not find graph class with name: " + graphClassName, e);
        }

        final Method createMethod;
        try {
            createMethod = graphClass.getDeclaredMethod("create", Map.class);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Could not find create(Map) method on class: " + graphClass.getName(), e);
        }

        return new Provider<Graph>() {
            @Override
            public Graph get() {
                try {
                    return (Graph) createMethod.invoke(null, configurationSubset);
                } catch (Exception e) {
                    throw new RuntimeException("Could not create graph " + graphClass.getName(), e);
                }
            }
        };
    }

    private void injectProviders() {
        LOGGER.info("Running BootstrapBindingProviders");
        ServiceLoader<BootstrapBindingProvider> bindingProviders = ServiceLoader.load(BootstrapBindingProvider.class);
        Binder binder = binder();
        for (BootstrapBindingProvider provider : bindingProviders) {
            LOGGER.debug("Configuring bindings from BootstrapBindingProvider: %s", provider.getClass().getName());
            provider.addBindings(binder, configuration);
        }
    }

    private static class CuratorFrameworkProvider implements Provider<CuratorFramework> {
        private String zookeeperConnectionString;
        private RetryPolicy retryPolicy;

        public CuratorFrameworkProvider(Configuration configuration) {
            zookeeperConnectionString = configuration.get(Configuration.ZK_SERVERS);
            retryPolicy = new ExponentialBackoffRetry(1000, 3);
        }

        @Override
        public CuratorFramework get() {
            try {
                CuratorFramework client = CuratorFrameworkFactory.newClient(zookeeperConnectionString, retryPolicy);
                client.start();
                return client;
            } catch (IOException ex) {
                throw new LumifyException("Could not create curator: " + zookeeperConnectionString, ex);
            }
        }
    }

    private <T> Provider<T> getConfigurableProvider(final Class<T> clazz, final Configuration config, final String key,
                                                    final boolean required) {
        return getConfigurableProvider(clazz, config, key, null, required);
    }

    private <T> Provider<T> getConfigurableProvider(final Class<T> clazz, final Configuration config, final String key,
                                                    final User user, final boolean required) {
        Class<? extends T> configuredClass = BootstrapUtils.getConfiguredClass(config, key, required);
        return configuredClass != null ? new ConfigurableProvider<T>(configuredClass, config, user) : new NullProvider<T>();
    }

    private static class NullProvider<T> implements Provider<T> {
        @Override
        public T get() {
            return (T) null;
        }
    }

    private static class ConfigurableProvider<T> implements Provider<T> {
        /**
         * The class to instantiate.
         */
        private final Class<? extends T> clazz;

        /**
         * The Constructor to invoke.
         */
        private final Constructor<? extends T> constructor;

        /**
         * The constructor arguments.
         */
        private final Object[] constructorArgs;

        /**
         * The init method.
         */
        private final Method initMethod;

        /**
         * The init args.
         */
        private final Object[] initMethodArgs;

        public ConfigurableProvider(final Class<? extends T> clazz, final Configuration config) {
            this(clazz, config, null);
        }

        public ConfigurableProvider(final Class<? extends T> clazz, final Configuration config, final User user) {
            Constructor con;
            Object[] conArgs;
            boolean checkInit;
            try {
                con = clazz.getConstructor(Configuration.class);
                conArgs = new Object[]{config};
                checkInit = false;
            } catch (NoSuchMethodException nsme) {
                try {
                    con = clazz.getConstructor();
                    conArgs = null;
                    checkInit = true;
                } catch (NoSuchMethodException nsme1) {
                    throw new BootstrapException("Unable to find <ctor>(Configuration) or <ctor>() for class %s.", clazz.getName());
                } catch (SecurityException se) {
                    throw new BootstrapException(se, "Error accessing <ctor>() in class %s.", clazz.getName());
                }
            } catch (SecurityException se) {
                throw new BootstrapException(se, "Error accessing <ctor>(Configuration) in class %s.", clazz.getName());
            }
            Method init = null;
            Object[] initArgs = null;
            if (checkInit) {
                init = findInit(clazz, Configuration.class, User.class);
                if (init != null) {
                    initArgs = new Object[]{config, user};
                } else {
                    init = findInit(clazz, Map.class, User.class);
                    if (init != null) {
                        initArgs = new Object[]{config.toMap(), user};
                    } else {
                        init = findInit(clazz, Configuration.class);
                        if (init != null) {
                            initArgs = new Object[]{config};
                        } else {
                            init = findInit(clazz, Map.class);
                            if (init != null) {
                                initArgs = new Object[]{config.toMap()};
                            } else {
                                throw new BootstrapException("Unable to locate init(Configuration, User), init(Map, User), " +
                                        "init(Configuration) or init(Map) in %s.", clazz.getName());
                            }
                        }
                    }
                }
            }
            this.clazz = clazz;
            this.constructor = con;
            this.constructorArgs = conArgs;
            this.initMethod = init;
            this.initMethodArgs = initArgs;
        }

        private Method findInit(Class<? extends T> target, Class<?>... paramTypes) {
            try {
                return target.getMethod("init", paramTypes);
            } catch (NoSuchMethodException nsme) {
                return null;
            } catch (SecurityException se) {
                List<String> paramNames = new ArrayList<String>();
                for (Class<?> pc : paramTypes) {
                    paramNames.add(pc.getSimpleName());
                }
                throw new BootstrapException(se, "Error accessing init(%s) method in %s.", paramNames, clazz.getName());
            }
        }

        @Override
        public T get() {
            Throwable error;
            try {
                LOGGER.debug("creating %s", this.clazz.getName());
                T impl = constructor.newInstance(constructorArgs);
                InjectHelper.inject(impl);
                if (initMethod != null) {
                    initMethod.invoke(impl, initMethodArgs);
                }
                return impl;
            } catch (InstantiationException ie) {
                LOGGER.error("Error instantiating %s.", clazz.getName(), ie);
                error = ie;
            } catch (IllegalAccessException iae) {
                LOGGER.error("Unable to access default constructor for %s", clazz.getName(), iae);
                error = iae;
            } catch (IllegalArgumentException iae) {
                LOGGER.error("Unable to initialize instance of %s.", clazz.getName(), iae);
                error = iae;
            } catch (InvocationTargetException ite) {
                LOGGER.error("Error initializing instance of %s.", clazz.getName(), ite);
                error = ite;
            }
            throw new BootstrapException(error, "Unable to initialize instance of %s", clazz.getName());
        }
    }
}
