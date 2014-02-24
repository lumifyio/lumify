package com.altamiracorp.lumify.web.routes.workspace;

import com.altamiracorp.lumify.core.model.user.UserLumifyProperties;
import com.altamiracorp.lumify.core.model.user.UserRepository;
import com.altamiracorp.lumify.core.model.workspace.Workspace;
import com.altamiracorp.lumify.core.model.workspace.WorkspaceRepository;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.core.util.LumifyLogger;
import com.altamiracorp.lumify.core.util.LumifyLoggerFactory;
import com.altamiracorp.lumify.web.BaseRequestHandler;
import com.altamiracorp.miniweb.HandlerChain;
import com.altamiracorp.securegraph.Vertex;
import com.google.inject.Inject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class WorkspaceNew extends BaseRequestHandler {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(WorkspaceNew.class);
    private static final String DEFAULT_WORKSPACE_TITLE = "Default";

    private final WorkspaceRepository workspaceRepository;
    private final UserRepository userRepository;

    @Inject
    public WorkspaceNew(final WorkspaceRepository workspaceRepository, final UserRepository userRepository) {
        this.workspaceRepository = workspaceRepository;
        this.userRepository = userRepository;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        User authUser = getUser(request);
        Vertex user = userRepository.findByUserName(authUser.getUsername());

        String title = getOptionalParameter(request, "title");

        Workspace workspace;
        if (title == null) {
            title = DEFAULT_WORKSPACE_TITLE + " - " + UserLumifyProperties.USERNAME.getPropertyValue(user);
        }
        workspace = workspaceRepository.add(title, authUser);

        LOGGER.info("Created workspace: %s, title: %s", workspace.getId(), workspace.getTitle());

        respondWithJson(response, workspace.toJson(true));
    }
}
