package com.altamiracorp.lumify.web.routes.workspace;

import com.altamiracorp.lumify.core.model.workspace.Workspace;
import com.altamiracorp.lumify.core.model.workspace.WorkspaceAccess;
import com.altamiracorp.lumify.core.model.workspace.WorkspaceRepository;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.core.util.LumifyLogger;
import com.altamiracorp.lumify.core.util.LumifyLoggerFactory;
import com.altamiracorp.lumify.web.BaseRequestHandler;
import com.altamiracorp.miniweb.HandlerChain;
import com.google.inject.Inject;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class WorkspaceUpdate extends BaseRequestHandler {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(WorkspaceUpdate.class);
    private final WorkspaceRepository workspaceRepository;

    @Inject
    public WorkspaceUpdate(final WorkspaceRepository workspaceRepository) {
        this.workspaceRepository = workspaceRepository;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        final String workspaceId = getAttributeString(request, "workspaceId");
        final String data = getRequiredParameter(request, "data");

        User authUser = getUser(request);

        Workspace workspace = workspaceRepository.findById(workspaceId, authUser);
        if (workspace == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        JSONObject dataJson = new JSONObject(data);

        if (dataJson.has("title")) {
            setTitle(workspace, dataJson.getString("title"), authUser);
        }

        JSONArray entityUpdates = dataJson.getJSONArray("entityUpdates");
        updateEntities(workspace, entityUpdates, authUser);

        JSONArray entityDeletes = dataJson.getJSONArray("entityDeletes");
        deleteEntities(workspace, entityDeletes, authUser);

        JSONArray userUpdates = dataJson.getJSONArray("userUpdates");
        updateUsers(workspace, userUpdates, authUser);

        JSONArray userDeletes = dataJson.getJSONArray("userDeletes");
        deleteUsers(workspace, userDeletes, authUser);

        JSONObject resultJson = new JSONObject();
        resultJson.put("result", "OK");
        respondWithJson(response, resultJson);
    }

    private void setTitle(Workspace workspace, String title, User authUser) {
        LOGGER.debug("setting title (%s): %s", workspace.getId(), title);
        workspaceRepository.setTitle(workspace, title, authUser);
    }

    private void deleteUsers(Workspace workspace, JSONArray userDeletes, User authUser) {
        for (int i = 0; i < userDeletes.length(); i++) {
            String userId = userDeletes.getString(i);
            LOGGER.debug("user delete (%s): %s", workspace.getId(), userId);
            workspaceRepository.deleteUserFromWorkspace(workspace, userId, authUser);
        }
    }

    private void updateUsers(Workspace workspace, JSONArray userUpdates, User authUser) {
        for (int i = 0; i < userUpdates.length(); i++) {
            JSONObject update = userUpdates.getJSONObject(i);
            LOGGER.debug("user update (%s): %s", workspace.getId(), update.toString());
            String userId = update.getString("userId");
            WorkspaceAccess workspaceAccess = WorkspaceAccess.valueOf(update.getString("access"));
            workspaceRepository.updateUserOnWorkspace(workspace, userId, workspaceAccess, authUser);
        }
    }

    private void deleteEntities(Workspace workspace, JSONArray entityDeletes, User authUser) {
        for (int i = 0; i < entityDeletes.length(); i++) {
            String entityId = entityDeletes.getString(i);
            LOGGER.debug("workspace delete (%s): %s", workspace.getId(), entityId);
            workspaceRepository.deleteEntityFromWorkspace(workspace, entityId, authUser);
        }
    }

    private void updateEntities(Workspace workspace, JSONArray entityUpdates, User authUser) {
        for (int i = 0; i < entityUpdates.length(); i++) {
            JSONObject update = entityUpdates.getJSONObject(i);
            LOGGER.debug("workspace update (%s): %s", workspace.getId(), update.toString());
            String entityId = update.getString("vertexId");
            JSONObject graphPosition = update.getJSONObject("graphPosition");
            int graphPositionX = graphPosition.getInt("x");
            int graphPositionY = graphPosition.getInt("y");
            workspaceRepository.updateEntityOnWorkspace(workspace, entityId, graphPositionX, graphPositionY, authUser);
        }
    }
}
