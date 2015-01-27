package io.lumify.securegraph.model.longRunningProcess;

import com.google.inject.Inject;
import io.lumify.core.model.longRunningProcess.LongRunningProcessRepository;
import io.lumify.core.model.ontology.Concept;
import io.lumify.core.model.ontology.OntologyRepository;
import io.lumify.core.model.properties.LumifyProperties;
import io.lumify.core.model.properties.types.JsonLumifyProperty;
import io.lumify.core.model.user.AuthorizationRepository;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.model.workQueue.WorkQueueRepository;
import io.lumify.core.user.User;
import org.json.JSONObject;
import org.securegraph.*;
import org.securegraph.util.ConvertingIterable;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.securegraph.util.IterableUtils.toList;

public class SecureGraphLongRunningProcessRepository extends LongRunningProcessRepository {
    private final WorkQueueRepository workQueueRepository;
    private final UserRepository userRepository;
    private final Graph graph;
    private JsonLumifyProperty QUEUE_ITEM_JSON_PROPERTY = new JsonLumifyProperty("http://lumify.io/longRunningProcess#queueItemJson");

    @Inject
    public SecureGraphLongRunningProcessRepository(
            AuthorizationRepository authorizationRepository,
            OntologyRepository ontologyRepository,
            UserRepository userRepository,
            WorkQueueRepository workQueueRepository,
            Graph graph) {
        this.userRepository = userRepository;
        this.workQueueRepository = workQueueRepository;
        this.graph = graph;

        authorizationRepository.addAuthorizationToGraph(VISIBILITY_STRING);

        Concept userConcept = ontologyRepository.getConceptByIRI(UserRepository.USER_CONCEPT_IRI);
        checkNotNull(userConcept, "Could not find user concept: " + UserRepository.USER_CONCEPT_IRI);

        Concept longRunningProcessConcept = ontologyRepository.getOrCreateConcept(null, LONG_RUNNING_PROCESS_CONCEPT_IRI, "workspace", null);
        checkNotNull(longRunningProcessConcept);
        List<Concept> userConceptList = new ArrayList<>();
        userConceptList.add(userConcept);
        List<Concept> longRunningProcessConceptList = new ArrayList<>();
        longRunningProcessConceptList.add(longRunningProcessConcept);
        ontologyRepository.getOrCreateRelationshipType(userConceptList, longRunningProcessConceptList, LONG_RUNNING_PROCESS_TO_USER_EDGE_IRI, "longRunningProcess to user", new String[0]);
    }

    @Override
    public String enqueue(JSONObject longRunningProcessQueueItem, User user, Authorizations authorizations) {
        authorizations = getAuthorizations(user);

        Vertex userVertex = graph.getVertex(user.getUserId(), authorizations);
        checkNotNull(userVertex, "Could not find user with id: " + user.getUserId());
        Visibility visibility = getVisibility();

        VertexBuilder vertexBuilder = this.graph.prepareVertex(visibility);
        LumifyProperties.CONCEPT_TYPE.setProperty(vertexBuilder, LONG_RUNNING_PROCESS_CONCEPT_IRI, visibility);
        QUEUE_ITEM_JSON_PROPERTY.setProperty(vertexBuilder, longRunningProcessQueueItem, visibility);
        Vertex longRunningProcessVertex = vertexBuilder.save(authorizations);

        this.graph.addEdge(userVertex, longRunningProcessVertex, LONG_RUNNING_PROCESS_TO_USER_EDGE_IRI, visibility, authorizations);

        this.graph.flush();

        longRunningProcessQueueItem.put("id", longRunningProcessVertex.getId());
        this.workQueueRepository.pushLongRunningProcessQueue(longRunningProcessQueueItem, user.getUserId());

        return longRunningProcessVertex.getId();
    }

    public Authorizations getAuthorizations(User user) {
        Authorizations authorizations;
        authorizations = userRepository.getAuthorizations(user, VISIBILITY_STRING, UserRepository.VISIBILITY_STRING);
        return authorizations;
    }

    @Override
    public void beginWork(JSONObject longRunningProcessQueueItem) {
        super.beginWork(longRunningProcessQueueItem);
        updateVertexWithJson(longRunningProcessQueueItem);
    }

