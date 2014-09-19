package io.lumify.it;

import io.lumify.test.LumifyTestCluster;
import io.lumify.web.clientapi.LumifyApi;
import io.lumify.web.clientapi.UserNameOnlyLumifyApi;
import io.lumify.web.clientapi.codegen.ApiException;
import io.lumify.web.clientapi.codegen.model.ArtifactImportResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(MockitoJUnitRunner.class)
public class UploadFileIntegrationTest {

    private LumifyTestCluster lumifyTestCluster;
    private int httpPort = 10000;
    private int httpsPort = 10001;
    private LumifyApi lumifyApi;
    private String username = "testUser";

    @Before
    public void before() throws ApiException, IOException, NoSuchAlgorithmException, KeyManagementException {
        disableSSLCertChecking();
        initLumifyTestCluster();
        initLumifyApi();
    }

    public void initLumifyApi() throws ApiException, IOException {
        lumifyApi = new UserNameOnlyLumifyApi("https://localhost:" + httpsPort, username);
        lumifyApi.loginAndGetCurrentWorkspace();
        lumifyApi.getAdminApi().uploadOntology(getClass().getResourceAsStream("test.owl"));
    }

    public void initLumifyTestCluster() {
        lumifyTestCluster = new LumifyTestCluster(httpPort, httpsPort);
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

    @Test
    public void testIt() throws IOException, ApiException {
        addUserAuth(username, "auth1");

        ArtifactImportResponse artifact = lumifyApi.getArtifactApi().importFile("auth1", "test.txt", new ByteArrayInputStream("Joe knows Sam".getBytes()));
        assertEquals(1, artifact.getVertexIds().size());
        String artifactVertexId = artifact.getVertexIds().get(0);
        assertNotNull(artifactVertexId);
    }

    public void addUserAuth(String username, String auth) throws ApiException {
        Map<String, String> queryParameters = new HashMap<String, String>();
        queryParameters.put("user-name", username);
        queryParameters.put("auth", auth);
        lumifyApi.invokeAPI("/user/auth/add", "POST", queryParameters, null, null, null, null);
    }
}
