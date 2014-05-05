package io.lumify.web.routes.user;

import io.lumify.core.config.Configuration;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.model.workspace.WorkspaceRepository;
import io.lumify.core.user.User;
import io.lumify.web.BaseRequestHandler;
import com.altamiracorp.miniweb.HandlerChain;
import com.google.inject.Inject;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class UserInfo extends BaseRequestHandler {
    @Inject
    public UserInfo(
            final UserRepository userRepository,
            final WorkspaceRepository workspaceRepository,
            final Configuration configuration) {
        super(userRepository, workspaceRepository, configuration);
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        String userId = getRequiredParameter(request, "userId");

        User user = getUserRepository().findById(userId);
        if (user == null) {
            respondWithNotFound(response, "Could not find user with id: " + userId);
            return;
        }

        JSONObject json = UserRepository.toJson(user);
        respondWithJson(response, json);
    }
}
