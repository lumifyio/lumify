package io.lumify.palantir.mr.mappers;

import io.lumify.core.model.properties.LumifyProperties;
import io.lumify.core.model.workspace.WorkspaceLumifyProperties;
import io.lumify.core.model.workspace.WorkspaceRepository;
import io.lumify.core.security.LumifyVisibility;
import io.lumify.palantir.model.AWState;
import io.lumify.palantir.model.PtGraph;
import io.lumify.palantir.util.TryInflaterInputStream;
import io.lumify.web.clientapi.model.WorkspaceAccess;
import org.apache.hadoop.io.LongWritable;
import org.securegraph.EdgeBuilderByVertexId;
import org.securegraph.VertexBuilder;
import org.securegraph.Visibility;

import java.io.IOException;

public class PtGraphMapper extends PalantirMapperBase<LongWritable, PtGraph> {

    private Visibility workspaceOnlyVisibility;

    @Override
    protected void setup(Context context) throws IOException, InterruptedException {
        super.setup(context);
        workspaceOnlyVisibility = new LumifyVisibility(WorkspaceRepository.VISIBILITY_STRING).getVisibility();
    }

    @Override
    protected void safeMap(LongWritable key, PtGraph ptGraph, Context context) throws Exception {
        context.setStatus(key.toString());

        byte[] awstateProto = TryInflaterInputStream.inflate(ptGraph.getAwstateProto());
        AWState.Wrapper1 awstate = AWState.Wrapper1.parseFrom(awstateProto);

        String workspaceVertexId = getWorkspaceVertexId(ptGraph);
        String userId = PtUserMapper.getUserVertexId(ptGraph.getCreatedBy());

        getAuthorizationRepository().addAuthorizationToGraph(workspaceVertexId);
        saveWorkspaceVertex(ptGraph, workspaceVertexId);
        saveWorkspaceToUserEdge(workspaceVertexId, userId);

        for (AWState.Vertex v : awstate.getWrapper2().getWrapper3().getVertexList()) {
            long objectId = v.getVertexInner().getObjectId();
            String objectVertexId = PtObjectMapper.getObjectVertexId(objectId);
            String edgeId = PtGraphObjectMapper.getWorkspaceToEntityEdgeId(workspaceVertexId, objectVertexId);

            EdgeBuilderByVertexId m = getGraph().prepareEdge(edgeId, workspaceVertexId, objectVertexId, WorkspaceRepository.WORKSPACE_TO_ENTITY_RELATIONSHIP_IRI, workspaceOnlyVisibility);
            WorkspaceLumifyProperties.WORKSPACE_TO_ENTITY_GRAPH_POSITION_X.setProperty(m, v.getVertexInner().getX(), workspaceOnlyVisibility);
            WorkspaceLumifyProperties.WORKSPACE_TO_ENTITY_GRAPH_POSITION_Y.setProperty(m, v.getVertexInner().getY(), workspaceOnlyVisibility);
            m.save(getAuthorizations());
        }
    }

    private void saveWorkspaceToUserEdge(String workspaceVertexId, String userId) {
        String edgeId = workspaceVertexId + WorkspaceRepository.WORKSPACE_TO_USER_RELATIONSHIP_IRI + userId;
        EdgeBuilderByVertexId edgeBuilder = getGraph().prepareEdge(edgeId, workspaceVertexId, userId, WorkspaceRepository.WORKSPACE_TO_USER_RELATIONSHIP_IRI, workspaceOnlyVisibility);
        WorkspaceLumifyProperties.WORKSPACE_TO_USER_IS_CREATOR.setProperty(edgeBuilder, true, workspaceOnlyVisibility);
        WorkspaceLumifyProperties.WORKSPACE_TO_USER_ACCESS.setProperty(edgeBuilder, WorkspaceAccess.WRITE.toString(), workspaceOnlyVisibility);
        edgeBuilder.save(getAuthorizations());
    }

    private void saveWorkspaceVertex(PtGraph ptGraph, String workspaceVertexId) {
        VertexBuilder m = prepareVertex(workspaceVertexId, workspaceOnlyVisibility);
        LumifyProperties.CONCEPT_TYPE.setProperty(m, WorkspaceRepository.WORKSPACE_CONCEPT_IRI, workspaceOnlyVisibility);
        WorkspaceLumifyProperties.TITLE.setProperty(m, ptGraph.getTitle(), workspaceOnlyVisibility);
        m.save(getAuthorizations());
    }

    public static String getWorkspaceVertexId(PtGraph ptGraph) {
        return getWorkspaceVertexId(ptGraph.getId());
    }

    public static String getWorkspaceVertexId(long graphId) {
        return ID_PREFIX + "WORKSPACE_" + graphId;
    }
}
