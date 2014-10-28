package io.lumify.web.routes.longRunningProcess;

import com.google.inject.Inject;
import io.lumify.core.config.Configuration;
import io.lumify.core.model.longRunningProcess.LongRunningProcessRepository;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.model.workspace.WorkspaceRepository;
import io.lumify.core.user.User;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import io.lumify.miniweb.HandlerChain;
import io.lumify.web.BaseRequestHandler;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class LongRunningProcessCancel extends BaseRequestHandler {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(LongRunningProcessCancel.class);
    private final LongRunningProcessRepository longRunningProcessRepository;

    @Inject
    public LongRunningProcessCancel(
            final WorkspaceRepository workspaceRepo,
            final UserRepository userRepository,
            final WorkspaceRepository workspaceRepository,
            final Configuration configuration,
            final LongRunningProcessRepository longRunningProcessRepository) {
        super(userRepository, workspaceRepository, configuration);
        this.longRunningProcessRepository = longRunningProcessRepository;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        final String longRunningProcessId = getRequiredParameter(request, "longRunningProcessId");
        final User authUser = getUser(request);

        LOGGER.info("Attempting to cancel long running process: %s", longRunningProcessId);
        JSONObject longRunningProcess = longRunningProcessRepository.findById(longRunningProcessId, authUser);
        if (longRunningProcess == null) {
            LOGGER.warn("Could not find long running process: %s", longRunningProcessId);
            respondWithNotFound(response);
        } else {
            longRunningProcessRepository.cancel(longRunningProcessId, authUser);
            respondWithSuccessJson(response);
        }
    }
}
