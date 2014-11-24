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
import org.securegraph.util.ConvertingIterable;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class PtGraphObjectImporter extends PtGroupingImporterBase<PtGraphObject, Long> {
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
    protected void processGroup(final Long graphId, List<PtGraphObject> rows) throws Exception {
        Workspace workspace = getDataImporter().getWorkspacesByGraphId().get(graphId);
        checkNotNull(workspace, "Could not find workspace with graph id: " + graphId);

        final AWState.Wrapper1 awstate = getDataImporter().getAwstateProtosByGraphId().get(graphId);
        checkNotNull(awstate, "Could not find awstate with graph id: " + graphId);

        Iterable<WorkspaceRepository.Update> updates = new ConvertingIterable<PtGraphObject, WorkspaceRepository.Update>(rows) {
            @Override
            protected WorkspaceRepository.Update convert(PtGraphObject row) {
                GraphPosition graphPosition = null;
                try {
                    AWState.VertexInner awstateVertex = findObject(awstate, row.getObjectId());
                    checkNotNull(awstateVertex, "Could not find awstateVertex: " + row.getObjectId());
                    graphPosition = new GraphPosition(awstateVertex.getX() / 2, awstateVertex.getY() / 2);
                } catch (Throwable ex) {
                    LOGGER.error("Could not parse graph position from awstate proto: graphId: " + graphId + ", objectId: " + row.getObjectId(), ex);
                }

                return new WorkspaceRepository.Update(getObjectVertexId(row), true, graphPosition);
            }
        };

        User user = getDataImporter().getSystemUser();

        workspaceRepository.updateEntitiesOnWorkspace(workspace, updates, user);
    }

    @Override
    protected Long getGroupKey(PtGraphObject row) {
        return row.getGraphId();
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
