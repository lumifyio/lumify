package io.lumify.it;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.lumify.core.ingest.FileImport;
import io.lumify.core.model.properties.LumifyProperties;
import io.lumify.core.util.GraphUtil;
import io.lumify.tikaTextExtractor.TikaTextExtractorGraphPropertyWorker;
import io.lumify.web.clientapi.LumifyApi;
import io.lumify.web.clientapi.codegen.ApiException;
import io.lumify.web.clientapi.model.*;
import io.lumify.web.clientapi.model.util.ObjectMapperFactory;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.securegraph.type.GeoPoint;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.junit.Assert.*;

public class UploadFileIntegrationTest extends TestBase {
    public static final String FILE_CONTENTS = "Joe Ferner knows David Singley.";
    private String user2Id;
    private String workspaceId;
    private String artifactVertexId;

    @Test
    public void testUploadFile() throws IOException, ApiException {
        testOntology();
        importArtifactAsUser1();
        assertUser1CanSeeInSearch();
        assertUser2DoesNotHaveAccessToUser1sWorkspace();
        grantUser2AccessToWorkspace();
        assertUser2HasAccessToWorkspace();
        assertUser3DoesNotHaveAccessToWorkspace();
        publishArtifact();
        assertUser3StillHasNoAccessToArtifactBecauseAuth1Visibility();
        assertUser3HasAccessWithAuth1Visibility();
        assertRawRoute();
        alterVisibilityOfArtifactToAuth2();
        assertUser2DoesNotHaveAccessToAuth2();
        testGeoSearch();
        testSetTitleAndCheckConfidence();
    }

    private void testOntology() throws ApiException {
        LumifyApi lumifyApi = login(USERNAME_TEST_USER_1);

        ClientApiOntology ontology = lumifyApi.getOntologyApi().get();

        boolean foundPersonConcept = false;
        for (ClientApiOntology.Concept concept : ontology.getConcepts()) {
            if (concept.getId().equals("http://lumify.io/test#person")) {
                foundPersonConcept = true;
                assertEquals("invalid title formula", "prop('http://lumify.io/test#firstName') + ' ' + prop('http://lumify.io/test#lastName')", concept.getTitleFormula());
                assertEquals("invalid sub-title formula", "prop('http://lumify.io/test#firstName') || ''", concept.getSubtitleFormula());
                assertEquals("invalid time formula", "prop('http://lumify.io/test#birthDate') || ''", concept.getTimeFormula());
            }
        }
        assertTrue("could not find http://lumify.io/test#person", foundPersonConcept);

        lumifyApi.logout();
    }

    public void importArtifactAsUser1() throws ApiException, IOException {
        LumifyApi lumifyApi = login(USERNAME_TEST_USER_1);
        addUserAuths(lumifyApi, USERNAME_TEST_USER_1, "auth1");
        addUserAuths(lumifyApi, USERNAME_TEST_USER_1, "auth2");
        workspaceId = lumifyApi.getCurrentWorkspaceId();

        ClientApiArtifactImportResponse artifact = lumifyApi.getVertexApi().importFile("auth1", "test.txt", new ByteArrayInputStream(FILE_CONTENTS.getBytes()));
        assertEquals(1, artifact.getVertexIds().size());
        artifactVertexId = artifact.getVertexIds().get(0);
        assertNotNull(artifactVertexId);

        lumifyTestCluster.processGraphPropertyQueue();

        assertArtifactCorrectWithUnresolvedTermMentions(lumifyApi, true, "auth1");

        lumifyApi.logout();
    }

    private void assertUser1CanSeeInSearch() throws ApiException {
        LumifyApi lumifyApi = login(USERNAME_TEST_USER_1);

        ClientApiVertexSearchResponse searchResults = lumifyApi.getVertexApi().vertexSearch("*");
        LOGGER.debug("searchResults: %s", searchResults.toString());
        assertEquals(1, searchResults.getVertices().size());
        ClientApiVertex searchResult = searchResults.getVertices().get(0);
        assertEquals(artifactVertexId, searchResult.getId());

        lumifyApi.logout();
    }

