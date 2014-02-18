package com.altamiracorp.lumify.web.routes.workspace;

import com.altamiracorp.lumify.core.model.workspace.Workspace;
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

        JSONArray adds = dataJson.getJSONArray("adds");
        for (int i = 0; i < adds.length(); i++) {
            JSONObject add = adds.getJSONObject(i);
            LOGGER.debug("workspace add (%s): %s", workspace.getId(), add.toString());
            String entityId = add.getString("vertexId");
            JSONObject graphPosition = add.getJSONObject("graphPosition");
            int graphPositionX = graphPosition.getInt("x");
            int graphPositionY = graphPosition.getInt("y");
            workspaceRepository.addEntityToWorkspace(workspace, entityId, graphPositionX, graphPositionY, authUser);
        }

        JSONArray updates = dataJson.getJSONArray("updates");
        for (int i = 0; i < updates.length(); i++) {
            JSONObject update = updates.getJSONObject(i);
            LOGGER.debug("workspace update (%s): %s", workspace.getId(), update.toString());
            String entityId = update.getString("vertexId");
            JSONObject graphPosition = update.getJSONObject("graphPosition");
            int graphPositionX = graphPosition.getInt("x");
            int graphPositionY = graphPosition.getInt("y");
            workspaceRepository.updateEntityOnWorkspace(workspace, entityId, graphPositionX, graphPositionY, authUser);
        }

        JSONArray deletes = dataJson.getJSONArray("deletes");
        for (int i = 0; i < deletes.length(); i++) {
            String entityId = deletes.getString(i);
            LOGGER.debug("workspace delete (%s): %s", workspace.getId(), entityId);
            workspaceRepository.deleteEntityFromWorkspace(workspace, entityId, authUser);
        }

        JSONObject resultJson = new JSONObject();
        resultJson.put("result", "OK");
        respondWithJson(response, resultJson);
    }
}
