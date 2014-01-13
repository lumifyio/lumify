package com.altamiracorp.lumify.core;

import com.altamiracorp.bigtable.model.ModelSession;
import com.altamiracorp.lumify.core.config.Configuration;
import com.altamiracorp.lumify.core.contentTypeExtraction.ContentTypeExtractor;
import com.altamiracorp.lumify.core.fs.FileSystemSession;
import com.altamiracorp.lumify.core.metrics.MetricsManager;
import com.altamiracorp.lumify.core.model.workQueue.WorkQueueRepository;
import com.altamiracorp.lumify.core.user.SystemUser;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.core.util.LumifyLogger;
import com.altamiracorp.lumify.core.util.LumifyLoggerFactory;
import com.altamiracorp.lumify.core.version.VersionService;
import com.altamiracorp.securegraph.Graph;
import com.altamiracorp.securegraph.GraphFactory;
import com.google.inject.AbstractModule;
import com.google.inject.Provider;

import java.lang.reflect.Constructor;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class BootstrapBase extends AbstractModule {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(BootstrapBase.class);

    private final Configuration config;

    protected BootstrapBase(Configuration config) {
        this.config = config;
    }

    @Override
    protected void configure() {
        LOGGER.info("Creating common bindings");
        User user = new SystemUser();

        MetricsManager metricManager = new MetricsManager();
        ModelSession modelSession = createModelSession();

        bind(MetricsManager.class).toInstance(metricManager);
        bind(ModelSession.class).toInstance(modelSession);
        bind(FileSystemSession.class).toInstance(createFileSystemSession());
        bind(WorkQueueRepository.class).toInstance(createWorkQueueRepository());
        bind(VersionService.class).toInstance(new VersionService());
        bind(ContentTypeExtractor.class).toProvider(new Provider<ContentTypeExtractor>() {
            @Override
            public ContentTypeExtractor get() {
                return createContentTypeExtractor();
            }
        });
        bind(Graph.class).toInstance(createGraph());
    }

    private Graph createGraph() {
        return new GraphFactory().createGraph(config.getSubset("graph").toMap());
    }

    private ContentTypeExtractor createContentTypeExtractor() {
        Class contentTypeExtractorClass = null;
        try {
            contentTypeExtractorClass = config.getClass(Configuration.CONTENT_TYPE_EXTRACTOR, null);
            if (contentTypeExtractorClass == null) {
                return null;
            }
            Constructor<ContentTypeExtractor> contentTypeExtractorConstructor = contentTypeExtractorClass.getConstructor();
            ContentTypeExtractor contentTypeExtractor = contentTypeExtractorConstructor.newInstance();
            contentTypeExtractor.init(config.toMap());
            return contentTypeExtractor;
        } catch (ClassNotFoundException e) {
            LOGGER.warn("Could not load class %s", config.get(Configuration.CONTENT_TYPE_EXTRACTOR));
            return null;
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("The provided model provider " + contentTypeExtractorClass.getName() + " does not have the required constructor");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private WorkQueueRepository createWorkQueueRepository() {
        Class workQueueRepositoryClass = null;
        try {
            workQueueRepositoryClass = config.getClass(Configuration.WORK_QUEUE_REPOSITORY, null);
            checkNotNull(workQueueRepositoryClass, Configuration.WORK_QUEUE_REPOSITORY + " must be configured");
            Constructor<WorkQueueRepository> workQueueRepositoryConstructor = workQueueRepositoryClass.getConstructor();
            WorkQueueRepository workQueueRepository = workQueueRepositoryConstructor.newInstance();
            workQueueRepository.init(config.toMap());
            return workQueueRepository;
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("The provided model provider " + workQueueRepositoryClass.getName() + " does not have the required constructor");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private ModelSession createModelSession() {
        Class modelProviderClass = null;
        try {
            modelProviderClass = config.getClass(Configuration.MODEL_PROVIDER, null);
            checkNotNull(modelProviderClass, Configuration.MODEL_PROVIDER + " must be configured");
            Constructor<ModelSession> modelSessionConstructor = modelProviderClass.getConstructor();
            ModelSession modelSession = modelSessionConstructor.newInstance();
            modelSession.init(config.toMap());
            return modelSession;
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("The provided model provider " + modelProviderClass.getName() + " does not have the required constructor");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private FileSystemSession createFileSystemSession() {
        Class fileSystemProviderClass = null;
        try {
            fileSystemProviderClass = config.getClass(Configuration.FILESYSTEM_PROVIDER, null);
            checkNotNull(fileSystemProviderClass, Configuration.FILESYSTEM_PROVIDER + " must be configured");
            Constructor<FileSystemSession> modelSessionConstructor = fileSystemProviderClass.getConstructor(Configuration.class);
            return modelSessionConstructor.newInstance(config);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("The provided filesystem provider " + fileSystemProviderClass.getName() + " does not have the required constructor");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
