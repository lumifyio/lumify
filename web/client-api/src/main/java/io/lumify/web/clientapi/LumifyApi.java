package io.lumify.web.clientapi;

import io.lumify.web.clientapi.codegen.ApiException;
import io.lumify.web.clientapi.model.ClientApiUser;
import io.lumify.web.clientapi.model.ClientApiWorkspace;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LumifyApi {
    private final UserApiExt userApi;
    private final WorkspaceApiExt workspaceApi;
    private final AdminApiExt adminApi;
    private final VertexApiExt vertexApi;
    private final OntologyApiExt ontologyApi;
    private final EdgeApiExt edgeApi;
    private final LongRunningProcessApiExt longRunningProcessApi;
    private final String basePath;
    private ClientApiUser me;

    public LumifyApi(String basePath) {
        this.basePath = basePath;

        userApi = new UserApiExt();
        userApi.setBasePath(basePath);

        workspaceApi = new WorkspaceApiExt();
        workspaceApi.setBasePath(basePath);

        adminApi = new AdminApiExt();
        adminApi.setBasePath(basePath);

        vertexApi = new VertexApiExt();
        vertexApi.setBasePath(basePath);

        edgeApi = new EdgeApiExt();
        edgeApi.setBasePath(basePath);

        ontologyApi = new OntologyApiExt();
        ontologyApi.setBasePath(basePath);

        longRunningProcessApi = new LongRunningProcessApiExt();
        longRunningProcessApi.setBasePath(basePath);
    }

    public UserApiExt getUserApi() {
        return userApi;
    }

    public WorkspaceApiExt getWorkspaceApi() {
        return workspaceApi;
    }

    public AdminApiExt getAdminApi() {
        return adminApi;
    }

    public VertexApiExt getVertexApi() {
        return vertexApi;
    }

    public OntologyApiExt getOntologyApi() {
        return ontologyApi;
    }

    public EdgeApiExt getEdgeApi() {
        return edgeApi;
    }

    public LongRunningProcessApiExt getLongRunningProcessApi() {
        return longRunningProcessApi;
    }

    public String getCurrentWorkspaceId() {
        return ApiInvoker.getInstance().getWorkspaceId();
    }

    public ClientApiWorkspace loginAndGetCurrentWorkspace() throws ApiException {
        me = getUserApi().getMe();
        ApiInvoker.getInstance().setCsrfToken(me.getCsrfToken());

        List<ClientApiWorkspace> workspaces = getWorkspaceApi().getAll().getWorkspaces();

        ClientApiWorkspace currentWorkspace = null;
        if (me.getCurrentWorkspaceId() != null) {
            for (ClientApiWorkspace workspace : workspaces) {
                if (workspace.getWorkspaceId().equals(me.getCurrentWorkspaceId())) {
                    currentWorkspace = workspace;
                    break;
                }
            }
        }

        if (currentWorkspace == null) {
            if (workspaces.size() == 0) {
                currentWorkspace = getWorkspaceApi().create();
            } else {
                currentWorkspace = workspaces.get(0);
            }
        }

        ApiInvoker.getInstance().setWorkspaceId(currentWorkspace.getWorkspaceId());

        return currentWorkspace;
    }

    public String invokeAPI(String path, String method, Map<String, String> queryParams, Object body, Map<String, String> headerParams, Map<String, String> formParams, String contentType) throws ApiException {
        if (queryParams == null) {
            queryParams = new HashMap<String, String>();
        }
        if (headerParams == null) {
            headerParams = new HashMap<String, String>();
        }
        if (formParams == null) {
            formParams = new HashMap<String, String>();
        }
        return ApiInvoker.getInstance().invokeAPI(this.basePath, path, method, queryParams, body, headerParams, formParams, contentType);
    }

    public void logout() throws ApiException {
        invokeAPI("/logout", "POST", null, null, null, null, null);
    }

    public String getCurrentUserId() {
        return me.getId();
    }

    public void setWorkspaceId(String workspaceId) {
        ApiInvoker.getInstance().setWorkspaceId(workspaceId);
    }
}
