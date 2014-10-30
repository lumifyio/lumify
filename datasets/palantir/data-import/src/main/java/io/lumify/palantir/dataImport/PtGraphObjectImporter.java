package io.lumify.palantir.dataImport;

import com.google.inject.Inject;
import io.lumify.core.model.workspace.Workspace;
import io.lumify.core.model.workspace.WorkspaceRepository;
import io.lumify.core.user.User;
import io.lumify.palantir.dataImport.model.PtGraphObject;

import static com.google.common.base.Preconditions.checkNotNull;

public class PtGraphObjectImporter extends PtImporterBase<PtGraphObject> {
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

        User user = getDataImporter().getSystemUser();

        workspaceRepository.updateEntityOnWorkspace(workspace, getObjectVertexId(row), true, null, user);
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
