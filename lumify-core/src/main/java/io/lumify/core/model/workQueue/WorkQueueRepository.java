package io.lumify.core.model.workQueue;

import com.altamiracorp.bigtable.model.FlushFlag;
import io.lumify.core.config.Configuration;
import io.lumify.core.exception.LumifyException;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.model.user.UserStatus;
import io.lumify.core.user.User;
import io.lumify.core.util.JsonSerializer;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import org.securegraph.Edge;
import org.securegraph.Graph;
import org.securegraph.Property;
import org.securegraph.Vertex;
import com.google.inject.Inject;
import org.json.JSONArray;
import org.json.JSONObject;

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

    public void pushGraphPropertyQueue(final Vertex graphVertex, final Property property) {
        pushGraphPropertyQueue(graphVertex, property.getKey(), property.getName());
    }

    public void pushGraphPropertyQueue(final Vertex graphVertex, final String propertyKey, final String propertyName) {
        getGraph().flush();

        checkNotNull(graphVertex);
        checkNotNull(propertyKey);
        checkNotNull(propertyName);
        JSONObject data = new JSONObject();
        data.put("graphVertexId", graphVertex.getId());
        data.put("propertyKey", propertyKey);
        data.put("propertyName", propertyName);
        pushOnQueue(GRAPH_PROPERTY_QUEUE_NAME, FlushFlag.DEFAULT, data);

        broadcastPropertyChange(graphVertex, propertyKey, propertyName);
    }

    public void pushGraphPropertyQueue(Edge edge, String propertyKey, String propertyName) {
        broadcastPropertyChange(edge, propertyKey, propertyName);
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

    public void pushDetectedObjectChange(JSONObject artifactVertexWithDetectedObjects) {
        broadcastDetectedObjectChange(artifactVertexWithDetectedObjects);
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
        json.put("data", UserRepository.toJson(user));
        broadcastJson(json);
    }

    protected void broadcastPropertyChange(Edge edge, String propertyKey, String propertyName) {
        try {
            JSONObject json = getBroadcastPropertyChangeJson(edge, propertyKey, propertyName);
            broadcastJson(json);
        } catch (Exception ex) {
            throw new LumifyException("Could not broadcast property change", ex);
        }
    }

    protected void broadcastPropertyChange(Vertex graphVertex, String propertyKey, String propertyName) {
        try {
            JSONObject json = getBroadcastPropertyChangeJson(graphVertex, propertyKey, propertyName);
            broadcastJson(json);
        } catch (Exception ex) {
            throw new LumifyException("Could not broadcast property change", ex);
        }
    }

    protected abstract void broadcastJson(JSONObject json);

    protected JSONObject getBroadcastPropertyChangeJson(Vertex graphVertex, String propertyKey, String propertyName) {
        JSONObject dataJson = new JSONObject();

        JSONObject vertexJson = JsonSerializer.toJson(graphVertex, null);
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

    protected JSONObject getBroadcastPropertyChangeJson(Edge edge, String propertyKey, String propertyName) {
        JSONObject dataJson = new JSONObject();

        JSONObject vertexJson = JsonSerializer.toJson(edge, null);
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

    public static abstract class BroadcastConsumer {
        public abstract void broadcastReceived(JSONObject json);
    }
}
