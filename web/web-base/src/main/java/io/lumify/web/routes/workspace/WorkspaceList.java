package io.lumify.web.routes.workspace;

import com.google.inject.Inject;
import io.lumify.core.config.Configuration;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.model.workspace.Workspace;
import io.lumify.core.model.workspace.WorkspaceRepository;
import io.lumify.core.user.User;
import io.lumify.miniweb.HandlerChain;
import io.lumify.web.BaseRequestHandler;
import io.lumify.web.clientapi.model.Workspaces;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class WorkspaceList extends BaseRequestHandler {
    private final WorkspaceRepository workspaceRepository;

    @Inject
    public WorkspaceList(
            final WorkspaceRepository workspaceRepository,
            final UserRepository userRepository,
            final Configuration configuration) {
        super(userRepository, workspaceRepository, configuration);
        this.workspaceRepository = workspaceRepository;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        User user = getUser(request);
        Workspaces results = handle(user);
        respondWith(response, results);
    }

    public Workspaces handle(User user) {
        Iterable<Workspace> workspaces = workspaceRepository.findAll(user);
        String activeWorkspaceId = getUserRepository().getCurrentWorkspaceId(user.getUserId());
        activeWorkspaceId = activeWorkspaceId != null ? activeWorkspaceId : "";

        Workspaces results = new Workspaces();
        for (Workspace workspace : workspaces) {
            io.lumify.web.clientapi.model.Workspace workspaceClientApi = workspaceRepository.toClientApi(workspace, user, false);
            if (workspaceClientApi != null) {
                if (activeWorkspaceId.equals(workspace.getWorkspaceId())) { //if its the active one
                    workspaceClientApi.setActive(true);
                }
                results.addWorkspace(workspaceClientApi);
            }
        }
        return results;
    }
}