    public void assertUser2DoesNotHaveAccessToUser1sWorkspace() throws ApiException {
        LumifyApi lumifyApi = login(USERNAME_TEST_USER_2);
        addUserAuths(lumifyApi, USERNAME_TEST_USER_2, "auth1");
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
        lumifyApi.getWorkspaceApi().setUserAccess(user2Id, WorkspaceAccess.READ);
        lumifyApi.logout();
    }

    public void assertUser2HasAccessToWorkspace() throws ApiException {
        LumifyApi lumifyApi;
        lumifyApi = login(USERNAME_TEST_USER_2);
        lumifyApi.setWorkspaceId(workspaceId);
        ClientApiElement artifactVertex = lumifyApi.getVertexApi().getByVertexId(artifactVertexId);
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
        assertPublishAll(lumifyApi, 11);
        lumifyApi.logout();
    }

    private void assertUser3StillHasNoAccessToArtifactBecauseAuth1Visibility() throws ApiException {
        LumifyApi lumifyApi = login(USERNAME_TEST_USER_3);
        ClientApiElement vertex = lumifyApi.getVertexApi().getByVertexId(artifactVertexId);
        assertNull("should have failed", vertex);
        lumifyApi.logout();
    }

    private void assertUser3HasAccessWithAuth1Visibility() throws ApiException {
        LumifyApi lumifyApi = login(USERNAME_TEST_USER_3);
        addUserAuths(lumifyApi, USERNAME_TEST_USER_3, "auth1");
        assertArtifactCorrect(lumifyApi, false, "auth1");
        lumifyApi.logout();
    }

    public void assertArtifactCorrectWithUnresolvedTermMentions(LumifyApi lumifyApi, boolean hasWorkspaceIdInVisibilityJson, String expectedVisibilitySource) throws ApiException {
        String highlightedText = assertArtifactCorrect(lumifyApi, hasWorkspaceIdInVisibilityJson, expectedVisibilitySource);
        assertTrue("highlightedText did not contain string: " + highlightedText, highlightedText.contains("class=\"vertex\""));
        assertTrue("highlightedText did not contain string: " + highlightedText, highlightedText.contains(TestOntology.CONCEPT_PERSON));
    }

    public String assertArtifactCorrect(LumifyApi lumifyApi, boolean hasWorkspaceIdInVisibilityJson, String expectedVisibilitySource) throws ApiException {
        ClientApiElement artifactVertex = lumifyApi.getVertexApi().getByVertexId(artifactVertexId);
        assertNotNull("could not get vertex: " + artifactVertexId, artifactVertex);
        assertEquals(expectedVisibilitySource, artifactVertex.getVisibilitySource());
        assertEquals(artifactVertexId, artifactVertex.getId());
        for (ClientApiProperty property : artifactVertex.getProperties()) {
            LOGGER.info("property: %s", property.toString());
        }
        assertEquals(11, artifactVertex.getProperties().size());
        assertHasProperty(artifactVertex.getProperties(), TikaTextExtractorGraphPropertyWorker.MULTI_VALUE_KEY, LumifyProperties.CREATE_DATE.getPropertyName());
        assertHasProperty(artifactVertex.getProperties(), "", LumifyProperties.MIME_TYPE.getPropertyName(), "text/plain");
        assertHasProperty(artifactVertex.getProperties(), TikaTextExtractorGraphPropertyWorker.MULTI_VALUE_KEY, LumifyProperties.TEXT.getPropertyName());
        VisibilityJson visibilityJson = new VisibilityJson();
        visibilityJson.setSource(expectedVisibilitySource);
        if (hasWorkspaceIdInVisibilityJson) {
            visibilityJson.addWorkspace(workspaceId);
        }
        assertHasProperty(artifactVertex.getProperties(), "", LumifyProperties.VISIBILITY_JSON.getPropertyName(), visibilityJson);
        assertHasProperty(artifactVertex.getProperties(), FileImport.MULTI_VALUE_KEY, LumifyProperties.CONTENT_HASH.getPropertyName(), "urn\u001Fsha256\u001F28fca952b9eb45d43663af8e3099da0572c8232243289b5d8a03eb5ea2cb066a");
        assertHasProperty(artifactVertex.getProperties(), FileImport.MULTI_VALUE_KEY, LumifyProperties.CREATE_DATE.getPropertyName());
        assertHasProperty(artifactVertex.getProperties(), FileImport.MULTI_VALUE_KEY, LumifyProperties.FILE_NAME.getPropertyName(), "test.txt");
        assertHasProperty(artifactVertex.getProperties(), FileImport.MULTI_VALUE_KEY, LumifyProperties.FILE_NAME_EXTENSION.getPropertyName(), "txt");
        assertHasProperty(artifactVertex.getProperties(), FileImport.MULTI_VALUE_KEY, LumifyProperties.RAW.getPropertyName());
        assertHasProperty(artifactVertex.getProperties(), FileImport.MULTI_VALUE_KEY, LumifyProperties.TITLE.getPropertyName(), "test.txt");
        assertHasProperty(artifactVertex.getProperties(), "", LumifyProperties.CONCEPT_TYPE.getPropertyName(), "http://lumify.io/test#document");

        String highlightedText = lumifyApi.getVertexApi().getHighlightedText(artifactVertexId, TikaTextExtractorGraphPropertyWorker.MULTI_VALUE_KEY);
        assertNotNull(highlightedText);
        LOGGER.info("highlightedText: %s", highlightedText);

        return highlightedText;
    }


