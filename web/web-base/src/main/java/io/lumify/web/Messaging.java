package io.lumify.web;

import com.google.inject.Inject;
import com.google.inject.Injector;
import org.apache.curator.framework.CuratorFramework;
import io.lumify.core.config.Configuration;
import io.lumify.core.exception.LumifyException;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.model.user.UserSessionCounterRepository;
import io.lumify.core.model.workQueue.WorkQueueRepository;
import io.lumify.core.model.workspace.Workspace;
import io.lumify.core.model.workspace.WorkspaceRepository;
import io.lumify.core.user.User;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import io.lumify.web.clientapi.model.UserStatus;
import org.apache.commons.lang.StringUtils;
import org.atmosphere.cache.UUIDBroadcasterCache;
import org.atmosphere.client.TrackMessageSizeInterceptor;
import org.atmosphere.config.service.AtmosphereHandlerService;
import org.atmosphere.cpr.*;
import org.atmosphere.interceptor.AtmosphereResourceLifecycleInterceptor;
import org.atmosphere.interceptor.BroadcastOnPostAtmosphereInterceptor;
import org.atmosphere.interceptor.HeartbeatInterceptor;
import org.atmosphere.interceptor.JavaScriptProtocol;
import org.json.JSONObject;

import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

@AtmosphereHandlerService(
        path = "/messaging",
        broadcasterCache = UUIDBroadcasterCache.class,
        interceptors = {
                AtmosphereResourceLifecycleInterceptor.class,
                BroadcastOnPostAtmosphereInterceptor.class,
                TrackMessageSizeInterceptor.class,
                HeartbeatInterceptor.class,
                JavaScriptProtocol.class
        })
public class Messaging implements AtmosphereHandler { //extends AbstractReflectorAtmosphereHandler {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(Messaging.class);

    private UserRepository userRepository;

    // TODO should we save off this broadcaster? When using the BroadcasterFactory
    //      we always get null when trying to get the default broadcaster
    private static Broadcaster broadcaster;
    private WorkspaceRepository workspaceRepository;
    private WorkQueueRepository workQueueRepository;
    private CuratorFramework curatorFramework;
    private Configuration configuration;
    private UserSessionCounterRepository userSessionCounterRepository;
    private boolean subscribedToBroadcast = false;

    @Override
    public void onRequest(AtmosphereResource resource) throws IOException {
        ensureInitialized(resource);

        String requestData = org.apache.commons.io.IOUtils.toString(resource.getRequest().getInputStream(), "UTF-8");
        try {
            if (!StringUtils.isBlank(requestData)) {
                processRequestData(resource, requestData);
            }
        } catch (Exception ex) {
            LOGGER.error("Could not handle async message: " + requestData, ex);
        }

        AtmosphereRequest req = resource.getRequest();
        if (req.getMethod().equalsIgnoreCase("GET")) {
            onOpen(resource);
            resource.suspend();
        } else if (req.getMethod().equalsIgnoreCase("POST")) {
            String line = req.getReader().readLine().trim();
            LOGGER.debug("onRequest() POST: %s", line);
            resource.getBroadcaster().broadcast(line);
        }
    }

    private void ensureInitialized(AtmosphereResource resource) {
        if (userRepository == null) {
            Injector injector = (Injector) resource.getAtmosphereConfig().getServletContext().getAttribute(Injector.class.getName());
            injector.injectMembers(this);
        }

        if (!subscribedToBroadcast) {
            this.workQueueRepository.subscribeToBroadcastMessages(new WorkQueueRepository.BroadcastConsumer() {
                @Override
                public void broadcastReceived(JSONObject json) {
                    if (broadcaster != null) {
                        broadcaster.broadcast(json.toString());
                    }
                }
            });
            subscribedToBroadcast = true;
        }
        broadcaster = resource.getBroadcaster();

        if (userSessionCounterRepository == null) {
            userSessionCounterRepository = new UserSessionCounterRepository(curatorFramework, configuration);
        }
    }

    @Override
    public void destroy() {
        LOGGER.debug("destroy");
    }

    @Override
    public void onStateChange(AtmosphereResourceEvent event) throws IOException {
        ensureInitialized(event.getResource());
        AtmosphereResponse response = ((AtmosphereResourceImpl) event.getResource()).getResponse(false);

        if (event.getMessage() != null && List.class.isAssignableFrom(event.getMessage().getClass())) {
            List<String> messages = List.class.cast(event.getMessage());
            for (String t : messages) {
                onMessage(event, response, t);
            }

        } else if (event.isClosedByApplication() || event.isClosedByClient() || event.isCancelled()) {
            onDisconnect(event, response);
        } else if (event.isSuspended()) {
            onMessage(event, response, (String) event.getMessage());
        } else if (event.isResuming()) {
            onResume(event, response);
        } else if (event.isResumedOnTimeout()) {
            onTimeout(event, response);
        }
    }

