package com.altamiracorp.lumify.core.model.detectedObjects;

import com.altamiracorp.bigtable.model.FlushFlag;
import com.altamiracorp.bigtable.model.ModelSession;
import com.altamiracorp.bigtable.model.Repository;
import com.altamiracorp.bigtable.model.Row;
import com.altamiracorp.bigtable.model.user.ModelUserContext;
import com.altamiracorp.lumify.core.model.properties.LumifyProperties;
import com.altamiracorp.lumify.core.model.termMention.TermMentionModel;
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

public abstract class DetectedObjectRepository extends Repository<DetectedObjectModel>{
    @Inject
    public DetectedObjectRepository(ModelSession modelSession) {
        super(modelSession);
    }

    public abstract DetectedObjectModel fromRow(Row row);

    public abstract Row toRow(DetectedObjectModel obj);

    public abstract String getTableName();

    public abstract Iterable<DetectedObjectModel> findByGraphVertexId(String vertexId, ModelUserContext modelUserContext);

    // TODO clean this method signature up. Takes way too many parameters.
    public abstract DetectedObjectModel saveDetectedObject(Object artifactVertexId, Object edgeId, Object graphVertexId, String concept,
                                           double x1, double y1, double x2, double y2, boolean resolved,
                                           String process, Visibility visibility, ModelUserContext modelUserContext);

    public abstract JSONArray toJSON (Vertex artifactVertex, ModelUserContext modelUserContext, Authorizations authorizations, String workspaceId);

    public abstract JSONObject toJSON(DetectedObjectModel detectedObjectModel, Authorizations authorizations);

    public abstract void updateColumnVisibility (DetectedObjectModel detectedObjectModel, String originalEdgeVisibility, String visibilityString, FlushFlag flushFlag);
}