    @Override
    public void ack(JSONObject longRunningProcessQueueItem) {
        updateVertexWithJson(longRunningProcessQueueItem);
    }

    @Override
    public void nak(JSONObject longRunningProcessQueueItem, Throwable ex) {
        updateVertexWithJson(longRunningProcessQueueItem);
    }

    public void updateVertexWithJson(JSONObject longRunningProcessQueueItem) {
        String longRunningProcessGraphVertexId = longRunningProcessQueueItem.getString("id");
        Authorizations authorizations = getAuthorizations(userRepository.getSystemUser());
        Vertex vertex = this.graph.getVertex(longRunningProcessGraphVertexId, authorizations);
        checkNotNull(vertex, "Could not find long running process vertex: " + longRunningProcessGraphVertexId);
        QUEUE_ITEM_JSON_PROPERTY.setProperty(vertex, longRunningProcessQueueItem, getVisibility(), authorizations);
        this.graph.flush();
    }

    @Override
    public List<JSONObject> getLongRunningProcesses(User user) {
        Authorizations authorizations = getAuthorizations(user);
        Vertex userVertex = graph.getVertex(user.getUserId(), authorizations);
        checkNotNull(userVertex, "Could not find user with id: " + user.getUserId());
        Iterable<Vertex> longRunningProcessVertices = userVertex.getVertices(Direction.OUT, LONG_RUNNING_PROCESS_TO_USER_EDGE_IRI, authorizations);
        return toList(new ConvertingIterable<Vertex, JSONObject>(longRunningProcessVertices) {
            @Override
            protected JSONObject convert(Vertex longRunningProcessVertex) {
                return QUEUE_ITEM_JSON_PROPERTY.getPropertyValue(longRunningProcessVertex);
            }
        });
    }

    @Override
    public JSONObject findById(String longRunningProcessId, User user) {
        Authorizations authorizations = getAuthorizations(user);
        Vertex vertex = this.graph.getVertex(longRunningProcessId, authorizations);
        if (vertex == null) {
            return null;
        }
        return QUEUE_ITEM_JSON_PROPERTY.getPropertyValue(vertex);
    }

    @Override
    public void cancel(String longRunningProcessId, User user) {
        Authorizations authorizations = getAuthorizations(userRepository.getSystemUser());
        Vertex vertex = this.graph.getVertex(longRunningProcessId, authorizations);
        checkNotNull(vertex, "Could not find long running process vertex: " + longRunningProcessId);
        JSONObject json = QUEUE_ITEM_JSON_PROPERTY.getPropertyValue(vertex);
        json.put("canceled", true);
        QUEUE_ITEM_JSON_PROPERTY.setProperty(vertex, json, getVisibility(), getAuthorizations(user));
        this.graph.flush();
    }

    @Override
    public void reportProgress(JSONObject longRunningProcessQueueItem, double progressPercent, String message) {
        String longRunningProcessGraphVertexId = longRunningProcessQueueItem.getString("id");
        Authorizations authorizations = getAuthorizations(userRepository.getSystemUser());
        Vertex vertex = this.graph.getVertex(longRunningProcessGraphVertexId, authorizations);
        checkNotNull(vertex, "Could not find long running process vertex: " + longRunningProcessGraphVertexId);

        JSONObject json = QUEUE_ITEM_JSON_PROPERTY.getPropertyValue(vertex);
        json.put("progress", progressPercent);
        json.put("progressMessage", message);
        QUEUE_ITEM_JSON_PROPERTY.setProperty(vertex, json, getVisibility(), authorizations);
        this.graph.flush();

        workQueueRepository.broadcastLongRunningProcessChange(json);
    }

    @Override
    public void delete(String longRunningProcessId, User authUser) {
        Authorizations authorizations = getAuthorizations(authUser);
        Vertex vertex = this.graph.getVertex(longRunningProcessId, authorizations);
        this.graph.removeVertex(vertex, authorizations);
        this.graph.flush();
    }

    private Visibility getVisibility() {
        return new Visibility(VISIBILITY_STRING);
    }
}
