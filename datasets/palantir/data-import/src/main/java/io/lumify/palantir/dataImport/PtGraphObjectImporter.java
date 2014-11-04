package io.lumify.palantir.dataImport;

import com.google.inject.Inject;
import io.lumify.core.model.workspace.Workspace;
import io.lumify.core.model.workspace.WorkspaceRepository;
import io.lumify.core.user.User;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import io.lumify.palantir.dataImport.model.PtGraphObject;
import io.lumify.palantir.dataImport.model.protobuf.AWState;
import io.lumify.web.clientapi.model.GraphPosition;

import static com.google.common.base.Preconditions.checkNotNull;

public class PtGraphObjectImporter extends PtImporterBase<PtGraphObject> {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(PtGraphObjectImporter.class);
    private WorkspaceRepository workspaceRepository;

    public PtGraphObjectImporter(DataImporter dataImporter) {
        super(dataImporter, PtGraphObject.class);
    }

    @Override
    protected void beforeProcessRows() {
        super.beforeProcessRows();
        getDataImporter().getGraph().flush();
    }

    @Override
    protected void processRow(PtGraphObject row) throws Exception {
        Workspace workspace = getDataImporter().getWorkspacesByGraphId().get(row.getGraphId());
        checkNotNull(workspace, "Could not find workspace with graph id: " + row.getGraphId());

        AWState.Wrapper1 awstate = getDataImporter().getAwstateProtosByGraphId().get(row.getGraphId());
        checkNotNull(awstate, "Could not find awstate with graph id: " + row.getGraphId());

        GraphPosition graphPosition = null;
        try {
            AWState.VertexInner awstateVertex = findObject(awstate, row.getObjectId());
            checkNotNull(awstateVertex, "Could not find awstateVertex: " + row.getObjectId());
            graphPosition = new GraphPosition(awstateVertex.getX() / 2, awstateVertex.getY() / 2);
        } catch (Throwable ex) {
            LOGGER.error("Could not parse graph position from awstate proto: graphId: " + row.getGraphId() + ", objectId: " + row.getObjectId(), ex);
        }

        User user = getDataImporter().getSystemUser();

        workspaceRepository.updateEntityOnWorkspace(workspace, getObjectVertexId(row), true, graphPosition, user);
    }

    private AWState.VertexInner findObject(AWState.Wrapper1 awstate, long objectId) {
        for (AWState.Vertex v : awstate.getWrapper2().getWrapper3().getVertexList()) {
            if (v.getVertexInner().getObjectId() == objectId) {
                return v.getVertexInner();
            }
        }
        return null;
    }

    private String getObjectVertexId(PtGraphObject row) {
        return getObjectVertexId(row.getObjectId());
    }

    @Override
    protected String getSql() {
        return "SELECT * FROM {namespace}.PT_GRAPH_OBJECT ORDER BY GRAPH_ID";
    }

    @Inject
    public void setWorkspaceRepository(WorkspaceRepository workspaceRepository) {
        this.workspaceRepository = workspaceRepository;
    }
}
