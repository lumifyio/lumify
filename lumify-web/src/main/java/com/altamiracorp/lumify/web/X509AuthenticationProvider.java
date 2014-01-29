package com.altamiracorp.lumify.web;

import com.altamiracorp.lumify.core.model.user.UserRepository;
import com.altamiracorp.lumify.core.model.user.UserRow;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.core.user.UserProvider;
import com.altamiracorp.lumify.core.util.LumifyLogger;
import com.altamiracorp.lumify.core.util.LumifyLoggerFactory;
import com.altamiracorp.miniweb.HandlerChain;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;

public abstract class X509AuthenticationProvider extends AuthenticationProvider {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(X509AuthenticationProvider.class);
    private final UserProvider userProvider;
    private final UserRepository userRepository;

    protected abstract String getUsername(X509Certificate cert);

    protected X509AuthenticationProvider(UserRepository userRepository, UserProvider userProvider) {
        this.userRepository = userRepository;
        this.userProvider = userProvider;
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

        UserRow userRow = userRepository.findOrAddUser(username, this.userProvider.getSystemUser());
        User authUser = this.userProvider.createFromModelUser(userRow);
        setUser(request, authUser);
        chain.next(request, response);
    }

    private boolean isInvalid(X509Certificate cert) {
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

    private X509Certificate extractCertificate(HttpServletRequest request) {
        X509Certificate[] certs = (X509Certificate[]) request.getAttribute("javax.servlet.request.X509Certificate");
        if (null != certs && certs.length > 0) {
            return certs[0];
        }
        return null;
    }

    private void respondWithAuthenticationFailure(HttpServletResponse response) throws IOException {
        response.sendError(HttpServletResponse.SC_FORBIDDEN);
    }
}
