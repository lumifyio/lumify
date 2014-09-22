package io.lumify.web.clientapi;

import io.lumify.web.clientapi.codegen.model.*;
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

        System.out.println(lumifyApi.getOntologyApi().get());
        if (lumifyApi.getOntologyApi().getConcept("http://lumify.io/dev#artifact") == null) {
            lumifyApi.getAdminApi().uploadOntology(getClass().getResourceAsStream("test.owl"));
        }

        ArtifactImportResponse artifactImportResponse = lumifyApi.getArtifactApi().importFile("", "test.txt", new ByteArrayInputStream("Joe and Sam".getBytes()));
        System.out.println("artifactImportResponse: " + artifactImportResponse);

        String artifactVertexId = artifactImportResponse.getVertexIds().get(0);

        Element artifactVertex = lumifyApi.getVertexApi().getByVertexId(artifactVertexId);
        for (Property prop : artifactVertex.getProperties()) {
            System.out.println(prop);
            if (prop.getName().equals("http://lumify.io#text")) {
                System.out.println(lumifyApi.getArtifactApi().getHighlightedText(artifactVertexId, prop.getKey()));
            }
        }

        WorkspaceUpdateData updateData = new WorkspaceUpdateData();
        WorkspaceEntityUpdate entityUpdate = new WorkspaceEntityUpdate();
        entityUpdate.setVertexId(artifactVertexId);
        GraphPosition graphPosition = new GraphPosition();
        graphPosition.setX(1);
        graphPosition.setY(1);
        entityUpdate.setGraphPosition(graphPosition);
        updateData.getEntityUpdates().add(entityUpdate);
        lumifyApi.getWorkspaceApi().update(updateData);

        WorkspaceDiff diff = lumifyApi.getWorkspaceApi().getDiff();
        for (WorkspaceDiffItem diffItem : diff.getDiffs()) {
            System.out.println(diffItem);
        }

        PublishResponse publishResponse = lumifyApi.getWorkspaceApi().publishAll(diff.getDiffs());
        System.out.println(publishResponse);

        Workspace workspace = lumifyApi.getWorkspaceApi().getById(lumifyApi.getCurrentWorkspace().getWorkspaceId());
        System.out.println(workspace);

        lumifyApi.getEntityApi().resolveTerm(
                artifactImportResponse.getVertexIds().get(0),
                "io.lumify.tikaTextExtractor.TikaTextExtractorGraphPropertyWorker",
                0, 3, "Joe", "http://lumify.io/dev#person", ""
        );

        GraphVertexSearchResult searchResults = lumifyApi.getGraphApi().vertexSearch("*");
        System.out.println(searchResults);
        System.out.println(searchResults.getVertices().get(0).getProperties().get(0).get("key"));

        lumifyApi.logout();
    }
}