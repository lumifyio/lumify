package com.altamiracorp.lumify.web.routes.workspace;

import com.altamiracorp.lumify.core.config.Configuration;
import com.altamiracorp.lumify.core.model.user.UserRepository;
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

public class WorkspaceById extends BaseRequestHandler {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(WorkspaceById.class);
    private final WorkspaceRepository workspaceRepository;

    @Inject
    public WorkspaceById(
            final WorkspaceRepository workspaceRepo,
            final UserRepository userRepository,
            final Configuration configuration) {
        super(userRepository, configuration);
        workspaceRepository = workspaceRepo;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        final String workspaceId = super.getAttributeString(request, "workspaceId");
        final User authUser = getUser(request);
        LOGGER.info("Attempting to retrieve workspace: %s", workspaceId);
        final Workspace workspace = workspaceRepository.findById(workspaceId, authUser);
        if (workspace == null) {
            LOGGER.warn("Could not find workspace: %s", workspaceId);
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        } else {
            LOGGER.debug("Successfully found workspace");
            request.getSession().setAttribute("activeWorkspace", workspaceId);
            final JSONObject resultJSON = workspace.toJson(true);
            respondWithJson(response, resultJSON);
        }

        chain.next(request, response);
    }
}
