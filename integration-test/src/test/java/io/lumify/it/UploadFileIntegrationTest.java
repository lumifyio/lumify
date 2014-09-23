package io.lumify.it;

import io.lumify.web.clientapi.LumifyApi;
import io.lumify.web.clientapi.codegen.ApiException;
import io.lumify.web.clientapi.codegen.model.ArtifactImportResponse;
import io.lumify.web.clientapi.codegen.model.Element;
import io.lumify.web.clientapi.codegen.model.Property;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class UploadFileIntegrationTest extends TestBase {
    @Test
    public void testIt() throws IOException, ApiException {
        LumifyApi lumifyApi = login(USERNAME_TEST_USER_1);

        addUserAuth(lumifyApi, USERNAME_TEST_USER_1, "auth1");
        String workspaceId = lumifyApi.getCurrentWorkspaceId();

        ArtifactImportResponse artifact = lumifyApi.getArtifactApi().importFile("auth1", "test.txt", new ByteArrayInputStream("Joe Ferner knows David Singley.".getBytes()));
        assertEquals(1, artifact.getVertexIds().size());
        String artifactVertexId = artifact.getVertexIds().get(0);
        assertNotNull(artifactVertexId);

        lumifyTestCluster.processGraphPropertyQueue();

        Element artifactVertex = lumifyApi.getVertexApi().getByVertexId(artifactVertexId);
        assertEquals(artifactVertexId, artifactVertex.getId());
        for (Property property : artifactVertex.getProperties()) {
            LOGGER.info("property: %s", property.toString());
        }
        assertEquals(10, artifactVertex.getProperties().size());
        assertHasProperty(artifactVertex.getProperties(), "io.lumify.tikaTextExtractor.TikaTextExtractorGraphPropertyWorker", "http://lumify.io#createDate");
        assertHasProperty(artifactVertex.getProperties(), "", "http://lumify.io#mimeType", "text/plain");
        assertHasProperty(artifactVertex.getProperties(), "io.lumify.tikaTextExtractor.TikaTextExtractorGraphPropertyWorker", "http://lumify.io#text");
        LinkedHashMap<String, Object> visibilityJson = new LinkedHashMap<String, Object>();
        visibilityJson.put("source", "auth1");
        ArrayList<String> visibilityJsonWorkspaces = new ArrayList<String>();
        visibilityJsonWorkspaces.add(workspaceId);
        visibilityJson.put("workspaces", visibilityJsonWorkspaces);
        assertHasProperty(artifactVertex.getProperties(), "io.lumify.core.ingest.FileImport", "http://lumify.io#visibilityJson", visibilityJson);
        assertHasProperty(artifactVertex.getProperties(), "io.lumify.core.ingest.FileImport", "http://lumify.io#contentHash", "urn\u001Fsha256\u001F28fca952b9eb45d43663af8e3099da0572c8232243289b5d8a03eb5ea2cb066a");
        assertHasProperty(artifactVertex.getProperties(), "io.lumify.core.ingest.FileImport", "http://lumify.io#createDate");
        assertHasProperty(artifactVertex.getProperties(), "io.lumify.core.ingest.FileImport", "http://lumify.io#fileName", "test.txt");
        assertHasProperty(artifactVertex.getProperties(), "io.lumify.core.ingest.FileImport", "http://lumify.io#fileNameExtension", "txt");
        assertHasProperty(artifactVertex.getProperties(), "io.lumify.core.ingest.FileImport", "http://lumify.io#raw");
        assertHasProperty(artifactVertex.getProperties(), "io.lumify.core.ingest.FileImport", "http://lumify.io#title", "test.txt");

        String highlightedText = lumifyApi.getArtifactApi().getHighlightedText(artifactVertexId, "io.lumify.tikaTextExtractor.TikaTextExtractorGraphPropertyWorker");
        assertNotNull(highlightedText);
        LOGGER.info("highlightedText: %s", highlightedText);
        assertTrue("highlightedText did not contain string: " + highlightedText, highlightedText.contains("class=\"entity\""));
        assertTrue("highlightedText did not contain string: " + highlightedText, highlightedText.contains("http://lumify.io/test#person"));

        lumifyApi.logout();

        lumifyApi = login(USERNAME_TEST_USER_2);
        addUserAuth(lumifyApi, USERNAME_TEST_USER_2, "auth1");
        String user2Id = lumifyApi.getCurrentUserId();
        lumifyApi.setWorkspaceId(workspaceId);
        try {
            lumifyApi.getVertexApi().getByVertexId(artifactVertexId);
            assertTrue("should have failed", false);
        } catch (ApiException ex) {
            // expected
        }
        lumifyApi.logout();

        lumifyApi = login(USERNAME_TEST_USER_1);
        lumifyApi.setWorkspaceId(workspaceId);
        lumifyApi.getWorkspaceApi().setUserAccess(user2Id, "READ");
        lumifyApi.logout();

        lumifyApi = login(USERNAME_TEST_USER_2);
        lumifyApi.setWorkspaceId(workspaceId);
        artifactVertex = lumifyApi.getVertexApi().getByVertexId(artifactVertexId);
        assertNotNull(artifactVertex);
        lumifyApi.logout();

        lumifyApi = login(USERNAME_TEST_USER_3);
        lumifyApi.setWorkspaceId(workspaceId);
        try {
            lumifyApi.getVertexApi().getByVertexId(artifactVertexId);
            assertTrue("should have failed", false);
        } catch (ApiException ex) {
            // expected
        }
        lumifyApi.logout();
    }
}
