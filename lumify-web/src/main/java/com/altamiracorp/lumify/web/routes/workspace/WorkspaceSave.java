package com.altamiracorp.lumify.web.routes.workspace;

import com.altamiracorp.bigtable.model.Column;
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
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

public class WorkspaceSave extends BaseRequestHandler {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(WorkspaceSave.class);
    private static final String DEFAULT_WORKSPACE_TITLE = "Default";

    private final WorkspaceRepository workspaceRepository;
    private final UserRepository userRepository;

    @Inject
    public WorkspaceSave(final WorkspaceRepository workspaceRepository, final UserRepository userRepository) {
        this.workspaceRepository = workspaceRepository;
        this.userRepository = userRepository;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        User authUser = getUser(request);
        Vertex user = userRepository.findByUserName(authUser.getUsername());

        final String data = getOptionalParameter(request, "data");
        final String users = getOptionalParameter(request, "users");
        String title = getOptionalParameter(request, "title");
        final String workspaceId = getAttributeString(request, "workspaceId");

        Workspace workspace;
        if (workspaceId == null) {
            if (title == null) {
                title = DEFAULT_WORKSPACE_TITLE + " - " + UserLumifyProperties.USERNAME.getPropertyValue(user);
            }
            workspace = workspaceRepository.add(title, authUser);
        } else {
            workspace = workspaceRepository.findById(workspaceId, authUser);
            if (title != null) {
                workspaceRepository.setTitle(workspace, title);
            }
        }

        LOGGER.info("Saved workspace: %s\ntitle: %s\ndata: %s", workspace.getId(), workspace.getTitle(), data);

        if (users != null) {
            // Getting user permissions
            JSONArray userList = new JSONArray(users);
            String userId = user.getId().toString();

            if (hasWritePermissions(userId, workspace)) {
                updateUserList(workspace, userList, authUser);
            }
        }

        respondWithJson(response, workspace.toJson(true));
    }

    private void updateUserList(Workspace workspace, JSONArray userList, com.altamiracorp.lumify.core.user.User user) {
        throw new RuntimeException("TODO workspace");
//        boolean updateList = false;
//        if (workspace.get(WorkspacePermissions.NAME) != null) {
//            updateList = workspace.get(WorkspacePermissions.NAME).getColumns().size() > 0;
//        } else {
//            workspace.getPermissions();
//        }
//
//        List<String> users = new ArrayList<String>();
//
//        for (int i = 0; i < userList.length(); i++) {
//            JSONObject obj = userList.getJSONObject(i);
//            workspace.get(WorkspacePermissions.NAME).set(obj.getString("user"), obj.getJSONObject("userPermissions"));
//            if (updateList) {
//                users.add(obj.getString("user"));
//            }
//        }
//
//        if (updateList) {
//            for (Column col : workspace.get(WorkspacePermissions.NAME).getColumns()) {
//                if (!users.contains(col.getName())) {
//                    col.setDelete(true);
//                }
//            }
//        }
    }

    private boolean hasWritePermissions(String user, Workspace workspace) {
        throw new RuntimeException("TODO workspace");
//        if (workspace.get(WorkspacePermissions.NAME) != null && workspace.get(WorkspacePermissions.NAME).get(user) != null) {
//            JSONObject permissions = new JSONObject(workspace.get(WorkspacePermissions.NAME).get(user).toString());
//            if (permissions.length() > 0 && permissions.getBoolean("edit")) {
//                return true;
//            }
//        }
//
//        return false;
    }
}
