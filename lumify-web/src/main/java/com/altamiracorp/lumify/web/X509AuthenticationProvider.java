package com.altamiracorp.lumify.web;

import com.altamiracorp.lumify.core.model.user.UserRepository;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.core.util.LumifyLogger;
import com.altamiracorp.lumify.core.util.LumifyLoggerFactory;
import com.altamiracorp.miniweb.HandlerChain;
import com.altamiracorp.securegraph.Graph;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;

public abstract class X509AuthenticationProvider extends AuthenticationProvider {
    public static final String CERTIFICATE_REQUEST_ATTRIBUTE = "javax.servlet.request.X509Certificate";

    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(X509AuthenticationProvider.class);
    protected static final String X509_USER_PASSWORD = "N/A";
    private final UserRepository userRepository;
    private final Graph graph;

    protected X509AuthenticationProvider(UserRepository userRepository, final Graph graph) {
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

        User user = getUser(request, cert);
        if(user == null){
            respondWithAuthenticationFailure(response);
            return;
        }
        setUser(request, user);
        chain.next(request, response);
    }

    protected User getUser(HttpServletRequest request, X509Certificate cert) {
        String username = getUsername(cert);
        if (username == null || username.trim().equals("")) {
            return null;
        }
        return userRepository.findOrAddUser(username, username, X509_USER_PASSWORD, new String[0]);
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
        String dn = cert.getSubjectX500Principal().getName();
        try {
            return getCn(dn);
        } catch (InvalidNameException e) {
            LOGGER.error("Unable to parse CN from X509 certificate DN: %s", dn);
            return null;
        }
    }

    private String getCn(String dn) throws InvalidNameException {
        LdapName ldapDN = new LdapName(dn);
        for (Rdn rdn : ldapDN.getRdns()) {
            if (rdn.getType().equalsIgnoreCase("CN")) {
                return rdn.getValue().toString();
            }
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
