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
import com.altamiracorp.securegraph.Graph;
import com.google.inject.Inject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class WorkspaceNew extends BaseRequestHandler {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(WorkspaceNew.class);
    private static final String DEFAULT_WORKSPACE_TITLE = "Default";

    private final WorkspaceRepository workspaceRepository;
    private final Graph graph;

    @Inject
    public WorkspaceNew(
            final WorkspaceRepository workspaceRepository,
            final UserRepository userRepository,
            final Configuration configuration,
            final Graph graph) {
        super(userRepository, configuration);
        this.workspaceRepository = workspaceRepository;
        this.graph = graph;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        User authUser = getUser(request);
        User user = getUserRepository().findByUserName(authUser.getUserName());

        String title = getOptionalParameter(request, "title");

        Workspace workspace;
        if (title == null) {
            title = DEFAULT_WORKSPACE_TITLE + " - " + user.getUserName();
        }
        workspace = workspaceRepository.add(title, authUser);

        LOGGER.info("Created workspace: %s, title: %s", workspace.getId(), workspace.getTitle());

        respondWithJson(response, workspace.toJson(true));
    }
}
