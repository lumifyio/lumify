package io.lumify.web.routes.user;

import com.google.inject.Inject;
import io.lumify.core.config.Configuration;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.model.workspace.Workspace;
import io.lumify.core.model.workspace.WorkspaceRepository;
import io.lumify.core.model.workspace.WorkspaceUser;
import io.lumify.core.user.User;
import io.lumify.miniweb.HandlerChain;
import io.lumify.web.BaseRequestHandler;
import io.lumify.web.clientapi.model.ClientApiUsers;
import org.securegraph.util.ConvertingIterable;
import org.securegraph.util.FilterIterable;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.sun.tools.javac.util.Assert.checkNull;
import static org.securegraph.util.IterableUtils.toList;

public class UserList extends BaseRequestHandler {
    @Inject
    public UserList(
            final UserRepository userRepository,
            final WorkspaceRepository workspaceRepository,
            final Configuration configuration) {
        super(userRepository, workspaceRepository, configuration);
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        String query = getOptionalParameter(request, "q");
        String workspaceId = getOptionalParameter(request, "workspaceId");
        String[] userIds = getOptionalParameterArray(request, "userIds[]");
        User user = getUser(request);

        List<User> users;
        if (userIds != null) {
            checkNull(query, "Cannot use userIds[] and q at the same time");
            checkNull(query, "Cannot use userIds[] and workspaceId at the same time");
            users = new ArrayList<User>();
            for (String userId : userIds) {
                User u = getUserRepository().findById(userId);
                if (u == null) {
                    respondWithNotFound(response, "User " + userId + " not found");
                    return;
                }
                users.add(u);
            }
        } else {
            users = toList(getUserRepository().find(query));

            if (workspaceId != null) {
                users = toList(getUsersWithWorkspaceAccess(workspaceId, users, user));
            }
        }

        Iterable<String> workspaceIds = getCurrentWorkspaceIds(users);
        Map<String, String> workspaceNames = getWorkspaceNames(workspaceIds, user);

        ClientApiUsers clientApiUsers = getUserRepository().toClientApi(users, workspaceNames);
        respondWithClientApiObject(response, clientApiUsers);
    }

    private Map<String, String> getWorkspaceNames(Iterable<String> workspaceIds, User user) {
        Map<String, String> result = new HashMap<String, String>();
        for (Workspace workspace : getWorkspaceRepository().findByIds(workspaceIds, user)) {
            if (workspace != null) {
                result.put(workspace.getWorkspaceId(), workspace.getDisplayTitle());
            }
        }
        return result;
    }

    private Iterable<String> getCurrentWorkspaceIds(Iterable<User> users) {
        return new ConvertingIterable<User, String>(users) {
            @Override
            protected String convert(User user) {
                return user.getCurrentWorkspaceId();
            }
        };
    }

    private Iterable<User> getUsersWithWorkspaceAccess(String workspaceId, final Iterable<User> users, User user) {
        final List<WorkspaceUser> usersWithAccess = getWorkspaceRepository().findUsersWithAccess(workspaceId, user);
        return new FilterIterable<User>(users) {
            @Override
            protected boolean isIncluded(User u) {
                return contains(usersWithAccess, u);
            }

            private boolean contains(List<WorkspaceUser> usersWithAccess, User u) {
                for (WorkspaceUser userWithAccess : usersWithAccess) {
                    if (userWithAccess.getUserId().equals(u.getUserId())) {
                        return true;
                    }
                }
                return false;
            }
        };
    }
}
