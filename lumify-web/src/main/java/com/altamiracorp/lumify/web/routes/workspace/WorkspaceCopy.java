package com.altamiracorp.lumify.web.routes.workspace;

import com.altamiracorp.lumify.core.model.workspace.Workspace;
import com.altamiracorp.lumify.core.model.workspace.WorkspaceRepository;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.web.BaseRequestHandler;
import com.altamiracorp.miniweb.HandlerChain;
import com.google.inject.Inject;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class WorkspaceCopy extends BaseRequestHandler {
    private final WorkspaceRepository workspaceRepository;

    @Inject
    public WorkspaceCopy(final WorkspaceRepository workspaceRepository) {
        this.workspaceRepository = workspaceRepository;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        final String workspaceId = getAttributeString(request, "workspaceId");

        User authUser = getUser(request);

        Workspace workspace = workspaceRepository.findById(workspaceId, authUser);
        if (workspace == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        Workspace newWorkspace = workspaceRepository.copy(workspace, authUser);
        request.getSession().setAttribute("activeWorkspace", newWorkspace.getId());
        JSONObject resultJson = newWorkspace.toJson(true);
        respondWithJson(response, resultJson);
    }
}
