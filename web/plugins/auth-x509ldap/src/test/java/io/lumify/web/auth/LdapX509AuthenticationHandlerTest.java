package io.lumify.web.auth;

import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import io.lumify.core.config.Configuration;
import io.lumify.core.config.HashMapConfigurationLoader;
import io.lumify.core.exception.LumifyException;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.user.User;
import io.lumify.ldap.LdapSearchConfiguration;
import io.lumify.ldap.LdapSearchService;
import io.lumify.ldap.LdapSearchServiceImpl;
import io.lumify.ldap.LdapSearchServiceTest;
import io.lumify.miniweb.HandlerChain;
import io.lumify.web.AuthenticationHandler;
import io.lumify.web.X509AuthenticationHandler;
import org.apache.commons.lang.StringUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.securegraph.Graph;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Matchers.isNull;
import static org.mockito.Matchers.notNull;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class LdapX509AuthenticationHandlerTest {
    private static InMemoryDirectoryServer ldapServer;

    @Mock
    private UserRepository userRepository;
    @Mock
    private User user;
    @Mock
    private Graph graph;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private HttpSession httpSession;
    @Mock
    private HandlerChain chain;

    @BeforeClass
    public static void setUp() throws Exception {
        ldapServer = LdapSearchServiceTest.configureInMemoryDirectoryServer();
        ldapServer.startListening();
    }

    @AfterClass
    public static void tearDown() {
        ldapServer.shutDown(true);
    }

    @Test
    public void testUserWithRequiredRole() throws Exception {
        LdapSearchService ldapSearchService = new LdapSearchServiceImpl(LdapSearchServiceTest.getServerConfig(ldapServer), getSearchConfigWithExtraUserAttribute("role"));
        Configuration configuration = getConfigurationWithRequiredAttribute("role", "lumify_administrator");
        AuthenticationHandler authenticationHandler = new LdapX509AuthenticationHandler(userRepository, graph, ldapSearchService, configuration);

        X509Certificate cert = LdapSearchServiceTest.getPersonCertificate("alice");
        when(request.getAttribute(X509AuthenticationHandler.CERTIFICATE_REQUEST_ATTRIBUTE)).thenReturn(new X509Certificate[]{cert});
        when(userRepository.findOrAddUser((String)notNull(), (String)notNull(), (String)isNull(), (String)notNull(), (String[])notNull())).thenReturn(user);
        when(user.toString()).thenReturn("alice");
        when(user.getUserId()).thenReturn("USER_alice");
        when(request.getSession()).thenReturn(httpSession);

        authenticationHandler.handle(request, response, chain);

        verify(chain).next(request, response);
    }

    @Test
    public void testUserWithoutRequiredRole() throws Exception {
        LdapSearchService ldapSearchService = new LdapSearchServiceImpl(LdapSearchServiceTest.getServerConfig(ldapServer), getSearchConfigWithExtraUserAttribute("role"));
        Configuration configuration = getConfigurationWithRequiredAttribute("role", "lumify_administrator");
        AuthenticationHandler authenticationHandler = new LdapX509AuthenticationHandler(userRepository, graph, ldapSearchService, configuration);

        X509Certificate cert = LdapSearchServiceTest.getPersonCertificate("bob");
        when(request.getAttribute(X509AuthenticationHandler.CERTIFICATE_REQUEST_ATTRIBUTE)).thenReturn(new X509Certificate[]{cert});

        authenticationHandler.handle(request, response, chain);

        verify(response).sendError(HttpServletResponse.SC_FORBIDDEN);
        verify(chain, never()).next(request, response);
    }

    @Test
    public void testUserWithoutAnyRoles() throws Exception {
        LdapSearchService ldapSearchService = new LdapSearchServiceImpl(LdapSearchServiceTest.getServerConfig(ldapServer), getSearchConfigWithExtraUserAttribute("role"));
        Configuration configuration = getConfigurationWithRequiredAttribute("role", "lumify_administrator");
        AuthenticationHandler authenticationHandler = new LdapX509AuthenticationHandler(userRepository, graph, ldapSearchService, configuration);

        X509Certificate cert = LdapSearchServiceTest.getPersonCertificate("carlos");
        when(request.getAttribute(X509AuthenticationHandler.CERTIFICATE_REQUEST_ATTRIBUTE)).thenReturn(new X509Certificate[]{cert});

        authenticationHandler.handle(request, response, chain);

        verify(response).sendError(HttpServletResponse.SC_FORBIDDEN);
        verify(chain, never()).next(request, response);
    }

    @Test
    public void testUserWithRequiredGroup() throws Exception {
        LdapSearchService ldapSearchService = new LdapSearchServiceImpl(LdapSearchServiceTest.getServerConfig(ldapServer), LdapSearchServiceTest.getSearchConfig());
        Configuration configuration = getConfigurationWithRequiredGroups("managers");
        AuthenticationHandler authenticationHandler = new LdapX509AuthenticationHandler(userRepository, graph, ldapSearchService, configuration);

        X509Certificate cert = LdapSearchServiceTest.getPersonCertificate("carlos");
        when(request.getAttribute(X509AuthenticationHandler.CERTIFICATE_REQUEST_ATTRIBUTE)).thenReturn(new X509Certificate[]{cert});
        when(userRepository.findOrAddUser((String)notNull(), (String)notNull(), (String)isNull(), (String)notNull(), (String[])notNull())).thenReturn(user);
        when(user.toString()).thenReturn("carlos");
        when(user.getUserId()).thenReturn("USER_carlos");
        when(request.getSession()).thenReturn(httpSession);

        authenticationHandler.handle(request, response, chain);

        verify(chain).next(request, response);
    }

    @Test
    public void testUserWithoutRequiredGroup() throws Exception {
        LdapSearchService ldapSearchService = new LdapSearchServiceImpl(LdapSearchServiceTest.getServerConfig(ldapServer), LdapSearchServiceTest.getSearchConfig());
        Configuration configuration = getConfigurationWithRequiredGroups("managers");
        AuthenticationHandler authenticationHandler = new LdapX509AuthenticationHandler(userRepository, graph, ldapSearchService, configuration);

        X509Certificate cert = LdapSearchServiceTest.getPersonCertificate("bob");
        when(request.getAttribute(X509AuthenticationHandler.CERTIFICATE_REQUEST_ATTRIBUTE)).thenReturn(new X509Certificate[]{cert});

        authenticationHandler.handle(request, response, chain);

        verify(response).sendError(HttpServletResponse.SC_FORBIDDEN);
        verify(chain, never()).next(request, response);
    }

    // TODO: user without any groups?

    @Test(expected = LumifyException.class)
    public void testUserWithoutLdapEntry() throws Exception {
        LdapSearchService ldapSearchService = new LdapSearchServiceImpl(LdapSearchServiceTest.getServerConfig(ldapServer), LdapSearchServiceTest.getSearchConfig());
        Map<String,String> map = new HashMap<String, String>();
        Configuration configuration = new HashMapConfigurationLoader(map).createConfiguration();
        AuthenticationHandler authenticationHandler = new LdapX509AuthenticationHandler(userRepository, graph, ldapSearchService, configuration);

        X509Certificate cert = LdapSearchServiceTest.getPersonCertificate("diane");
        when(request.getAttribute(X509AuthenticationHandler.CERTIFICATE_REQUEST_ATTRIBUTE)).thenReturn(new X509Certificate[]{cert});

        authenticationHandler.handle(request, response, chain);
    }

    private LdapSearchConfiguration getSearchConfigWithExtraUserAttribute(String extraAttribute) {
        LdapSearchConfiguration searchConfiguration = LdapSearchServiceTest.getSearchConfig();
        List<String> userAttributes = new ArrayList<String>(searchConfiguration.getUserAttributes());
        userAttributes.add(extraAttribute);
        searchConfiguration.setUserAttributes(StringUtils.join(userAttributes, ","));
        return searchConfiguration;
    }

    private Configuration getConfigurationWithRequiredAttribute(String attribute, String values) {
        Map<String,String> map = new HashMap<String, String>();
        map.put("ldap.x509Authentication.requiredAttribute", attribute);
        map.put("ldap.x509Authentication.requiredAttributeValues", values);
        return new HashMapConfigurationLoader(map).createConfiguration();
    }

    private Configuration getConfigurationWithRequiredGroups(String groups) {
        Map<String,String> map = new HashMap<String, String>();
        map.put("ldap.x509Authentication.requiredGroups", groups);
        return new HashMapConfigurationLoader(map).createConfiguration();
    }
}
