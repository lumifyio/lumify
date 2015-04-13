package io.lumify.web;

import io.lumify.core.exception.LumifyException;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.user.User;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import io.lumify.miniweb.HandlerChain;
import org.securegraph.Graph;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.List;

public abstract class X509AuthenticationHandler extends AuthenticationHandler {
    public static final String CERTIFICATE_REQUEST_ATTRIBUTE = "javax.servlet.request.X509Certificate";

    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(X509AuthenticationHandler.class);
    private final UserRepository userRepository;
    private final Graph graph;

    protected X509AuthenticationHandler(UserRepository userRepository, final Graph graph) {
        this.userRepository = userRepository;
        this.graph = graph;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        String userId = CurrentUser.get(request);
        if (userId == null) {
            X509Certificate cert = extractCertificate(request);
            if (isInvalid(cert)) {
                respondWithAuthenticationFailure(response);
                return;
            }

            User user = getUser(request, cert);
            if (user == null) {
                respondWithAuthenticationFailure(response);
                return;
            }
            userRepository.recordLogin(user, getRemoteAddr(request));
            CurrentUser.set(request, user.getUserId(), user.getUsername());
        }
        chain.next(request, response);
    }

    protected User getUser(HttpServletRequest request, X509Certificate cert) {
        String username = getUsername(cert);
        if (username == null || username.trim().equals("")) {
            return null;
        }
        String displayName = getDisplayName(cert);
        if (displayName == null || displayName.trim().equals("")) {
            return null;
        }
        String randomPassword = UserRepository.createRandomPassword();
        return userRepository.findOrAddUser(username, displayName, null, randomPassword, new String[0]);
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

    protected String getUsername(X509Certificate cert) {
        String dn = getDn(cert);
        if (dn != null) {
            return dn;
        } else {
            throw new LumifyException("failed to get DN from cert for username");
        }
    }

    protected String getDisplayName(X509Certificate cert) {
        String cn = getCn(cert);
        if (cn != null) {
            return cn;
        } else {
            throw new LumifyException("failed to get CN from cert for displayName");
        }
    }

    private String getDn(X509Certificate cert) {
        String dn = cert.getSubjectX500Principal().getName();
        LOGGER.debug("certificate DN is [%s]", dn);
        return dn;
    }

    private String getCn(X509Certificate cert) {
        String dn = getDn(cert);
        try {
            List<Rdn> rdns = new LdapName(dn).getRdns();
            for (int i = rdns.size() - 1; i >= 0; i--) {
                Rdn rdn = rdns.get(i);
                if (rdn.getType().equalsIgnoreCase("CN")) {
                    String cn = rdn.getValue().toString();
                    LOGGER.debug("certificate CN is [%s]", cn);
                    return cn;
                }
            }
        } catch (InvalidNameException ine) {
            return null;
        }
        return null;
    }

    protected void respondWithAuthenticationFailure(HttpServletResponse response) throws IOException {
        response.sendError(HttpServletResponse.SC_FORBIDDEN);
    }

    protected UserRepository getUserRepository() {
        return userRepository;
    }

    protected Graph getGraph() {
        return graph;
    }
}
