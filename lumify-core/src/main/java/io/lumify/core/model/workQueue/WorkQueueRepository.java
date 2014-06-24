package io.lumify.core.model.workQueue;

import com.altamiracorp.bigtable.model.FlushFlag;
import com.google.inject.Inject;
import io.lumify.core.config.Configuration;
import io.lumify.core.exception.LumifyException;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.model.user.UserStatus;
import io.lumify.core.user.User;
import io.lumify.core.util.JsonSerializer;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import org.json.JSONArray;
import org.json.JSONObject;
import org.securegraph.*;

import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class WorkQueueRepository {
    protected static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(WorkQueueRepository.class);
    public static final String GRAPH_PROPERTY_QUEUE_NAME = "graphProperty";
    private final Graph graph;

    @Inject
    protected WorkQueueRepository(Graph graph) {
        this.graph = graph;
    }

    public void pushGraphPropertyQueue(final Element element, final Property property) {
        pushGraphPropertyQueue(element, property.getKey(), property.getName());
    }

    public void pushElementImageQueue(final Element element, final Property property) {
        pushElementImageQueue(element, property.getKey(), property.getName());
    }

    public void pushElementImageQueue(final Element element, String propertyKey, final String propertyName) {
        getGraph().flush();
        checkNotNull(element);
        JSONObject data = new JSONObject();
        if (element instanceof Vertex) {
            data.put("graphVertexId", element.getId());
        } else if (element instanceof Edge) {
            data.put("graphEdgeId", element.getId());
        } else {
            throw new LumifyException("Unexpected element type: " + element.getClass().getName());
        }
        data.put("propertyKey", propertyKey);
        data.put("propertyName", propertyName);
        pushOnQueue(GRAPH_PROPERTY_QUEUE_NAME, FlushFlag.DEFAULT, data);

        broadcastEntityImage(element, propertyKey, propertyName);
    }

    public void pushGraphPropertyQueue(final Element element, String propertyKey, final String propertyName) {
        pushGraphPropertyQueue(element, propertyKey, propertyName);
    }

    public void pushGraphPropertyQueue(final Element element, String propertyKey, final String propertyName, String workspaceId) {
        getGraph().flush();
        checkNotNull(element);
        JSONObject data = new JSONObject();
        if (element instanceof Vertex) {
            data.put("graphVertexId", element.getId());
        } else if (element instanceof Edge) {
            data.put("graphEdgeId", element.getId());
        } else {
            throw new LumifyException("Unexpected element type: " + element.getClass().getName());
        }
        data.put("propertyKey", propertyKey);
        data.put("propertyName", propertyName);
        pushOnQueue(GRAPH_PROPERTY_QUEUE_NAME, FlushFlag.DEFAULT, data);

        broadcastPropertyChange(element, propertyKey, propertyName, workspaceId);
    }

    public void pushElement(Element element) {
        pushGraphPropertyQueue(element, null, null);
    }

    public void pushEdgeDeletion(Edge edge) {
        broadcastEdgeDeletion(edge);
    }

    protected void broadcastEdgeDeletion(Edge edge) {
        JSONObject dataJson = new JSONObject();
        if (edge != null) {
            dataJson.put("edgeId", edge.getId());
        }

        JSONObject json = new JSONObject();
        json.put("type", "edgeDeletion");
        json.put("data", dataJson);
        broadcastJson(json);
    }

    public void pushVerticesDeletion(JSONArray verticesDeleted) {
        broadcastVerticesDeletion(verticesDeleted);
    }

    protected void broadcastVerticesDeletion(JSONArray verticesDeleted) {
        JSONObject dataJson = new JSONObject();
        if (verticesDeleted != null) {
            dataJson.put("vertexIds", verticesDeleted);
        }

        JSONObject json = new JSONObject();
        json.put("type", "verticesDeleted");
        json.put("data", dataJson);
        broadcastJson(json);
    }

    public void pushDetectedObjectChange(JSONObject artifactVertexWithDetectedObjects) {
        broadcastDetectedObjectChange(artifactVertexWithDetectedObjects);
    }

    public void pushTextUpdated(String vertexId) {
        broadcastTextUpdated(vertexId);
    }

    protected void broadcastTextUpdated(String vertexId) {
        JSONObject dataJson = new JSONObject();
        if (vertexId != null) {
            dataJson.put("graphVertexId", vertexId);
        }

        JSONObject json = new JSONObject();
        json.put("type", "textUpdated");
        json.put("data", dataJson);
        broadcastJson(json);
    }

    protected void broadcastDetectedObjectChange(JSONObject artifactVertexWithDetectedObjects) {
        JSONObject dataJson = new JSONObject();
        if (artifactVertexWithDetectedObjects != null) {
            dataJson = artifactVertexWithDetectedObjects;
        }

        JSONObject json = new JSONObject();
        json.put("type", "detectedObjectChange");
        json.put("data", dataJson);
        broadcastJson(json);
    }

    public void pushUserStatusChange(User user, UserStatus status) {
        broadcastUserStatusChange(user, status);
    }

    protected void broadcastUserStatusChange(User user, UserStatus status) {
        JSONObject json = new JSONObject();
        json.put("type", "userStatusChange");
        JSONObject data = UserRepository.toJson(user);
        data.put("status", status.toString());
        json.put("data", data);
        broadcastJson(json);
    }

    public void pushUserWorkspaceChange(User user, String workspaceId) {
        broadcastUserWorkspaceChange(user, workspaceId);
    }

    protected void broadcastUserWorkspaceChange(User user, String workspaceId) {
        JSONObject json = new JSONObject();
        json.put("type", "userWorkspaceChange");
        JSONObject data = UserRepository.toJson(user);
        data.put("workspaceId", workspaceId);
        json.put("data", data);
        broadcastJson(json);
    }

    protected void broadcastPropertyChange(Element element, String propertyKey, String propertyName, String workspaceId) {
        try {
            JSONObject json;
            if (element instanceof Vertex) {
                json = getBroadcastPropertyChangeJson((Vertex) element, propertyKey, propertyName, workspaceId);
            } else if (element instanceof Edge) {
                json = getBroadcastPropertyChangeJson((Edge) element, propertyKey, propertyName, workspaceId);
            } else {
                throw new LumifyException("Unexpected element type: " + element.getClass().getName());
            }
            broadcastJson(json);
        } catch (Exception ex) {
            throw new LumifyException("Could not broadcast property change", ex);
        }
    }

    protected void broadcastEntityImage(Element element, String propertyKey, String propertyName) {
        try {
            JSONObject json = getBroadcastEntityImageJson((Vertex) element);
            broadcastJson(json);
        } catch (Exception ex) {
            throw new LumifyException("Could not broadcast property change", ex);
        }
    }

    protected abstract void broadcastJson(JSONObject json);

    protected JSONObject getBroadcastEntityImageJson(Vertex graphVertex) {
        JSONObject dataJson = new JSONObject();

        JSONObject vertexJson = JsonSerializer.toJson(graphVertex, null);
        dataJson.put("vertex", vertexJson);
        dataJson.put("graphVertexId", graphVertex.getId());

        JSONObject json = new JSONObject();
        json.put("type", "entityImageUpdated");
        json.put("data", dataJson);
        return json;
    }

    protected JSONObject getBroadcastPropertyChangeJson(Vertex graphVertex, String propertyKey, String propertyName, String workspaceId) {
        JSONObject dataJson = new JSONObject();

        JSONObject vertexJson = JsonSerializer.toJson(graphVertex, workspaceId);
        dataJson.put("vertex", vertexJson);

        JSONObject propertyJson = new JSONObject();
        propertyJson.put("graphVertexId", graphVertex.getId());
        propertyJson.put("propertyKey", propertyKey);
        propertyJson.put("propertyName", propertyName);
        JSONArray propertiesJson = new JSONArray();
        propertiesJson.put(propertyJson);

        dataJson.put("properties", propertiesJson);

        JSONObject json = new JSONObject();
        json.put("type", "propertiesChange");
        json.put("data", dataJson);
        return json;
    }

    protected JSONObject getBroadcastPropertyChangeJson(Edge edge, String propertyKey, String propertyName, String workspaceId) {
        JSONObject dataJson = new JSONObject();

        JSONObject vertexJson = JsonSerializer.toJson(edge, workspaceId);
        dataJson.put("edge", vertexJson);

        JSONObject propertyJson = new JSONObject();
        propertyJson.put("graphEdgeId", edge.getId());
        propertyJson.put("propertyKey", propertyKey);
        propertyJson.put("propertyName", propertyName);
        JSONArray propertiesJson = new JSONArray();
        propertiesJson.put(propertyJson);

        dataJson.put("properties", propertiesJson);

        JSONObject json = new JSONObject();
        json.put("type", "propertiesChange");
        json.put("data", dataJson);
        return json;
    }

    public abstract void pushOnQueue(String queueName, FlushFlag flushFlag, JSONObject json);

    public void init(Map map) {

    }

    // TODO this is pretty awful but returning backtype.storm.topology.IRichSpout causes a dependency hell problem because it requires storm jar
    //      one possibility would be to return a custom type but this just pushes the problem
    public abstract Object createSpout(Configuration configuration, String queueName);

    public abstract void flush();

    public abstract void format();

    public Graph getGraph() {
        return graph;
    }

    public abstract void subscribeToBroadcastMessages(BroadcastConsumer broadcastConsumer);

    public void shutdown() {

    }

    public static abstract class BroadcastConsumer {
        public abstract void broadcastReceived(JSONObject json);
    }
}
