package com.altamiracorp.lumify.core.model.detectedObjects;

import com.altamiracorp.bigtable.model.ModelSession;
import com.altamiracorp.bigtable.model.Repository;
import com.altamiracorp.bigtable.model.Row;
import com.altamiracorp.bigtable.model.user.ModelUserContext;
import com.altamiracorp.lumify.core.model.properties.LumifyProperties;
import com.altamiracorp.lumify.core.util.GraphUtil;
import com.altamiracorp.securegraph.Authorizations;
import com.altamiracorp.securegraph.Graph;
import com.altamiracorp.securegraph.Vertex;
import com.altamiracorp.securegraph.Visibility;
import com.altamiracorp.securegraph.util.IterableUtils;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Iterator;

@Singleton
public class DetectedObjectRepository extends Repository<DetectedObjectModel> {
    private DetectedObjectBuilder termMentionBuilder = new DetectedObjectBuilder();
    private final Graph graph;

    @Inject
    public DetectedObjectRepository(final ModelSession modelSession,
                                    final Graph graph) {
        super(modelSession);
        this.graph = graph;
    }

    @Override
    public DetectedObjectModel fromRow(Row row) {
        return termMentionBuilder.fromRow(row);
    }

    @Override
    public Row toRow(DetectedObjectModel obj) {
        return obj;
    }

    @Override
    public String getTableName() {
        return termMentionBuilder.getTableName();
    }

    public Iterable<DetectedObjectModel> findByGraphVertexId(String vertexId, ModelUserContext modelUserContext) {
        return findByRowStartsWith(vertexId + ":", modelUserContext);
    }

    // TODO clean this method signature up. Takes way too many parameters.
d    public DetectedObjectModel saveDetectedObject(Object artifactVertexId, Object edgeId, Object graphVertexId,  String concept,
                                                  double x1, double y1, double x2, double y2, boolean resolved,
                                                  String process, Visibility visibility, ModelUserContext modelUserContext) {
        DetectedObjectRowKey detectedObjectRowKey;
        if (edgeId == null) {
            detectedObjectRowKey = new DetectedObjectRowKey(artifactVertexId, x1, y1, x2, y2);
        } else {
            detectedObjectRowKey = new DetectedObjectRowKey(artifactVertexId, x1, y1, x2, y2, edgeId);
        }

        DetectedObjectModel detectedObjectModel = findByRowKey(detectedObjectRowKey.toString(), modelUserContext);
        if (detectedObjectModel == null) {
            detectedObjectModel = new DetectedObjectModel(detectedObjectRowKey);
        }
        detectedObjectModel.getMetadata()
                .setX1(x1, visibility)
                .setY1(y1, visibility)
                .setX2(x2, visibility)
                .setY2(y2, visibility);

        if (resolved) {
            detectedObjectModel.getMetadata().setResolvedId(graphVertexId, visibility);
            detectedObjectModel.getMetadata().setEdgeId(edgeId, visibility);
        } else {
            detectedObjectModel.getMetadata().setClassifierConcept(concept, visibility);
        }

        if (process != null) {
            detectedObjectModel.getMetadata().setProcess(process, visibility);
        }
        save(detectedObjectModel);
        return detectedObjectModel;
    }

    public JSONArray toJSON (Vertex artifactVertex, ModelUserContext modelUserContext, Authorizations authorizations, String workspaceId) {
        Iterator<DetectedObjectModel> detectedObjectModels = findByGraphVertexId(artifactVertex.getId().toString(), modelUserContext).iterator();
        JSONArray detectedObjects = new JSONArray();
        while (detectedObjectModels.hasNext()) {
            DetectedObjectModel model = detectedObjectModels.next();
            if (IterableUtils.count(findByRowStartsWith(model.getRowKey().toString(), modelUserContext)) > 1) {
                continue;
            }
            detectedObjects.put(toJSON(model, authorizations));
        }
        return detectedObjects;
    }

    public JSONObject toJSON (DetectedObjectModel detectedObjectModel, Authorizations authorizations) {
        JSONObject object = detectedObjectModel.toJson();
        if (detectedObjectModel.getMetadata().getResolvedId() != null) {
            Vertex vertex = graph.getVertex(detectedObjectModel.getMetadata().getResolvedId(), authorizations);
            object.put("title", vertex.getPropertyValue(LumifyProperties.TITLE.getKey()));
            object.put("graphVertexId", vertex.getId());
            object.put("edgeId", detectedObjectModel.getRowKey().getEdgeId());
            object.put("http://lumify.io#conceptType", vertex.getPropertyValue("http://lumify.io#conceptType"));
        }
        object.put(LumifyProperties.ROW_KEY.getKey(), detectedObjectModel.getRowKey().toString());

        return object;
    }
}
