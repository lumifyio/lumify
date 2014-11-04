package io.lumify.palantir.dataImport;

import com.google.inject.Inject;
import io.lumify.core.model.workspace.Workspace;
import io.lumify.core.model.workspace.WorkspaceRepository;
import io.lumify.core.user.User;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import io.lumify.palantir.dataImport.model.PtGraphObject;
import io.lumify.palantir.dataImport.model.awstateProto.AwstateProto;
import io.lumify.palantir.dataImport.model.awstateProto.AwstateProtoObject;
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

        AwstateProto awstateProto = getDataImporter().getAwstateProtosByGraphId().get(row.getGraphId());
        checkNotNull(awstateProto, "Could not find awstateProto with graph id: " + row.getGraphId());

        GraphPosition graphPosition = null;
        try {
            AwstateProtoObject awstateProtoObject = awstateProto.findObject(row.getObjectId());
            checkNotNull(awstateProtoObject, "Could not find awstateProtoObject: " + row.getObjectId());
            graphPosition = new GraphPosition(awstateProtoObject.getX() / 3, awstateProtoObject.getY() / 3);
        } catch (Throwable ex) {
            LOGGER.error("Could not parse graph position from awstate proto", ex);
        }

        User user = getDataImporter().getSystemUser();

        workspaceRepository.updateEntityOnWorkspace(workspace, getObjectVertexId(row), true, graphPosition, user);
    }

    private String getObjectVertexId(PtGraphObject row) {
        return getObjectVertexId(row.getObjectId());
    }

    @Override
    protected String getSql() {
        return "SELECT * FROM {namespace}.PT_GRAPH_OBJECT";
    }

    @Inject
    public void setWorkspaceRepository(WorkspaceRepository workspaceRepository) {
        this.workspaceRepository = workspaceRepository;
    }
}
