package com.altamiracorp.lumify.web;

import com.altamiracorp.lumify.core.model.user.UserRepository;
import com.altamiracorp.lumify.core.util.LumifyLogger;
import com.altamiracorp.lumify.core.util.LumifyLoggerFactory;
import com.altamiracorp.securegraph.Graph;
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
