package io.lumify.web.clientapi;

import io.lumify.web.clientapi.model.UserMe;
import io.lumify.web.clientapi.model.Workspace;

import java.util.List;

public class LumifyApi {
    private final UserApi userApi;
    private final ArtifactApiExt artifactApi;
    private final WorkspaceApi workspaceApi;
    private final WorkspacesApi workspacesApi;
    private final AdminApiExt adminApi;
    private Workspace currentWorkspace;

    public LumifyApi(String basePath) {
        userApi = new UserApi();
        userApi.setBasePath(basePath);

        artifactApi = new ArtifactApiExt();
        artifactApi.setBasePath(basePath);

        workspaceApi = new WorkspaceApi();
        workspaceApi.setBasePath(basePath);

        workspacesApi = new WorkspacesApi();
        workspacesApi.setBasePath(basePath);

        adminApi = new AdminApiExt();
        adminApi.setBasePath(basePath);
    }

    public UserApi getUserApi() {
        return userApi;
    }

    public ArtifactApiExt getArtifactApi() {
        return artifactApi;
    }

    public WorkspaceApi getWorkspaceApi() {
        return workspaceApi;
    }

    public WorkspacesApi getWorkspacesApi() {
        return workspacesApi;
    }

    public AdminApiExt getAdminApi() {
        return adminApi;
    }

    public Workspace loginAndGetCurrentWorkspace() throws ApiException {
        UserMe me = getUserApi().getMe();
        ApiInvoker.getInstance().setCsrfToken(me.getCsrfToken());

        List<Workspace> workspaces = getWorkspacesApi().getWorkspaces().getWorkspaces();

        currentWorkspace = null;
        if (me.getCurrentWorkspaceId() != null) {
            for (Workspace workspace : workspaces) {
                if (workspace.getWorkspaceId().equals(me.getCurrentWorkspaceId())) {
                    currentWorkspace = workspace;
                    break;
                }
            }
        }

        if (currentWorkspace == null) {
            if (workspaces.size() == 0) {
                currentWorkspace = getWorkspaceApi().newWorkspace();
            } else {
                currentWorkspace = workspaces.get(0);
            }
        }

        ApiInvoker.getInstance().setWorkspaceId(currentWorkspace.getWorkspaceId());

        return currentWorkspace;
    }
}
