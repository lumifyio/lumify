package io.lumify.palantir.mr.mappers;

import io.lumify.core.model.workspace.WorkspaceLumifyProperties;
import io.lumify.core.model.workspace.WorkspaceRepository;
import io.lumify.core.security.LumifyVisibility;
import io.lumify.palantir.model.LongLongWritable;
import io.lumify.palantir.model.PtGraphObject;
import org.securegraph.EdgeBuilderByVertexId;
import org.securegraph.Visibility;

import java.io.IOException;

public class PtGraphObjectMapper extends PalantirMapperBase<LongLongWritable, PtGraphObject> {
    private Visibility visibility;

    @Override
    protected void setup(Context context) throws IOException, InterruptedException {
        super.setup(context);
        visibility = new LumifyVisibility(WorkspaceRepository.VISIBILITY_STRING).getVisibility();
    }

    @Override
    protected void safeMap(LongLongWritable key, PtGraphObject ptGraphObject, Context context) throws Exception {
        context.setStatus(key.toString());

        String workspaceVertexId = PtGraphMapper.getWorkspaceVertexId(ptGraphObject.getGraphId());
        String objectVertexId = PtObjectMapper.getObjectVertexId(ptGraphObject.getObjectId());
        String edgeId = getWorkspaceToEntityEdgeId(workspaceVertexId, objectVertexId);

        EdgeBuilderByVertexId m = getGraph().prepareEdge(edgeId, workspaceVertexId, objectVertexId, WorkspaceRepository.WORKSPACE_TO_ENTITY_RELATIONSHIP_IRI, visibility);
        WorkspaceLumifyProperties.WORKSPACE_TO_ENTITY_VISIBLE.setProperty(m, true, visibility);
        m.save(getAuthorizations());
    }

    public static String getWorkspaceToEntityEdgeId(String workspaceVertexId, String objectVertexId) {
        return workspaceVertexId + WorkspaceRepository.WORKSPACE_TO_ENTITY_RELATIONSHIP_IRI + objectVertexId;
    }
}
