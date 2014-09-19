package io.lumify.web.clientapi;

import io.lumify.web.clientapi.model.ArtifactImportResponse;
import org.junit.Before;
import org.junit.Test;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.ByteArrayInputStream;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

public class LumifyHttpURLConnectionWebClientTest {
    @Before
    public void before() throws NoSuchAlgorithmException, KeyManagementException {
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

    @Test
    public void testIt() throws Exception {
        LumifyApi lumifyApi = new UserNameOnlyLumifyApi("https://localhost:8889", "testUser");

        lumifyApi.loginAndGetCurrentWorkspace();

        lumifyApi.getAdminApi().uploadOntology(getClass().getResourceAsStream("test.owl"));

        ArtifactImportResponse artifactImportResponse = lumifyApi.getArtifactApi().importFile("", "test.txt", new ByteArrayInputStream("Joe and Sam".getBytes()));
        System.out.println("artifactImportResponse: " + artifactImportResponse);

        System.out.println(lumifyApi.getArtifactApi().getHighlightedText("a", "b"));

//
//        List<WorkspaceUpdateItem> workspaceUpdateItems = new ArrayList<WorkspaceUpdateItem>();
//        for (String vertexId : artifactImportResponse.getVertexIds()) {
//            workspaceUpdateItems.add(new VertexWorkspaceUpdateItem(vertexId, new GraphPosition(0, 0)));
//        }
//        client.workspaceUpdate(workspaceUpdateItems);
//
//        WorkspaceDiffResponse workspaceDiffResponse = client.workspaceDiff();
//        System.out.println("workspaceDiffResponse: " + workspaceDiffResponse);
//
//        try {
//            WorkspacePublishResponse workspacePublishResponse = client.workspacePublishAll(workspaceDiffResponse.getDiffs());
//            System.out.println("workspacePublishResponse: " + workspacePublishResponse);
//        } catch (LumifyClientApiPublishException ex) {
//            System.out.println("workspacePublish failed: " + ex.getResponse());
//            return;
//        }
//
//        WorkspaceVerticesResponse workspaceVerticesResponse = client.workspaceVertices();
//        System.out.println("workspaceVerticesResponse: " + workspaceVerticesResponse);
//        for (WorkspaceVertex workspaceVertex : workspaceVerticesResponse.getVertices()) {
//            System.out.println("  workspaceVertex: " + workspaceVertex);
//            for (WorkspaceVertexProperty property : workspaceVertex.getProperties()) {
//                String propertyName = property.getName();
//                String propertyKey = property.getKey();
//                Object propertyValue = property.getValue();
//                System.out.println("    property: " + propertyName + " " + propertyKey + " " + propertyValue);
//            }
//        }
//
////        String highlightedText = client.artifactHighlightedText(workspaceVerticesResponse.getVertices()[0].getId(), "io.lumify.tikaTextExtractor.TikaTextExtractorGraphPropertyWorker");
////        System.out.println("highlightedText\n" + highlightedText + "\n\n");
////
////        client.entityResolveTerm(
////                artifactImportResponse.getVertexIds()[0],
////                "io.lumify.tikaTextExtractor.TikaTextExtractorGraphPropertyWorker",
////                0, 3, "Joe", "http://lumify.io/dev#person", ""
////        );
////
////        highlightedText = client.artifactHighlightedText(workspaceVerticesResponse.getVertices()[0].getId(), "io.lumify.tikaTextExtractor.TikaTextExtractorGraphPropertyWorker");
////        System.out.println("highlightedText\n" + highlightedText + "\n\n");
////
////        client.workspacePublishAll();
//
//        GraphVertexSearchResponse graphVertexSearchResponse = client.graphVertexSearch("*");
//        System.out.println("graphVertexSearchResponse: " + graphVertexSearchResponse);
//        for (GraphVertexSearchVertex vertex : graphVertexSearchResponse.getVertices()) {
//            System.out.println("  vertex: " + vertex);
//        }
//
//        client.logOut();
    }
}