    private void assertRawRoute() throws ApiException, IOException {
        byte[] expected = FILE_CONTENTS.getBytes();

        LumifyApi lumifyApi = login(USERNAME_TEST_USER_1);

        byte[] found = IOUtils.toByteArray(lumifyApi.getVertexApi().getRaw(artifactVertexId));
        assertArrayEquals(expected, found);

        lumifyApi.logout();
    }

    private void alterVisibilityOfArtifactToAuth2() throws ApiException {
        LumifyApi lumifyApi = login(USERNAME_TEST_USER_1);

        lumifyApi.getVertexApi().setVisibility(artifactVertexId, "auth2");
        assertArtifactCorrectWithUnresolvedTermMentions(lumifyApi, false, "auth2");

        lumifyApi.logout();
    }

    private void assertUser2DoesNotHaveAccessToAuth2() throws ApiException {
        LumifyApi lumifyApi = login(USERNAME_TEST_USER_2);

        lumifyApi.getVertexApi().getByVertexId(artifactVertexId);

        lumifyApi.logout();
    }

    private void testGeoSearch() throws ApiException, JsonProcessingException {
        LumifyApi lumifyApi = login(USERNAME_TEST_USER_1);

        String geoPoint = ObjectMapperFactory.getInstance().writeValueAsString(new GeoPoint(38.8951, -77.0367));
        lumifyApi.getVertexApi().setProperty(artifactVertexId, "", TestOntology.PROPERTY_GEO_LOCATION.getPropertyName(), geoPoint, "", "justification", null, null);

        ClientApiVertexSearchResponse geoSearchResults = lumifyApi.getVertexApi().vertexGeoSearch(38.8951, -77.0367, 1000.0);
        assertEquals(1, geoSearchResults.getVertices().size());

        lumifyApi.logout();
    }

    private void testSetTitleAndCheckConfidence() throws ApiException {
        LumifyApi lumifyApi = login(USERNAME_TEST_USER_1);

        ClientApiWorkspace newWorkspace = lumifyApi.getWorkspaceApi().create();
        lumifyApi.setWorkspaceId(newWorkspace.getWorkspaceId());

        lumifyApi.getVertexApi().setProperty(artifactVertexId, "", LumifyProperties.TITLE.getPropertyName(), "New Title", "", "new title");

        ClientApiElement artifactVertex = lumifyApi.getVertexApi().getByVertexId(artifactVertexId);
        boolean foundNewTitle = false;
        for (ClientApiProperty prop : artifactVertex.getProperties()) {
            if (prop.getKey().equals("") && prop.getName().equals(LumifyProperties.TITLE.getPropertyName())) {
                foundNewTitle = true;
                LOGGER.info("new title prop: %s", prop.toString());
                assertNotNull("could not find confidence", LumifyProperties.CONFIDENCE.getMetadataValue(prop.getMetadata()));
                assertEquals(GraphUtil.SET_PROPERTY_CONFIDENCE, LumifyProperties.CONFIDENCE.getMetadataValue(prop.getMetadata()), 0.01);
            }
        }
        assertTrue("Could not find new title", foundNewTitle);

        lumifyApi.logout();
        lumifyApi.setWorkspaceId(workspaceId);
    }
}
