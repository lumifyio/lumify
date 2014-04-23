package io.lumify.web;

import io.lumify.core.model.user.UserRepository;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import org.securegraph.Graph;
import com.google.inject.Inject;

import javax.servlet.http.HttpServletRequest;

public class DevX509AuthenticationProvider extends X509AuthenticationProvider {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(DevX509AuthenticationProvider.class);

    @Inject
    public DevX509AuthenticationProvider(final UserRepository userRepository, final Graph graph) {
        super(userRepository, graph);
    }

    @Override
    public boolean login(HttpServletRequest request) {
        return false;
    }
}
