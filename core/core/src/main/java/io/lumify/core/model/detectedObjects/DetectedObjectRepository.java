package io.lumify.core.model.detectedObjects;

import com.altamiracorp.bigtable.model.FlushFlag;
import com.altamiracorp.bigtable.model.ModelSession;
import com.altamiracorp.bigtable.model.Repository;
import com.altamiracorp.bigtable.model.Row;
import com.altamiracorp.bigtable.model.user.ModelUserContext;
import org.securegraph.Authorizations;
import org.securegraph.Vertex;
import org.securegraph.Visibility;
import com.google.inject.Inject;
import org.json.JSONArray;
import org.json.JSONObject;

public abstract class DetectedObjectRepository extends Repository<DetectedObjectModel>{
    @Inject
    public DetectedObjectRepository(ModelSession modelSession) {
        super(modelSession);
    }

    public abstract DetectedObjectModel fromRow(Row row);

    public abstract Row toRow(DetectedObjectModel obj);

    public abstract String getTableName();

    // TODO clean this method signature up. Takes way too many parameters.
    public abstract DetectedObjectModel saveDetectedObject(Object artifactVertexId, Object edgeId, Object graphVertexId, String concept,
                                           double x1, double y1, double x2, double y2, boolean resolved,
                                           String process, Visibility visibility, ModelUserContext modelUserContext);

    public abstract JSONArray toJSON (Vertex artifactVertex, ModelUserContext modelUserContext, Authorizations authorizations, String workspaceId);

    public abstract JSONObject toJSON(DetectedObjectModel detectedObjectModel, Authorizations authorizations);

    public abstract void updateColumnVisibility (DetectedObjectModel detectedObjectModel, String originalEdgeVisibility, String visibilityString, FlushFlag flushFlag);
}
