package io.lumify.it;

import io.lumify.core.ingest.FileImport;
import io.lumify.core.model.properties.LumifyProperties;
import io.lumify.core.security.LumifyVisibilityProperties;
import io.lumify.tikaTextExtractor.TikaTextExtractorGraphPropertyWorker;
import io.lumify.web.clientapi.LumifyApi;
import io.lumify.web.clientapi.codegen.ApiException;
import io.lumify.web.clientapi.codegen.model.*;
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
    private String user2Id;
    private String workspaceId;
    private String artifactVertexId;

    @Test
    public void testUploadFile() throws IOException, ApiException {
        importArtifactAsUser1();
        assertUser2DoesNotHaveAccessToUser1sWorkspace();
        grantUser2AccessToWorkspace();
        assertUser2HasAccessToWorkspace();
        assertUser3DoesNotHaveAccessToWorkspace();
        publishArtifact();
        assertUser3StillHasNoAccessToArtifactBecauseAuth1Visibility();
        assertUser3HasAccessWithAuth1Visibility();
    }

    public void importArtifactAsUser1() throws ApiException, IOException {
        LumifyApi lumifyApi = login(USERNAME_TEST_USER_1);
        addUserAuth(lumifyApi, USERNAME_TEST_USER_1, "auth1");
        workspaceId = lumifyApi.getCurrentWorkspaceId();

        ArtifactImportResponse artifact = lumifyApi.getArtifactApi().importFile("auth1", "test.txt", new ByteArrayInputStream("Joe Ferner knows David Singley.".getBytes()));
        assertEquals(1, artifact.getVertexIds().size());
        artifactVertexId = artifact.getVertexIds().get(0);
        assertNotNull(artifactVertexId);

        lumifyTestCluster.processGraphPropertyQueue();

        assertArtifactCorrect(lumifyApi, true);

        lumifyApi.logout();
    }

    public void assertUser2DoesNotHaveAccessToUser1sWorkspace() throws ApiException {
        LumifyApi lumifyApi = login(USERNAME_TEST_USER_2);
        addUserAuth(lumifyApi, USERNAME_TEST_USER_2, "auth1");
        user2Id = lumifyApi.getCurrentUserId();

        lumifyApi.setWorkspaceId(workspaceId);
        try {
            lumifyApi.getVertexApi().getByVertexId(artifactVertexId);
            assertTrue("should have failed", false);
        } catch (ApiException ex) {
            // expected
        }

        lumifyApi.logout();
    }

    public void grantUser2AccessToWorkspace() throws ApiException {
        LumifyApi lumifyApi;
        lumifyApi = login(USERNAME_TEST_USER_1);
        lumifyApi.setWorkspaceId(workspaceId);
        lumifyApi.getWorkspaceApi().setUserAccess(user2Id, "READ");
        lumifyApi.logout();
    }

    public void assertUser2HasAccessToWorkspace() throws ApiException {
        LumifyApi lumifyApi;
        lumifyApi = login(USERNAME_TEST_USER_2);
        lumifyApi.setWorkspaceId(workspaceId);
        Element artifactVertex = lumifyApi.getVertexApi().getByVertexId(artifactVertexId);
        assertNotNull(artifactVertex);
        lumifyApi.logout();
    }

    public void assertUser3DoesNotHaveAccessToWorkspace() throws ApiException {
        LumifyApi lumifyApi = login(USERNAME_TEST_USER_3);
        lumifyApi.setWorkspaceId(workspaceId);
        try {
            lumifyApi.getVertexApi().getByVertexId(artifactVertexId);
            assertTrue("should have failed", false);
        } catch (ApiException ex) {
            // expected
        }
        lumifyApi.logout();
    }

    private void publishArtifact() throws ApiException {
        LumifyApi lumifyApi = login(USERNAME_TEST_USER_1);

        WorkspaceDiff diff = lumifyApi.getWorkspaceApi().getDiff();
        PublishResponse publishResults = lumifyApi.getWorkspaceApi().publishAll(diff.getDiffs());
        assertEquals(0, publishResults.getFailures().size());
        assertTrue(publishResults.getSuccess());

        lumifyApi.logout();
    }

    private void assertUser3StillHasNoAccessToArtifactBecauseAuth1Visibility() throws ApiException {
        LumifyApi lumifyApi = login(USERNAME_TEST_USER_3);
        Element vertex = lumifyApi.getVertexApi().getByVertexId(artifactVertexId);
        assertNull("should have failed", vertex);
        lumifyApi.logout();
    }

    private void assertUser3HasAccessWithAuth1Visibility() throws ApiException {
        LumifyApi lumifyApi = login(USERNAME_TEST_USER_3);
        addUserAuth(lumifyApi, USERNAME_TEST_USER_3, "auth1");
        assertArtifactCorrect(lumifyApi, false);
        lumifyApi.logout();
    }

    public void assertArtifactCorrect(LumifyApi lumifyApi, boolean hasWorkspaceIdInVisibilityJson) throws ApiException {
        Element artifactVertex = lumifyApi.getVertexApi().getByVertexId(artifactVertexId);
        assertNotNull("could not get vertex: " + artifactVertexId, artifactVertex);
        assertEquals(artifactVertexId, artifactVertex.getId());
        for (Property property : artifactVertex.getProperties()) {
            LOGGER.info("property: %s", property.toString());
        }
        assertEquals(10, artifactVertex.getProperties().size());
        assertHasProperty(artifactVertex.getProperties(), TikaTextExtractorGraphPropertyWorker.MULTI_VALUE_KEY, LumifyProperties.CREATE_DATE.getPropertyName());
        assertHasProperty(artifactVertex.getProperties(), "", LumifyProperties.MIME_TYPE.getPropertyName(), "text/plain");
        assertHasProperty(artifactVertex.getProperties(), TikaTextExtractorGraphPropertyWorker.MULTI_VALUE_KEY, LumifyProperties.TEXT.getPropertyName());
        LinkedHashMap<String, Object> visibilityJson = new LinkedHashMap<String, Object>();
        visibilityJson.put("source", "auth1");
        ArrayList<String> visibilityJsonWorkspaces = new ArrayList<String>();
        if (hasWorkspaceIdInVisibilityJson) {
            visibilityJsonWorkspaces.add(workspaceId);
        }
        visibilityJson.put("workspaces", visibilityJsonWorkspaces);
        assertHasProperty(artifactVertex.getProperties(), "", LumifyVisibilityProperties.VISIBILITY_JSON_PROPERTY.getPropertyName(), visibilityJson);
        assertHasProperty(artifactVertex.getProperties(), FileImport.MULTI_VALUE_KEY, LumifyProperties.CONTENT_HASH.getPropertyName(), "urn\u001Fsha256\u001F28fca952b9eb45d43663af8e3099da0572c8232243289b5d8a03eb5ea2cb066a");
        assertHasProperty(artifactVertex.getProperties(), FileImport.MULTI_VALUE_KEY, LumifyProperties.CREATE_DATE.getPropertyName());
        assertHasProperty(artifactVertex.getProperties(), FileImport.MULTI_VALUE_KEY, LumifyProperties.FILE_NAME.getPropertyName(), "test.txt");
        assertHasProperty(artifactVertex.getProperties(), FileImport.MULTI_VALUE_KEY, LumifyProperties.FILE_NAME_EXTENSION.getPropertyName(), "txt");
        assertHasProperty(artifactVertex.getProperties(), FileImport.MULTI_VALUE_KEY, LumifyProperties.RAW.getPropertyName());
        assertHasProperty(artifactVertex.getProperties(), FileImport.MULTI_VALUE_KEY, LumifyProperties.TITLE.getPropertyName(), "test.txt");

        String highlightedText = lumifyApi.getArtifactApi().getHighlightedText(artifactVertexId, TikaTextExtractorGraphPropertyWorker.MULTI_VALUE_KEY);
        assertNotNull(highlightedText);
        LOGGER.info("highlightedText: %s", highlightedText);
        assertTrue("highlightedText did not contain string: " + highlightedText, highlightedText.contains("class=\"entity\""));
        assertTrue("highlightedText did not contain string: " + highlightedText, highlightedText.contains(CONCEPT_TEST_PERSON));
    }
}
