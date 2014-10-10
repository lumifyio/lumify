package io.lumify.web.routes.workspace;

import com.google.inject.Inject;
import io.lumify.core.config.Configuration;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.model.workspace.Workspace;
import io.lumify.core.model.workspace.WorkspaceRepository;
import io.lumify.core.user.User;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import io.lumify.miniweb.HandlerChain;
import io.lumify.web.BaseRequestHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class WorkspaceCreate extends BaseRequestHandler {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(WorkspaceCreate.class);
    private static final String DEFAULT_WORKSPACE_TITLE = "Default";

    private final WorkspaceRepository workspaceRepository;

    @Inject
    public WorkspaceCreate(
            final WorkspaceRepository workspaceRepository,
            final UserRepository userRepository,
            final Configuration configuration) {
        super(userRepository, workspaceRepository, configuration);
        this.workspaceRepository = workspaceRepository;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        User authUser = getUser(request);
        User user = getUserRepository().findById(authUser.getUserId());

        String title = getOptionalParameter(request, "title");

        Workspace workspace = handle(title, user, authUser);
        respondWithClientApiObject(response, workspaceRepository.toClientApi(workspace, user, true));
    }

    public Workspace handle(String title, User user, User authUser) {
        Workspace workspace;
        if (title == null) {
            title = DEFAULT_WORKSPACE_TITLE + " - " + user.getDisplayName();
        }
        workspace = workspaceRepository.add(title, authUser);

        LOGGER.info("Created workspace: %s, title: %s", workspace.getWorkspaceId(), workspace.getDisplayTitle());
        return workspace;
    }
}
