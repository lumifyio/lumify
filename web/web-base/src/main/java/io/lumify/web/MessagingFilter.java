package io.lumify.web;

import com.google.inject.Inject;
import io.lumify.core.bootstrap.InjectHelper;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.BroadcastFilter;
import org.atmosphere.cpr.PerRequestBroadcastFilter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import static com.google.common.base.Preconditions.checkNotNull;

public class MessagingFilter implements PerRequestBroadcastFilter {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(MessagingFilter.class);
    private UserRepository userRepository;


    @Override
    public BroadcastAction filter(String broadcasterId, Object originalMessage, Object message) {
        return new BroadcastAction(message);
    }

    @Override
    public BroadcastAction filter(String broadcasterId, AtmosphereResource r, Object originalMessage, Object message) {
        ensureInitialized();

        try {
            JSONObject json = new JSONObject("" + originalMessage);

            String type = json.optString("type");
            if (type != null && type.equals("setActiveWorkspace")) {
                return new BroadcastAction(BroadcastAction.ACTION.ABORT, message);
            }

            JSONObject permissionsJson = json.optJSONObject("permissions");
            if (permissionsJson == null) {
                return new BroadcastAction(message);
            }

            JSONArray users = permissionsJson.optJSONArray("users");
            if (users != null) {
                String currentUserId = CurrentUser.get(r.getRequest().getSession());
                if (!isUserInList(users, currentUserId)) {
                    return new BroadcastAction(BroadcastAction.ACTION.ABORT, message);
                }
            }

            JSONArray sessionIds = permissionsJson.optJSONArray("sessionIds");
            if (sessionIds != null) {
                String currentSessionId = r.getRequest().getSession().getId();
                if (!isSessionIdInList(sessionIds, currentSessionId)) {
                    return new BroadcastAction(BroadcastAction.ACTION.ABORT, message);
                }
            }

            JSONArray workspaces = permissionsJson.optJSONArray("workspaces");
            if (workspaces != null) {
                String currentUserId = CurrentUser.get(r.getRequest().getSession());
                if (!isWorkspaceInList(workspaces, userRepository.getCurrentWorkspaceId(currentUserId))) {
                    return new BroadcastAction(BroadcastAction.ACTION.ABORT, message);
                }
            }

            return new BroadcastAction(message);
        } catch (JSONException e) {
            LOGGER.error("Failed to filter message:\n" + originalMessage, e);
            return new BroadcastAction(BroadcastAction.ACTION.ABORT, message);
        }
    }

    public void ensureInitialized() {
        if (userRepository == null) {
            InjectHelper.inject(this);
            if (userRepository == null) {
                LOGGER.error("userRepository cannot be null");
                checkNotNull(userRepository, "userRepository cannot be null");
            }
        }
    }

    private boolean isWorkspaceInList(JSONArray workspaces, String currentWorkspace) throws JSONException {
        for (int i = 0; i < workspaces.length(); i++) {
            String workspaceItemRowKey = workspaces.getString(i);
            if (workspaceItemRowKey.equals(currentWorkspace)) {
                return true;
            }
        }
        return false;
    }

    private boolean isUserInList(JSONArray users, String userId) throws JSONException {
        for (int i = 0; i < users.length(); i++) {
            String userItemId = users.getString(i);
            if (userItemId.equals(userId)) {
                return true;
            }
        }
        return false;
    }

    private boolean isSessionIdInList(JSONArray sessionIds, String id) {
        for (int i = 0; i < sessionIds.length(); i++) {
            String sessionId = sessionIds.getString(i);
            if (sessionId.equals(id)) {
                return true;
            }
        }
        return false;
    }

    @Inject
    public void setUserRepository(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

}
