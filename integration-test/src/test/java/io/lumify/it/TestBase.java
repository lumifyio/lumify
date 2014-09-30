package io.lumify.it;

import io.lumify.core.config.LumifyTestClusterConfigurationLoader;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import io.lumify.test.LumifyTestCluster;
import io.lumify.web.clientapi.LumifyApi;
import io.lumify.web.clientapi.UserNameOnlyLumifyApi;
import io.lumify.web.clientapi.codegen.ApiException;
import io.lumify.web.clientapi.codegen.model.Property;
import io.lumify.web.clientapi.codegen.model.PublishResponse;
import io.lumify.web.clientapi.codegen.model.WorkspaceDiff;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

import static com.mongodb.util.MyAsserts.assertTrue;
import static org.junit.Assert.assertEquals;

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

    protected void assertHasProperty(Iterable<Property> properties, String propertyKey, String propertyName) {
        Property property = getProperty(properties, propertyKey, propertyName);
        if (property == null) {
            assertTrue(false, "could not find property " + propertyKey + ":" + propertyName);
        }
    }

    private Property getProperty(Iterable<Property> properties, String propertyKey, String propertyName) {
        for (Property property : properties) {
            if (propertyKey.equals(property.getKey()) && propertyName.equals(property.getName())) {
                return property;
            }
        }
        return null;
    }

    protected void assertHasProperty(Iterable<Property> properties, String propertyKey, String propertyName, Object expectedValue) {
        Property property = getProperty(properties, propertyKey, propertyName);
        if (property != null) {
            assertEquals("property value does not match for property " + propertyKey + ":" + propertyName, expectedValue, property.getValue());
        } else {
            assertTrue(false, "could not find property " + propertyKey + ":" + propertyName);
        }
    }

    protected void assertPublishAll(LumifyApi lumifyApi, int expectedDiffsBeforePublish) throws ApiException {
        WorkspaceDiff diff = lumifyApi.getWorkspaceApi().getDiff();
        LOGGER.info("diff before publish: %s", diff.toString());
        assertEquals(expectedDiffsBeforePublish, diff.getDiffs().size());
        PublishResponse publishAllResult = lumifyApi.getWorkspaceApi().publishAll(diff.getDiffs());
        LOGGER.info("publish all results: %s", publishAllResult.toString());
        Assert.assertTrue("publish all failed: " + publishAllResult, publishAllResult.getSuccess());
        assertEquals("publish all expected 0 failures: " + publishAllResult, 0, publishAllResult.getFailures().size());

        diff = lumifyApi.getWorkspaceApi().getDiff();
        LOGGER.info("diff after publish: %s", diff.toString());
        assertEquals(0, diff.getDiffs().size());
    }
}
