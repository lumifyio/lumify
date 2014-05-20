package io.lumify.web.auth;

import com.google.inject.Inject;
import io.lumify.core.model.user.UserRepository;
import io.lumify.web.X509AuthenticationHandler;
import org.securegraph.Graph;

public class X509IdentityAuthenticationHandler extends X509AuthenticationHandler {
    // default behavior is all that's needed

    @Inject
    public X509IdentityAuthenticationHandler(UserRepository userRepository, Graph graph) {
        super(userRepository, graph);
    }
}
