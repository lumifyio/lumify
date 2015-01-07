package io.lumify.palantir.dataImport;

import com.google.inject.Inject;
import io.lumify.core.exception.LumifyException;
import io.lumify.core.model.workspace.Workspace;
import io.lumify.core.model.workspace.WorkspaceRepository;
import io.lumify.core.user.User;
import io.lumify.palantir.dataImport.model.PtGraph;
import io.lumify.palantir.dataImport.model.protobuf.AWState;
import io.lumify.palantir.dataImport.util.TryInflaterInputStream;

public class PtGraphImporter extends PtRowImporterBase<PtGraph> {
    private static final String PALANTIR_WORKSPACE_ID_PREFIX = "PALANTIR_";
    private WorkspaceRepository workspaceRepository;

    public PtGraphImporter(DataImporter dataImporter) {
        super(dataImporter, PtGraph.class);
    }

    @Override
    protected void beforeProcessRows() {
        super.beforeProcessRows();

        for (User user : getDataImporter().getUsers().values()) {
            for (Workspace workspace : workspaceRepository.findAll(user)) {
                if (workspace.getWorkspaceId().startsWith(PALANTIR_WORKSPACE_ID_PREFIX)) {
                    Long key = Long.parseLong(workspace.getWorkspaceId().substring(PALANTIR_WORKSPACE_ID_PREFIX.length()));
                    getDataImporter().getWorkspacesByGraphId().put(key, workspace);
                }
            }
        }
    }

    @Override
    protected void processRow(PtGraph row) throws Exception {
        byte[] awstateProto = TryInflaterInputStream.inflate(row.getAwstateProto());
        AWState.Wrapper1 awstate = AWState.Wrapper1.parseFrom(awstateProto);
        getDataImporter().getAwstateProtosByGraphId().put(row.getId(), awstate);

        if (getDataImporter().getWorkspacesByGraphId().containsKey(row.getId())) {
            return;
        }

        String id = PALANTIR_WORKSPACE_ID_PREFIX + row.getId();
        Workspace workspace = workspaceRepository.add(id, row.getTitle(), getUser(row));
        getDataImporter().getWorkspacesByGraphId().put(row.getId(), workspace);
    }

    private User getUser(PtGraph row) {
        User user = getDataImporter().getUsers().get(row.getCreatedBy());
        if (user == null) {
            throw new LumifyException("Could not find user with id: " + row.getCreatedBy());
        }
        return user;
    }

    @Override
    protected String getSql() {
        return "select * FROM {namespace}.PT_GRAPH";
    }

    @Inject
    public void setWorkspaceRepository(WorkspaceRepository workspaceRepository) {
        this.workspaceRepository = workspaceRepository;
    }
}