    public void onOpen(AtmosphereResource resource) throws IOException {
        setStatus(resource, UserStatus.ACTIVE);
        incrementUserSessionCount(resource);
    }

    public void onResume(AtmosphereResourceEvent event, AtmosphereResponse response) throws IOException {
        LOGGER.debug("onResume");
    }

    public void onTimeout(AtmosphereResourceEvent event, AtmosphereResponse response) throws IOException {
        LOGGER.debug("onTimeout");
    }

    public void onDisconnect(AtmosphereResourceEvent event, AtmosphereResponse response) throws IOException {
        onDisconnectOrClose(event);
    }

    public void onClose(AtmosphereResourceEvent event, AtmosphereResponse response) {
        onDisconnectOrClose(event);
    }

    private void onDisconnectOrClose(AtmosphereResourceEvent event) {
        boolean lastSession = decrementUserSessionCount(event.getResource());
        if (lastSession) {
            LOGGER.info("last session for user %s", getCurrentUserId(event.getResource()));
            setStatus(event.getResource(), UserStatus.OFFLINE);
        }
    }

    public void onMessage(AtmosphereResourceEvent event, AtmosphereResponse response, String message) throws IOException {
        try {
            if (!StringUtils.isBlank(message)) {
                processRequestData(event.getResource(), message);
            }
        } catch (Exception ex) {
            LOGGER.error("Could not handle async message: " + message, ex);
        }
        if (message != null) {
            response.write(message);
        } else {
            onDisconnectOrClose(event);
        }
    }

    private void processRequestData(AtmosphereResource resource, String message) {
        JSONObject messageJson = new JSONObject(message);

        String type = messageJson.optString("type");
        if (type == null) {
            return;
        }

        JSONObject dataJson = messageJson.optJSONObject("data");
        if (dataJson == null) {
            return;
        }

        if ("setActiveWorkspace".equals(type)) {
            String authUserId = getCurrentUserId(resource);
            String workspaceId = dataJson.getString("workspaceId");
            String userId = dataJson.getString("userId");
            if (userId.equals(authUserId)) {
                switchWorkspace(authUserId, workspaceId);
            }
        }
    }

    private void switchWorkspace(String authUserId, String workspaceId) {
        if (!workspaceId.equals(userRepository.getCurrentWorkspaceId(authUserId))) {
            User authUser = userRepository.findById(authUserId);
            Workspace workspace = workspaceRepository.findById(workspaceId, authUser);
            userRepository.setCurrentWorkspace(authUserId, workspace.getWorkspaceId());
            workQueueRepository.pushUserCurrentWorkspaceChange(authUser, workspace.getWorkspaceId());

            LOGGER.debug("User %s switched current workspace to %s", authUserId, workspaceId);
        }
    }

    private void setStatus(AtmosphereResource resource, UserStatus status) {
        broadcaster = resource.getBroadcaster();
        try {
            String authUserId = CurrentUser.get(resource.getRequest());
            if (authUserId == null) {
                throw new RuntimeException("Could not find user in session");
            }
            User authUser = userRepository.findById(authUserId);

            LOGGER.debug("Setting user %s status to %s", authUserId, status.toString());
            userRepository.setStatus(authUserId, status);

            this.workQueueRepository.pushUserStatusChange(authUser, status);
        } catch (Exception ex) {
            LOGGER.error("Could not update status", ex);
        } finally {
            // TODO session is held open by getAppSession
            // session.close();
        }
    }

    private void incrementUserSessionCount(AtmosphereResource resource) {
        String userId = getCurrentUserId(resource);
        userSessionCounterRepository.incrementAndGet(userId);
    }

    private boolean decrementUserSessionCount(AtmosphereResource resource) {
        String userId = getCurrentUserId(resource);
        return userSessionCounterRepository.decrementAndGet(userId) < 1;
    }

    private String getCurrentUserId(AtmosphereResource resource) {
        String userId = CurrentUser.get(resource.getRequest());
        if (userId != null && userId.trim().length() > 0) {
            return userId;
        }
        throw new LumifyException("failed to get a current userId via an AtmosphereResource");
    }

    @Inject
    public void setUserRepository(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Inject
    public void setWorkspaceRepository(WorkspaceRepository workspaceRepository) {
        this.workspaceRepository = workspaceRepository;
    }

    @Inject
    public void setWorkQueueRepository(WorkQueueRepository workQueueRepository) {
        this.workQueueRepository = workQueueRepository;
    }

    @Inject
    public void setCuratorFramework(CuratorFramework curatorFramework) {
        this.curatorFramework = curatorFramework;
    }

    @Inject
    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }
}
