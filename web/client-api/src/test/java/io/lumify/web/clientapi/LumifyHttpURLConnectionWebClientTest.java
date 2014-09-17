package io.lumify.web.clientapi;

import io.lumify.web.clientapi.model.*;
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
import java.util.ArrayList;
import java.util.List;

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
    public void testIt() {
        LumifyHttpURLConnectionWebClient client = new UserNameOnlyAuthLumifyHttpURLConnectionWebClient("https://localhost:8889/", "testUser");

        UserMeResponse userMeResponse = client.userMe();
        System.out.println("userMeResponse: " + userMeResponse);

        WorkspacesResponse workspacesResponse = client.workspaces();
        System.out.println("workspacesResponse: " + workspacesResponse);
        Workspace currentWorkspace;
        Workspace[] workspaces = workspacesResponse.getWorkspaces();
        if (workspaces.length == 0) {
            WorkspaceNewResponse workspaceNewResponse = client.workspaceNew();
            System.out.println("workspaceNewResponse: " + workspaceNewResponse);
            currentWorkspace = workspaceNewResponse.getWorkspace();
        } else {
            currentWorkspace = null;
            for (Workspace w : workspaces) {
                if (userMeResponse.getCurrentWorkspaceId() == null) {
                    currentWorkspace = w;
                }
                if (w.getId().equals(userMeResponse.getCurrentWorkspaceId())) {
                    currentWorkspace = w;
                    break;
                }
            }
        }
        client.setCurrentWorkspaceId(currentWorkspace.getId());
        System.out.println("currentWorkspace: " + currentWorkspace);

        ArtifactImportResponse artifactImportResponse = client.artifactImport("", "test.txt", new ByteArrayInputStream("Joe and Sam".getBytes()));
        System.out.println("artifactImportResponse: " + artifactImportResponse);

        List<WorkspaceUpdateItem> workspaceUpdateItems = new ArrayList<WorkspaceUpdateItem>();
        for (String vertexId : artifactImportResponse.getVertexIds()) {
            workspaceUpdateItems.add(new VertexWorkspaceUpdateItem(vertexId, new GraphPosition(0, 0)));
        }
        client.workspaceUpdate(workspaceUpdateItems);

        WorkspaceDiffResponse workspaceDiffResponse = client.workspaceDiff();
        System.out.println("workspaceDiffResponse: " + workspaceDiffResponse);

        try {
            WorkspacePublishResponse workspacePublishResponse = client.workspacePublishAll(workspaceDiffResponse.getDiffs());
            System.out.println("workspacePublishResponse: " + workspacePublishResponse);
        } catch (LumifyClientApiPublishException ex) {
            System.out.println("workspacePublish failed: " + ex.getResponse());
            return;
        }

        WorkspaceVerticesResponse workspaceVerticesResponse = client.workspaceVertices();
        System.out.println("workspaceVerticesResponse: " + workspaceVerticesResponse);
        for (WorkspaceVertex workspaceVertex : workspaceVerticesResponse.getVertices()) {
            System.out.println("workspaceVertex: " + workspaceVertex);
        }
    }
}