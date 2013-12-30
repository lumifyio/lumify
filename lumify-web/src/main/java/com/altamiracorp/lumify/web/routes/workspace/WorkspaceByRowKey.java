package com.altamiracorp.lumify.web.routes.workspace;

import com.altamiracorp.lumify.core.model.workspace.Workspace;
import com.altamiracorp.lumify.core.model.workspace.WorkspaceRepository;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.core.util.LumifyLogger;
import com.altamiracorp.lumify.core.util.LumifyLoggerFactory;
import com.altamiracorp.lumify.web.BaseRequestHandler;
import com.altamiracorp.miniweb.HandlerChain;
import com.google.inject.Inject;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class WorkspaceByRowKey extends BaseRequestHandler {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(WorkspaceByRowKey.class);
    private final WorkspaceRepository workspaceRepository;

    @Inject
    public WorkspaceByRowKey(final WorkspaceRepository workspaceRepo) {
        workspaceRepository = workspaceRepo;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        final String workspaceRowKey = getAttributeString(request, "workspaceRowKey");
        final User authUser = getUser(request);

        LOGGER.info("Attempting to retrieve workspace: %s", workspaceRowKey);
        final Workspace workspace = workspaceRepository.findByRowKey(workspaceRowKey.toString(), authUser.getModelUserContext());

        if (workspace == null) {
            LOGGER.warn("Could not find workspace: %s", workspaceRowKey);
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        } else {
            LOGGER.debug("Successfully found workspace");
            request.getSession().setAttribute("activeWorkspace", workspaceRowKey);
            final JSONObject resultJSON = workspace.toJson(authUser);
            if (resultJSON == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
            } else {
                resultJSON.put("id", workspace.getRowKey().toString());

                if (workspace.getContent().getData() != null) {
                    resultJSON.put("data", new JSONObject(workspace.getContent().getData()));
                }

                respondWithJson(response, resultJSON);
            }
        }

        chain.next(request, response);
    }
}
