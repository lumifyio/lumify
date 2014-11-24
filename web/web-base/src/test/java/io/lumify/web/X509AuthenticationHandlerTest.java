package io.lumify.web;

import io.lumify.core.model.user.UserRepository;
import io.lumify.core.user.User;
import io.lumify.miniweb.HandlerChain;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.securegraph.Graph;
import org.securegraph.Vertex;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.X509Certificate;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class X509AuthenticationHandlerTest {
    public static final String X509_REQ_ATTR_NAME = "javax.servlet.request.X509Certificate";
    public static final String TEST_USERNAME = "testuser";

    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private HttpSession httpSession;
    @Mock
    private HandlerChain chain;
    @Mock
    private WebApp app;
    @Mock
    private UserRepository userRepository;
    @Mock
    private Graph graph;
    @Mock
    private Vertex userVertex;
    @Mock
    private User user;

    @Mock
    private Delegate delegate;

    private X509AuthenticationHandler instance;

    @Before
    public void setupTests() {
        instance = new TestX509AuthenticationHandler(userRepository, graph);

        when(request.getSession()).thenReturn(httpSession);
    }

    @Test
    public void testNoCertificateAvailable() throws Exception {
        when(request.getAttribute(X509_REQ_ATTR_NAME)).thenReturn(null);
        instance.handle(request, response, chain);
        verify(response).sendError(HttpServletResponse.SC_FORBIDDEN);
    }

    @Test
    public void testEmptyCertificateArrayAvailable() throws Exception {
        X509Certificate[] certs = new X509Certificate[0];
        when(request.getAttribute(X509_REQ_ATTR_NAME)).thenReturn(certs);
        instance.handle(request, response, chain);
        verify(response).sendError(HttpServletResponse.SC_FORBIDDEN);
    }

    @Test
    public void testExpiredCertificate() throws Exception {
        X509Certificate cert = getCertificate("expired");
        X509Certificate[] certs = new X509Certificate[]{cert};
        when(request.getAttribute(X509_REQ_ATTR_NAME)).thenReturn(certs);
        instance.handle(request, response, chain);
        verify(delegate, never()).getUsername(any(X509Certificate.class));
        verify(response).sendError(HttpServletResponse.SC_FORBIDDEN);
    }

    @Test
    public void testValidCertificate() throws Exception {
        X509Certificate cert = getCertificate("valid");
        X509Certificate[] certs = new X509Certificate[]{cert};
        when(request.getAttribute(X509_REQ_ATTR_NAME)).thenReturn(certs);
        when(delegate.getUsername(cert)).thenReturn(TEST_USERNAME);
        when(userRepository.findOrAddUser(eq(TEST_USERNAME), eq("valid"), isNull(String.class), anyString(), any(String[].class))).thenReturn(user);
        instance.handle(request, response, chain);
        verify(delegate).getUsername(cert);
        verify(chain).next(request, response);
    }

    private X509Certificate getCertificate(String name) {
        try {
            KeyStore ks = KeyStore.getInstance("JKS");
            InputStream ksis = X509AuthenticationHandlerTest.class.getResourceAsStream("/" + name + ".jks");
            ks.load(ksis, "password".toCharArray());
            return (X509Certificate) ks.getCertificate(name);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static interface Delegate {
        String getUsername(X509Certificate cert);

        boolean login(HttpServletRequest request);
    }

    private class TestX509AuthenticationHandler extends X509AuthenticationHandler {

        public TestX509AuthenticationHandler(UserRepository userRepository, Graph graph) {
            super(userRepository, graph);
        }

        @Override
        protected String getUsername(X509Certificate cert) {
            return delegate.getUsername(cert);
        }
    }
}
