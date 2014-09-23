package io.lumify.it;

import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import io.lumify.test.LumifyTestCluster;
import io.lumify.web.clientapi.LumifyApi;
import io.lumify.web.clientapi.UserNameOnlyLumifyApi;
import io.lumify.web.clientapi.codegen.ApiException;
import org.junit.After;
import org.junit.Before;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

public class TestBase {
    protected LumifyLogger LOGGER;
    protected LumifyTestCluster lumifyTestCluster;
    protected static final int HTTP_PORT = 10000;
    protected static final int HTTPS_PORT = 10001;
    protected static final String USERNAME_TEST_USER_1 = "testUser1";
    protected static final String USERNAME_TEST_USER_2 = "testUser2";

    @Before
    public void before() throws ApiException, IOException, NoSuchAlgorithmException, KeyManagementException {
        disableSSLCertChecking();
        initLumifyTestCluster();
        LOGGER = LumifyLoggerFactory.getLogger(this.getClass());
    }

    public void initLumifyTestCluster() {
        lumifyTestCluster = new LumifyTestCluster(HTTP_PORT, HTTPS_PORT);
        lumifyTestCluster.startup();
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
        UserNameOnlyLumifyApi lumifyApi = new UserNameOnlyLumifyApi("https://localhost:" + HTTPS_PORT, username);
        lumifyApi.loginAndGetCurrentWorkspace();
        return lumifyApi;
    }
}
