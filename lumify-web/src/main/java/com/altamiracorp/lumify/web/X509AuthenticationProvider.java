package com.altamiracorp.lumify.web;

import com.altamiracorp.lumify.core.model.user.UserRepository;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.core.util.LumifyLogger;
import com.altamiracorp.lumify.core.util.LumifyLoggerFactory;
import com.altamiracorp.miniweb.HandlerChain;
import com.altamiracorp.securegraph.Graph;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;

public abstract class X509AuthenticationProvider extends AuthenticationProvider {
    public static final String CERTIFICATE_REQUEST_ATTRIBUTE = "javax.servlet.request.X509Certificate";

    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(X509AuthenticationProvider.class);
    private static final String X509_USER_PASSWORD = "P1OpQsfZMFizHqqyt7lXNE56a6HSVQxdMJHClZ0hhZPhY1OrHvkfDwysDhvWrUIUZbIuEY09FH99qo9t0rjikwEaHK4u03yTLidY";
    private final UserRepository userRepository;
    private final Graph graph;

    protected abstract String getUsername(X509Certificate cert);

    protected X509AuthenticationProvider(UserRepository userRepository,
                                         final Graph graph) {
        this.userRepository = userRepository;
        this.graph = graph;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        X509Certificate cert = extractCertificate(request);
        if (isInvalid(cert)) {
            respondWithAuthenticationFailure(response);
            return;
        }

        String username = getUsername(cert);
        if (username == null || username.trim().equals("")) {
            respondWithAuthenticationFailure(response);
            return;
        }

        User authUser = userRepository.findByUserName(username);
        if (authUser == null) {
            authUser = userRepository.addUser(graph.getIdGenerator().nextId().toString(), username, X509_USER_PASSWORD, new String[0]);
        }
        setUser(request, authUser);
        chain.next(request, response);
    }

    protected boolean isInvalid(X509Certificate cert) {
        if (cert == null) {
            return true;
        }

        try {
            cert.checkValidity();
            return false;
        } catch (CertificateExpiredException e) {
            LOGGER.warn("Authentication attempt with expired certificate: %s", cert.getSubjectDN());
        } catch (CertificateNotYetValidException e) {
            LOGGER.warn("Authentication attempt with certificate that's not yet valid: %s", cert.getSubjectDN());
        }

        return true;
    }

    protected X509Certificate extractCertificate(HttpServletRequest request) {
        X509Certificate[] certs = (X509Certificate[]) request.getAttribute(CERTIFICATE_REQUEST_ATTRIBUTE);
        if (null != certs && certs.length > 0) {
            return certs[0];
        }
        return null;
    }

    protected void respondWithAuthenticationFailure(HttpServletResponse response) throws IOException {
        response.sendError(HttpServletResponse.SC_FORBIDDEN);
    }
}
