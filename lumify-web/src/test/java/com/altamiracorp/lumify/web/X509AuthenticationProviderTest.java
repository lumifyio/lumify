package com.altamiracorp.lumify.web;

import com.altamiracorp.bigtable.model.ModelSession;
import com.altamiracorp.lumify.core.model.user.UserRow;
import com.altamiracorp.lumify.core.model.user.UserMetadata;
import com.altamiracorp.lumify.core.model.user.UserRepository;
import com.altamiracorp.lumify.core.model.user.UserRowKey;
import com.altamiracorp.miniweb.HandlerChain;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.powermock.reflect.Whitebox;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.X509Certificate;

import static org.mockito.Mockito.*;

public class X509AuthenticationProviderTest {
    public static final String X509_REQ_ATTR_NAME = "javax.servlet.request.X509Certificate";
    public static final String TEST_USERNAME = "testuser";

    private X509AuthenticationProvider mock;

    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private HandlerChain chain;
    @Mock
    private WebApp app;
    @Mock
    private ModelSession modelSession;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserRow user;
    @Mock
    private UserMetadata userMetadata;

    @Before
    public void setupTests() {
        mock = Mockito.mock(X509AuthenticationProvider.class);
        MockitoAnnotations.initMocks(this);
        Whitebox.setInternalState(mock, "userRepository", userRepository);
    }

    @Test
    public void testNoCertificateAvailable() throws Exception {
        when(request.getAttribute(X509_REQ_ATTR_NAME)).thenReturn(null);
        doCallRealMethod().when(mock).handle(request, response, chain);
        mock.handle(request, response, chain);
        verify(response).sendError(HttpServletResponse.SC_FORBIDDEN);
    }

    @Test
    public void testEmptyCertificateArrayAvailable() throws Exception {
        X509Certificate[] certs = new X509Certificate[]{};
        when(request.getAttribute(X509_REQ_ATTR_NAME)).thenReturn(certs);
        doCallRealMethod().when(mock).handle(request, response, chain);
        mock.handle(request, response, chain);
        verify(response).sendError(HttpServletResponse.SC_FORBIDDEN);
    }

    @Test
    public void testExpiredCertificate() throws Exception {
        X509Certificate cert = getCertificate("expired");
        X509Certificate[] certs = new X509Certificate[]{cert};
        when(request.getAttribute(X509_REQ_ATTR_NAME)).thenReturn(certs);
        when(mock.getUsername(cert)).thenReturn(TEST_USERNAME);
        doCallRealMethod().when(mock).handle(request, response, chain);
        mock.handle(request, response, chain);
        verify(response).sendError(HttpServletResponse.SC_FORBIDDEN);
    }

    @Test
    public void testValidCertificate() throws Exception {
        X509Certificate cert = getCertificate("valid");
        X509Certificate[] certs = new X509Certificate[]{cert};
        when(request.getAttribute(X509_REQ_ATTR_NAME)).thenReturn(certs);
        when(mock.getUsername(cert)).thenReturn(TEST_USERNAME);
        when(userRepository.findOrAddUser(eq(TEST_USERNAME), any(com.altamiracorp.lumify.core.user.User.class))).thenReturn(user);
        when(user.getRowKey()).thenReturn(new UserRowKey("rowkey"));
        when(user.getMetadata()).thenReturn(userMetadata);
        when(userMetadata.getUserName()).thenReturn(TEST_USERNAME);
        when(userMetadata.getCurrentWorkspace()).thenReturn("workspaceName");
        doCallRealMethod().when(mock).handle(request, response, chain);
        mock.handle(request, response, chain);
        verify(chain).next(request, response);
    }

    private X509Certificate getCertificate(String name) {
        try {
            KeyStore ks = KeyStore.getInstance("JKS");
            InputStream ksis = X509AuthenticationProviderTest.class.getResourceAsStream("/" + name + ".jks");
            ks.load(ksis, "password".toCharArray());
            return (X509Certificate) ks.getCertificate(name);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
