package com.altamiracorp.lumify.web.routes.workspace;

import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.core.model.workspace.WorkspaceRepository;
import com.altamiracorp.lumify.core.model.workspace.WorkspaceRowKey;
import com.altamiracorp.lumify.web.BaseRequestHandler;
import com.altamiracorp.miniweb.HandlerChain;
import com.google.inject.Inject;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class WorkspaceDelete extends BaseRequestHandler {
    private final WorkspaceRepository workspaceRepository;
    private static final Logger LOGGER = LoggerFactory.getLogger(WorkspaceDelete.class);

    @Inject
    public WorkspaceDelete(final WorkspaceRepository workspaceRepository) {
        this.workspaceRepository = workspaceRepository;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        if (isDeleteAuthorized(request)) {
            final String strRowKey = getAttributeString(request, "workspaceRowKey");

            User user = getUser(request);
            WorkspaceRowKey rowKey = new WorkspaceRowKey(strRowKey);

            LOGGER.info("Deleting workspace with id: " + strRowKey);
            workspaceRepository.delete(rowKey, user.getModelUserContext());

            JSONObject resultJson = new JSONObject();
            resultJson.put("success", true);

            respondWithJson(response, resultJson);

        } else {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
        }
    }

    // TODO: Make this workspace delete authorization more robust
    private boolean isDeleteAuthorized(HttpServletRequest request) {
        return true;
    }
}
