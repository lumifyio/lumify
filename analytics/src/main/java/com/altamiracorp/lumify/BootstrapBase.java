package com.altamiracorp.lumify;

import com.altamiracorp.bigtable.model.ModelSession;
import com.altamiracorp.bigtable.model.accumulo.AccumuloSession;
import com.altamiracorp.lumify.contentTypeExtraction.ContentTypeExtractor;
import com.altamiracorp.lumify.contentTypeExtraction.TikaContentTypeExtractor;
import com.altamiracorp.lumify.core.config.Configuration;
import com.altamiracorp.lumify.core.fs.FileSystemSession;
import com.altamiracorp.lumify.core.model.GraphSession;
import com.altamiracorp.lumify.core.model.search.SearchProvider;
import com.altamiracorp.lumify.core.model.workQueue.WorkQueueRepository;
import com.altamiracorp.lumify.core.user.SystemUser;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.fs.hdfs.HdfsSession;
import com.altamiracorp.lumify.model.KafkaWorkQueueRepository;
import com.altamiracorp.lumify.model.TitanGraphSession;
import com.altamiracorp.lumify.search.ElasticSearchProvider;
import com.google.inject.AbstractModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;

public abstract class BootstrapBase extends AbstractModule {
    private static final Logger LOGGER = LoggerFactory.getLogger(BootstrapBase.class);

    private final Configuration config;
    private static final String DEFAULT_MODEL_PROVIDER = AccumuloSession.class.getName();
    private static final String DEFAULT_GRAPH_PROVIDER = TitanGraphSession.class.getName();
    private static final String DEFAULT_SEARCH_PROVIDER = ElasticSearchProvider.class.getName();
    private static final String DEFAULT_FILESYSTEM_PROVIDER = HdfsSession.class.getName();

    protected BootstrapBase(Configuration config) {
        this.config = config;
    }

    @Override
    protected void configure() {
        LOGGER.info("Creating common bindings");
        User user = new SystemUser();

        bind(ModelSession.class).toInstance(createModelSession());
        bind(FileSystemSession.class).toInstance(createFileSystemSession());
        bind(GraphSession.class).toInstance(createGraphSession());
        bind(SearchProvider.class).toInstance(createSearchProvider(user));
        bind(WorkQueueRepository.class).toInstance(createWorkQueueRepository());
        bind(ContentTypeExtractor.class).toInstance(new TikaContentTypeExtractor());
    }

    private WorkQueueRepository createWorkQueueRepository() {
        String zookeeperServerNames = config.get(Configuration.ZK_SERVERS);
        return new KafkaWorkQueueRepository(zookeeperServerNames);
    }


    private ModelSession createModelSession() {
        Class modelProviderClass = null;
        try {
            modelProviderClass = config.getClass(Configuration.MODEL_PROVIDER, DEFAULT_MODEL_PROVIDER);
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
            fileSystemProviderClass = config.getClass(Configuration.FILESYSTEM_PROVIDER, DEFAULT_FILESYSTEM_PROVIDER);
            Constructor<FileSystemSession> modelSessionConstructor = fileSystemProviderClass.getConstructor(Configuration.class);
            return modelSessionConstructor.newInstance(config);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("The provided filesystem provider " + fileSystemProviderClass.getName() + " does not have the required constructor");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private GraphSession createGraphSession() {
        Class graphSessionClass = null;
        try {
            graphSessionClass = config.getClass(Configuration.GRAPH_PROVIDER, DEFAULT_GRAPH_PROVIDER);
            Constructor<GraphSession> graphSessionConstructor = graphSessionClass.getConstructor(Configuration.class);
            return graphSessionConstructor.newInstance(config);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("The provided graph provider " + graphSessionClass.getName() + " does not have the required constructor");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private SearchProvider createSearchProvider(User user) {
        Class searchProviderClass = null;
        try {
            searchProviderClass = config.getClass(Configuration.SEARCH_PROVIDER, DEFAULT_SEARCH_PROVIDER);
            Constructor<SearchProvider> searchProviderConstructor = searchProviderClass.getConstructor(Configuration.class, User.class);
            return searchProviderConstructor.newInstance(config, user);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("The provided search provider " + searchProviderClass.getName() + " does not have the required constructor");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
