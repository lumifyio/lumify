package com.altamiracorp.lumify.core.model.detectedObjects;

import com.altamiracorp.bigtable.model.ModelSession;
import com.altamiracorp.bigtable.model.Repository;
import com.altamiracorp.bigtable.model.Row;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.securegraph.Graph;
import com.altamiracorp.securegraph.Visibility;
import com.google.inject.Inject;
import com.google.inject.Singleton;

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

    public Iterable<DetectedObjectModel> findByGraphVertexId(String vertexId, User user) {
        return findByRowStartsWith(vertexId + ":", user.getModelUserContext());
    }

    public DetectedObjectModel saveDetectedObject(Object artifactVertexId, Object id, String concept,
                                                  double x1, double y1, double x2, double y2, boolean resolved,
                                                  String process, Visibility visibility) {
        if (id == null) {
            id = graph.getIdGenerator().nextId();
        }
        DetectedObjectRowKey detectedObjectRowKey = new DetectedObjectRowKey(artifactVertexId, id);
        DetectedObjectModel detectedObjectModel = new DetectedObjectModel(detectedObjectRowKey);
        detectedObjectModel.getMetadata().setClassifierConcept(concept, visibility)
                .setX1(x1, visibility)
                .setY1(y1, visibility)
                .setX2(x2, visibility)
                .setY2(y2, visibility);

        if (resolved) {
            detectedObjectModel.getMetadata().setResolvedId(id, visibility);
        }

        if (process != null) {
            detectedObjectModel.getMetadata().setProcess(process, visibility);
        }
        save(detectedObjectModel);
        return detectedObjectModel;
    }
}
