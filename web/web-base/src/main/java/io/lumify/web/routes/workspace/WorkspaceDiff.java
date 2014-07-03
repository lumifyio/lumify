package io.lumify.web.routes.workspace;

import io.lumify.core.config.Configuration;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.model.workspace.Workspace;
import io.lumify.core.model.workspace.WorkspaceRepository;
import io.lumify.core.model.workspace.diff.DiffItem;
import io.lumify.core.user.User;
import io.lumify.web.BaseRequestHandler;
import com.altamiracorp.miniweb.HandlerChain;
import com.google.inject.Inject;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

public class WorkspaceDiff extends BaseRequestHandler {
    private final WorkspaceRepository workspaceRepository;

    @Inject
    public WorkspaceDiff(
            final WorkspaceRepository workspaceRepository,
            final UserRepository userRepository,
            final Configuration configuration) {
        super(userRepository, workspaceRepository, configuration);
        this.workspaceRepository = workspaceRepository;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        User user = getUser(request);
        String workspaceId = getActiveWorkspaceId(request);
        Workspace workspace = workspaceRepository.findById(workspaceId, user);
        if (workspace == null) {
            respondWithNotFound(response);
            return;
        }

        List<DiffItem> diffItems = this.workspaceRepository.getDiff(workspace, user);
        JSONObject result = new JSONObject();
        JSONArray diffItemsJson = DiffItem.toJson(diffItems);
        result.put("diffs", diffItemsJson);
        respondWithJson(response, result);
    }
}
