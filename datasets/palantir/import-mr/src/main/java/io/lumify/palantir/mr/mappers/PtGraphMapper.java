package io.lumify.palantir.mr.mappers;

import io.lumify.core.model.properties.LumifyProperties;
import io.lumify.core.model.workspace.WorkspaceLumifyProperties;
import io.lumify.core.model.workspace.WorkspaceRepository;
import io.lumify.palantir.model.AWState;
import io.lumify.palantir.model.PtGraph;
import io.lumify.palantir.util.TryInflaterInputStream;
import io.lumify.web.clientapi.model.VisibilityJson;
import io.lumify.web.clientapi.model.WorkspaceAccess;
import org.apache.hadoop.io.LongWritable;
import org.securegraph.EdgeBuilderByVertexId;
import org.securegraph.VertexBuilder;
import org.securegraph.Visibility;

import java.io.IOException;

public class PtGraphMapper extends PalantirMapperBase<PtGraph> {

    private Visibility workspaceOnlyVisibility;

    @Override
    protected void setup(Context context) throws IOException, InterruptedException {
        super.setup(context);
        workspaceOnlyVisibility = new Visibility(WorkspaceRepository.VISIBILITY_STRING);
    }

    @Override
    protected void safeMap(LongWritable key, PtGraph ptGraph, Context context) throws Exception {
        context.setStatus(key.toString());

        byte[] awstateProto = TryInflaterInputStream.inflate(ptGraph.getAwstateProto());
        AWState.Wrapper1 awstate = AWState.Wrapper1.parseFrom(awstateProto);

        String workspaceVertexId = getWorkspaceVertexId(ptGraph);
        String userId = PtUserMapper.getUserVertexId(ptGraph.getCreatedBy());

        Visibility visibility = getVisibility(workspaceVertexId);
        getAuthorizationRepository().addAuthorizationToGraph(workspaceVertexId);
        saveWorkspaceVertex(ptGraph, workspaceVertexId);
        saveWorkspaceToUserEdge(workspaceVertexId, userId);
    }

    private Visibility getVisibility(String workspaceVertexId) {
        VisibilityJson visibilityJson = new VisibilityJson();
        visibilityJson.addWorkspace(workspaceVertexId);
        return getVisibilityTranslator().toVisibilityNoSuperUser(visibilityJson);
    }

    private void saveWorkspaceToUserEdge(String workspaceVertexId, String userId) {
        EdgeBuilderByVertexId edgeBuilder = getGraph().prepareEdge(workspaceVertexId, userId, WorkspaceRepository.WORKSPACE_TO_USER_RELATIONSHIP_IRI, workspaceOnlyVisibility);
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
