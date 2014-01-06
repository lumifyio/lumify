package com.altamiracorp.lumify.core.model.graph;

import com.altamiracorp.lumify.core.model.GraphSession;
import com.altamiracorp.lumify.core.model.ontology.LabelName;
import com.altamiracorp.lumify.core.model.ontology.PropertyName;
import com.altamiracorp.lumify.core.model.ontology.VertexType;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.core.util.LumifyLogger;
import com.altamiracorp.lumify.core.util.LumifyLoggerFactory;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.tinkerpop.blueprints.Edge;
import org.json.JSONArray;

import java.util.List;
import java.util.Map;

@Singleton
public class GraphRepository {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(GraphRepository.class);
    private final GraphSession graphSession;

    @Inject
    public GraphRepository(GraphSession graphSession) {
        this.graphSession = graphSession;
    }

    public Edge findEdge(String sourceId, String destId, String label, User user) {
        return graphSession.findEdge(sourceId, destId, label, user);
    }

    public GraphVertex findVertex(String graphVertexId, User user) {
        return graphSession.findGraphVertex(graphVertexId, user);
    }

    public List<GraphVertex> findVertices(String[] vertexIds, User user) {
        return graphSession.findGraphVertices(vertexIds, user);
    }

    public GraphVertex findVertexByTitleAndType(String graphVertexTitle, VertexType type, User user) {
        return graphSession.findVertexByExactTitleAndType(graphVertexTitle, type, user);
    }

    public GraphVertex findVertexByPropertyAndType(String property, String graphVertexPropertyValue, VertexType type, User user) {
        return graphSession.findVertexByExactPropertyAndType(property, graphVertexPropertyValue, type, user);
    }

    public List<GraphVertex> getRelatedVertices(String graphVertexId, User user) {
        return graphSession.getRelatedVertices(graphVertexId, user);
    }

    public List<GraphRelationship> getRelationships(List<String> allIds, User user) {
        return graphSession.getRelationships(allIds, user);
    }

    public void saveMany(List<GraphRelationship> graphRelationships, User user) {
        for (GraphRelationship graphRelationship : graphRelationships) {
            save(graphRelationship, user);
        }
    }

    public GraphRelationship save(GraphRelationship graphRelationship, User user) {
        graphSession.save(graphRelationship, user);
        return graphRelationship;
    }

    public String save(GraphVertex graphVertex, User user) {
        return graphSession.save(graphVertex, user);
    }

    public GraphRelationship saveRelationship(String sourceGraphVertexId, String destGraphVertexId, String label, User user) {
        GraphRelationship relationship = new GraphRelationship(null, sourceGraphVertexId, destGraphVertexId, label);
        relationship.setProperty(PropertyName.RELATIONSHIP_TYPE.toString(), label);
        return save(relationship, user);
    }

    public GraphRelationship saveRelationship(String sourceGraphVertexId, String destGraphVertexId, LabelName label, User user) {
        return saveRelationship(sourceGraphVertexId, destGraphVertexId, label.toString(), user);
    }

    public GraphRelationship saveRelationship(GraphVertex sourceGraphVertex, GraphVertex destGraphVertex, LabelName label, User user) {
        return saveRelationship(sourceGraphVertex.getId(), destGraphVertex.getId(), label, user);
    }

    public String saveVertex(GraphVertex graphVertex, User user) {
        return graphSession.save(graphVertex, user);
    }

    public Map<String, String> getVertexProperties(String graphVertexId, User user) {
        return graphSession.getVertexProperties(graphVertexId, user);
    }

    public Map<GraphRelationship, GraphVertex> getRelationships(String graphVertexId, User user) {
        return graphSession.getRelationships(graphVertexId, user);
    }

    public Map<String, String> getEdgeProperties(String sourceVertex, String destVertex, String label, User user) {
        return graphSession.getEdgeProperties(sourceVertex, destVertex, label, user);
    }

    public List<GraphVertex> findByGeoLocation(double latitude, double longitude, double radius, User user) {
        LOGGER.info("findByGeoLocation latitude: %d, longitude: %d, radius: %d", latitude, longitude, radius);
        return graphSession.findByGeoLocation(latitude, longitude, radius, user);
    }

    public GraphPagedResults search(String title, JSONArray filterJson, User user, long offset, long size, String subType) {
        return graphSession.search(title, filterJson, user, offset, size, subType);
    }

    public void removeRelationship(String source, String target, String label, User user) {
        graphSession.removeRelationship(source, target, label, user);
    }

    public GraphRelationship findOrAddRelationship(String sourceVertexId, String targetVertexId, String label, User user) {
        Map<GraphRelationship, GraphVertex> relationships = getRelationships(sourceVertexId, user);
        for (Map.Entry<GraphRelationship, GraphVertex> relationship : relationships.entrySet()) {
            if (relationship.getValue().getId().equals(targetVertexId) &&
                    relationship.getKey().getLabel().equals(label)) {
                return relationship.getKey();
            }
        }
        return this.saveRelationship(sourceVertexId, targetVertexId, label, user);
    }

    public GraphRelationship findOrAddRelationship(String sourceVertexId, String targetVertexId, LabelName label, User user) {
        return findOrAddRelationship(sourceVertexId, targetVertexId, label.toString(), user);
    }

    public GraphRelationship findOrAddRelationship(GraphVertex sourceVertex, GraphVertex targetVertex, LabelName label, User user) {
        return findOrAddRelationship(sourceVertex.getId(), targetVertex.getId(), label, user);
    }

    public List<List<GraphVertex>> findPath(GraphVertex sourceVertex, GraphVertex destVertex, int depth, int hops, User user) {
        return graphSession.findPath(sourceVertex, destVertex, depth, hops, user);
    }

    public void remove(String graphVertexId, User user) {
        graphSession.remove(graphVertexId, user);
    }

    public void setPropertyEdge(String sourceId, String destId, String label, String propertyName, Object value, User user) {
        Edge edge = findEdge(sourceId, destId, label, user);
        edge.setProperty(propertyName, value);
        LOGGER.info("set property of vertex: %s, property name: %s, value: %s", edge.getId(), propertyName, value);
        graphSession.commit();
    }

    public void commit() {
        graphSession.commit();
    }
}
