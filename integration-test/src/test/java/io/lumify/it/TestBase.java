package io.lumify.it;

import io.lumify.core.config.LumifyTestClusterConfigurationLoader;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import io.lumify.test.LumifyTestCluster;
import io.lumify.web.clientapi.LumifyApi;
import io.lumify.web.clientapi.UserNameOnlyLumifyApi;
import io.lumify.web.clientapi.codegen.ApiException;
import io.lumify.web.clientapi.model.ClientApiProperty;
import io.lumify.web.clientapi.model.ClientApiWorkspaceDiff;
import io.lumify.web.clientapi.model.ClientApiWorkspacePublishResponse;
import io.lumify.web.clientapi.model.ClientApiWorkspaceUndoResponse;
import io.lumify.web.clientapi.model.util.ObjectMapperFactory;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class TestBase {
    protected LumifyLogger LOGGER;
    protected LumifyTestCluster lumifyTestCluster;
    protected int httpPort;
    protected int httpsPort;
    protected static final String USERNAME_TEST_USER_1 = "testUser1";
    protected static final String USERNAME_TEST_USER_2 = "testUser2";
    protected static final String USERNAME_TEST_USER_3 = "testUser3";
    public static final String TEST_MULTI_VALUE_KEY = TestBase.class.getName();
    public static final String CONCEPT_TEST_PERSON = "http://lumify.io/test#person";

    @Before
    public void before() throws ApiException, IOException, NoSuchAlgorithmException, KeyManagementException, InterruptedException {
        LumifyTestClusterConfigurationLoader.set("repository.ontology.owl.1.iri", "http://lumify.io/test");
        LumifyTestClusterConfigurationLoader.set("repository.ontology.owl.1.dir", new File(LumifyTestCluster.getLumifyRootDir(), "integration-test/src/test/resources/io/lumify/it/").getAbsolutePath());

        disableSSLCertChecking();
        initLumifyTestCluster();
        LOGGER = LumifyLoggerFactory.getLogger(this.getClass());
    }

    public void initLumifyTestCluster() {
        httpPort = findOpenPort(10080);
        httpsPort = findOpenPort(10443);
        lumifyTestCluster = new LumifyTestCluster(httpPort, httpsPort);
        lumifyTestCluster.startup();
    }

    private int findOpenPort(int startingPort) {
        for (int port = startingPort; port < 65535; port++) {
            try {
                ServerSocket socket = new ServerSocket(port);
                socket.close();
                return port;
            } catch (IOException ex) {
                // try next port
            }
        }
        throw new RuntimeException("No free ports found");
    }

    public void disableSSLCertChecking() throws NoSuchAlgorithmException, KeyManagementException {
        HttpsURLConnection.setDefaultHostnameVerifier(new javax.net.ssl.HostnameVerifier() {
            public boolean verify(String hostname, javax.net.ssl.SSLSession sslSession) {
                if (hostname.equals("localhost")) {
                    return true;
                }
                return false;
            }
        });

        TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            public void checkClientTrusted(X509Certificate[] certs, String authType) {
            }

            public void checkServerTrusted(X509Certificate[] certs, String authType) {
            }
        }};

        SSLContext sc = SSLContext.getInstance("TLS");
        sc.init(null, trustAllCerts, new SecureRandom());
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
    }

    @After
    public void after() {
        lumifyTestCluster.shutdown();
    }

    public void addUserAuth(LumifyApi lumifyApi, String username, String auth) throws ApiException {
        Map<String, String> queryParameters = new HashMap<String, String>();
        queryParameters.put("user-name", username);
        queryParameters.put("auth", auth);
        lumifyApi.invokeAPI("/user/auth/add", "POST", queryParameters, null, null, null, null);
    }

    LumifyApi login(String username) throws ApiException {
        UserNameOnlyLumifyApi lumifyApi = new UserNameOnlyLumifyApi("https://localhost:" + httpsPort, username);
        lumifyApi.loginAndGetCurrentWorkspace();
        return lumifyApi;
    }

    protected void assertHasProperty(Iterable<ClientApiProperty> properties, String propertyKey, String propertyName) {
        ClientApiProperty property = getProperty(properties, propertyKey, propertyName);
        if (property == null) {
            fail("could not find property " + propertyKey + ":" + propertyName);
        }
    }

    protected ClientApiProperty getProperty(Iterable<ClientApiProperty> properties, String propertyKey, String propertyName) {
        for (ClientApiProperty property : properties) {
            if (propertyKey.equals(property.getKey()) && propertyName.equals(property.getName())) {
                return property;
            }
        }
        return null;
    }

    protected List<ClientApiProperty> getProperties(Iterable<ClientApiProperty> properties, String propertyKey, String propertyName) {
        List<ClientApiProperty> propertyList = new ArrayList<ClientApiProperty>();
        for (ClientApiProperty property : properties) {
            if (propertyKey.equals(property.getKey()) && propertyName.equals(property.getName())) {
                propertyList.add(property);
            }
        }
        return propertyList;
    }

    protected void assertHasProperty(Iterable<ClientApiProperty> properties, String propertyKey, String propertyName, Object expectedValue) {
        ClientApiProperty property = getProperty(properties, propertyKey, propertyName);
        if (property != null) {
            Object value = property.getValue();
            if (value instanceof Map) {
                try {
                    value = ObjectMapperFactory.getInstance().writeValueAsString(value);
                    expectedValue = ObjectMapperFactory.getInstance().writeValueAsString(expectedValue);
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
            assertEquals("property value does not match for property " + propertyKey + ":" + propertyName, expectedValue, value);
        } else {
            fail("could not find property " + propertyKey + ":" + propertyName);
        }
    }

    protected void assertPublishAll(LumifyApi lumifyApi, int expectedDiffsBeforePublish) throws ApiException {
        ClientApiWorkspaceDiff diff = lumifyApi.getWorkspaceApi().getDiff();
        LOGGER.info("diff before publish: %s", diff.toString());
        assertEquals(expectedDiffsBeforePublish, diff.getDiffs().size());
        ClientApiWorkspacePublishResponse publishAllResult = lumifyApi.getWorkspaceApi().publishAll(diff.getDiffs());
        LOGGER.info("publish all results: %s", publishAllResult.toString());
        assertTrue("publish all failed: " + publishAllResult, publishAllResult.isSuccess());
        assertEquals("publish all expected 0 failures: " + publishAllResult, 0, publishAllResult.getFailures().size());

        diff = lumifyApi.getWorkspaceApi().getDiff();
        LOGGER.info("diff after publish: %s", diff.toString());
        assertEquals(0, diff.getDiffs().size());
    }

    protected void assertUndoAll(LumifyApi lumifyApi, int expectedDiffsBeforeUndo) throws ApiException {
        ClientApiWorkspaceDiff diff = lumifyApi.getWorkspaceApi().getDiff();
        LOGGER.info("diff before undo: %s", diff.toString());
        assertEquals(expectedDiffsBeforeUndo, diff.getDiffs().size());
        ClientApiWorkspaceUndoResponse undoAllResult = lumifyApi.getWorkspaceApi().undoAll(diff.getDiffs());
        LOGGER.info("undo all results: %s", undoAllResult.toString());
        assertTrue("undo all failed: " + undoAllResult, undoAllResult.isSuccess());
        assertEquals("undo all expected 0 failures: " + undoAllResult, 0, undoAllResult.getFailures().size());

        diff = lumifyApi.getWorkspaceApi().getDiff();
        LOGGER.info("diff after undo: %s", diff.toString());
        assertEquals(0, diff.getDiffs().size());
    }

    protected static String getResourceString(String resourceName) {
        InputStream resource = TestBase.class.getResourceAsStream(resourceName);
        if (resource == null) {
            throw new RuntimeException("Could not find resource: " + resourceName);
        }
        try {
            try {
                return IOUtils.toString(resource);
            } catch (IOException e) {
                throw new RuntimeException("Could not convert resource " + resourceName + " to string");
            }
        } finally {
            try {
                resource.close();
            } catch (IOException e) {
                throw new RuntimeException("Could not close resource " + resourceName);
            }
        }
    }
}
