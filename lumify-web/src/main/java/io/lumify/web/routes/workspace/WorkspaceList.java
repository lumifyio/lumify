package io.lumify.web.routes.workspace;

import com.altamiracorp.miniweb.HandlerChain;
import com.google.inject.Inject;
import io.lumify.core.config.Configuration;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.model.workspace.Workspace;
import io.lumify.core.model.workspace.WorkspaceRepository;
import io.lumify.core.user.User;
import io.lumify.web.BaseRequestHandler;
import org.json.JSONArray;
import org.json.JSONObject;

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

        Iterable<Workspace> workspaces = workspaceRepository.findAll(user);
        String activeWorkspaceId = getUserRepository().getCurrentWorkspaceId(user.getUserId());
        activeWorkspaceId = activeWorkspaceId != null ? activeWorkspaceId : "";
        JSONArray workspacesJson = new JSONArray();
        for (Workspace workspace : workspaces) {
            JSONObject workspaceJson = workspaceRepository.toJson(workspace, user, false);
            if (workspaceJson != null) {
                if (activeWorkspaceId.equals(workspace.getId())) { //if its the active one
                    workspaceJson.put("active", true);
                }
                workspacesJson.put(workspaceJson);
            }
        }

        JSONObject json = new JSONObject();
        json.put("workspaces", workspacesJson);

        respondWithJson(response, json);
        chain.next(request, response);
    }
}
