package io.lumify.test;

import com.google.inject.Injector;
import io.lumify.core.config.Configuration;
import io.lumify.core.config.ConfigurationLoader;
import io.lumify.core.config.HashMapConfigurationLoader;
import io.lumify.core.exception.LumifyException;
import io.lumify.core.ingest.graphProperty.GraphPropertyWorkData;
import io.lumify.core.ingest.graphProperty.GraphPropertyWorker;
import io.lumify.core.ingest.graphProperty.GraphPropertyWorkerPrepareData;
import io.lumify.core.ingest.graphProperty.TermMentionFilter;
import io.lumify.core.model.audit.AuditRepository;
import io.lumify.core.model.audit.InMemoryAuditRepository;
import io.lumify.core.model.user.InMemoryUser;
import io.lumify.core.model.workQueue.WorkQueueRepository;
import io.lumify.core.security.DirectVisibilityTranslator;
import io.lumify.core.security.VisibilityTranslator;
import io.lumify.core.user.User;
import io.lumify.web.clientapi.model.Privilege;
import org.apache.commons.io.IOUtils;
import org.apache.hadoop.fs.FileSystem;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.securegraph.*;
import org.securegraph.id.IdGenerator;
import org.securegraph.id.QueueIdGenerator;
import org.securegraph.inmemory.InMemoryAuthorizations;
import org.securegraph.inmemory.InMemoryGraph;
import org.securegraph.inmemory.InMemoryGraphConfiguration;
import org.securegraph.search.DefaultSearchIndex;
import org.securegraph.search.SearchIndex;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public abstract class GraphPropertyWorkerTestBase {
    private InMemoryGraph graph;
    private IdGenerator graphIdGenerator;
    private SearchIndex graphSearchIndex;
    private HashMap configurationMap;
    private GraphPropertyWorkerPrepareData graphPropertyWorkerPrepareData;
    private User user;
    private AuditRepository auditRepository;
    private WorkQueueRepository workQueueRepository;
    private VisibilityTranslator visibilityTranslator = new DirectVisibilityTranslator();

    protected GraphPropertyWorkerTestBase() {

    }

    @Before
    public final void clearGraph() {
        if (graph != null) {
            graph.shutdown();
            graph = null;
        }
        graphIdGenerator = null;
        graphSearchIndex = null;
        configurationMap = null;
        graphPropertyWorkerPrepareData = null;
        user = null;
        auditRepository = null;
        workQueueRepository = null;
        System.setProperty(ConfigurationLoader.ENV_CONFIGURATION_LOADER, HashMapConfigurationLoader.class.getName());
        getConfiguration();

        InMemoryWorkQueueRepository.clearQueue();
    }

    @After
    public final void after() {
        clearGraph();
    }

    protected GraphPropertyWorkerPrepareData getWorkerPrepareData() {
        return getWorkerPrepareData(null, null, null, null, null, null);
    }

    protected GraphPropertyWorkerPrepareData getWorkerPrepareData(Map configuration, List<TermMentionFilter> termMentionFilters, FileSystem fileSystem, User user, Authorizations authorizations, Injector injector) {
        if (graphPropertyWorkerPrepareData == null) {
            if (configuration == null) {
                configuration = getConfigurationMap();
            }
            if (termMentionFilters == null) {
                termMentionFilters = new ArrayList<TermMentionFilter>();
            }
            if (user == null) {
                user = getUser();
            }
            if (authorizations == null) {
                authorizations = getGraphAuthorizations();
            }
            graphPropertyWorkerPrepareData = new GraphPropertyWorkerPrepareData(configuration, termMentionFilters, fileSystem, user, authorizations, injector);
        }
        return graphPropertyWorkerPrepareData;
    }

    protected User getUser() {
        if (user == null) {
            Set<Privilege> privileges = Privilege.ALL;
            String[] authorizations = new String[0];
            user = new InMemoryUser("test", "Test User", "test@lumify.io", privileges, authorizations, null);
        }
        return user;
    }

    protected Graph getGraph() {
        if (graph == null) {
            Map graphConfiguration = getConfigurationMap();
            InMemoryGraphConfiguration inMemoryGraphConfiguration = new InMemoryGraphConfiguration(graphConfiguration);
            graph = InMemoryGraph.create(inMemoryGraphConfiguration, getGraphIdGenerator(), getGraphSearchIndex(inMemoryGraphConfiguration));
        }
        return graph;
    }

    protected IdGenerator getGraphIdGenerator() {
        if (graphIdGenerator == null) {
            graphIdGenerator = new QueueIdGenerator();
        }
        return graphIdGenerator;
    }

    protected SearchIndex getGraphSearchIndex(GraphConfiguration inMemoryGraphConfiguration) {
        if (graphSearchIndex == null) {
            graphSearchIndex = new DefaultSearchIndex(inMemoryGraphConfiguration);
        }
        return graphSearchIndex;
    }

    protected Map getConfigurationMap() {
        if (configurationMap == null) {
            configurationMap = new HashMap();
            configurationMap.put("ontology.intent.concept.location", "http://lumify.io/test#location");
            configurationMap.put("ontology.intent.concept.organization", "http://lumify.io/test#organization");
            configurationMap.put("ontology.intent.concept.person", "http://lumify.io/test#person");
        }
        return configurationMap;
    }

    protected Authorizations getGraphAuthorizations(String... authorizations) {
        return new InMemoryAuthorizations(authorizations);
    }

    protected byte[] getResourceAsByteArray(Class sourceClass, String resourceName) {
        try {
            InputStream in = sourceClass.getResourceAsStream(resourceName);
            if (in == null) {
                throw new IOException("Could not find resource: " + resourceName);
            }
            return IOUtils.toByteArray(in);
        } catch (IOException e) {
            throw new LumifyException("Could not load resource. " + sourceClass.getName() + " at " + resourceName, e);
        }
    }

    protected boolean run(GraphPropertyWorker gpw, GraphPropertyWorkerPrepareData workerPrepareData, Element e, Property prop, InputStream in) {
        return run(gpw, workerPrepareData, e, prop, in, null, null);
    }

    protected boolean run(GraphPropertyWorker gpw, GraphPropertyWorkerPrepareData workerPrepareData, Element e, Property prop, InputStream in, String workspaceId, String visibilitySource) {
        try {
            gpw.setConfiguration(getConfiguration());
            gpw.setAuditRepository(getAuditRepository());
            gpw.setGraph(getGraph());
            gpw.setVisibilityTranslator(getVisibilityTranslator());
            gpw.setWorkQueueRepository(getWorkQueueRepository());
            gpw.prepare(workerPrepareData);
        } catch (Exception ex) {
            throw new LumifyException("Failed to prepare: " + gpw.getClass().getName(), ex);
        }

        try {
            if (!gpw.isHandled(e, prop)) {
                return false;
            }
        } catch (Exception ex) {
            throw new LumifyException("Failed to isHandled: " + gpw.getClass().getName(), ex);
        }

        try {
            GraphPropertyWorkData executeData = new GraphPropertyWorkData(visibilityTranslator, e, prop, workspaceId, visibilitySource);
            gpw.execute(in, executeData);
        } catch (Exception ex) {
            throw new LumifyException("Failed to execute: " + gpw.getClass().getName(), ex);
        }
        return true;
    }

    protected WorkQueueRepository getWorkQueueRepository() {
        if (workQueueRepository == null) {
            workQueueRepository = new InMemoryWorkQueueRepository(getGraph(), getConfiguration());
        }
        return workQueueRepository;
    }

    protected Queue<JSONObject> getGraphPropertyQueue() {
        return InMemoryWorkQueueRepository.getQueue(WorkQueueRepository.GRAPH_PROPERTY_QUEUE_NAME);
    }

    protected AuditRepository getAuditRepository() {
        if (auditRepository == null) {
            auditRepository = new InMemoryAuditRepository();
        }
        return auditRepository;
    }

    protected VisibilityTranslator getVisibilityTranslator() {
        return visibilityTranslator;
    }

    protected Configuration getConfiguration() {
        return ConfigurationLoader.load(getConfigurationMap());
    }
}
