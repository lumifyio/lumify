package io.lumify.it;

import io.lumify.core.util.ClientApiConverter;
import io.lumify.web.clientapi.LumifyApi;
import io.lumify.web.clientapi.codegen.ApiException;
import io.lumify.web.clientapi.model.*;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class UploadRdfIntegrationTest extends TestBase {
    private static final String FILE_CONTENTS = getResourceString("sample.rdf");
    private String artifactVertexId;
    private String joeFernerVertexId;
    private String daveSingleyVertexId;
    private String altamiraCorporationVertexId;

    @Test
    public void testUploadRdf() throws IOException, ApiException {
        uploadAndProcessRdf();
        assertUser2CanSeeRdfVertices();
        assertGetEdgesWithVisibilities();
    }

    public void uploadAndProcessRdf() throws ApiException, IOException {
        LumifyApi lumifyApi = login(USERNAME_TEST_USER_1);
        addUserAuths(lumifyApi, USERNAME_TEST_USER_1, "auth1");
        addUserAuths(lumifyApi, USERNAME_TEST_USER_1, "auth2");

        ClientApiArtifactImportResponse artifact = lumifyApi.getVertexApi().importFile("auth1", "sample.rdf", new ByteArrayInputStream(FILE_CONTENTS.getBytes()));
        artifactVertexId = artifact.getVertexIds().get(0);

        lumifyTestCluster.processGraphPropertyQueue();

        assertPublishAll(lumifyApi, 25);

        ClientApiVertexSearchResponse searchResults = lumifyApi.getVertexApi().vertexSearch("*");
        LOGGER.info("searchResults (user1): %s", searchResults);
        assertEquals(4, searchResults.getVertices().size());
        for (ClientApiVertex v : searchResults.getVertices()) {
            assertEquals("auth1", v.getVisibilitySource());

            if (v.getId().equals("PERSON_Joe_Ferner")) {
                joeFernerVertexId = v.getId();
            }
            if (v.getId().equals("PERSON_Dave_Singley")) {
                daveSingleyVertexId = v.getId();
            }
            if (v.getId().equals("COMPANY_Altamira_Corporation")) {
                altamiraCorporationVertexId = v.getId();
            }
        }
        assertNotNull(joeFernerVertexId, "Could not find joe ferner");
        assertNotNull(daveSingleyVertexId, "Could not find dave singley");

        lumifyApi.logout();
    }

    private void assertUser2CanSeeRdfVertices() throws ApiException {
        LumifyApi lumifyApi = login(USERNAME_TEST_USER_2);
        addUserAuths(lumifyApi, USERNAME_TEST_USER_2, "auth1");

        assertSearch(lumifyApi);
        assertGetEdges(lumifyApi);

        // TODO/BUG The following is commented out until long-running processes are supported in integration tests.
        //          Leaving this in causes the test to hang indefinitely because the long-running process queue is
        //          not being read.
        // assertFindPath(lumifyApi);

        assertFindRelated(lumifyApi);
        assertFindMultiple(lumifyApi);
        assertWorkspace(lumifyApi);

        lumifyApi.logout();
    }

    private void assertFindMultiple(LumifyApi lumifyApi) throws ApiException {
        List<String> graphVertexIds = new ArrayList<>();
        graphVertexIds.add(artifactVertexId);
        graphVertexIds.add(joeFernerVertexId);
        graphVertexIds.add(daveSingleyVertexId);
        graphVertexIds.add(altamiraCorporationVertexId);
        ClientApiVertexMultipleResponse vertices = lumifyApi.getVertexApi().findMultiple(graphVertexIds, true);
        LOGGER.info("vertices: %s", vertices.toString());
        assertEquals(4, vertices.getVertices().size());
        assertFalse("isRequiredFallback", vertices.isRequiredFallback());
        boolean foundAltamiraCorporation = false;
        boolean foundArtifact = false;
        boolean foundDaveSingley = false;
        boolean foundJoeFerner = false;
        for (ClientApiVertex v : vertices.getVertices()) {
            if (v.getId().equals(altamiraCorporationVertexId)) {
                foundAltamiraCorporation = true;
            }
            if (v.getId().equals(artifactVertexId)) {
                foundArtifact = true;
            }
            if (v.getId().equals(joeFernerVertexId)) {
                foundDaveSingley = true;
            }
            if (v.getId().equals(daveSingleyVertexId)) {
                foundJoeFerner = true;
            }
        }
        assertTrue("could not find AltamiraCorporation in multiple", foundAltamiraCorporation);
        assertTrue("could not find Artifact in multiple", foundArtifact);
        assertTrue("could not find DaveSingley in multiple", foundDaveSingley);
        assertTrue("could not find JoeFerner in multiple", foundJoeFerner);
    }

    private void assertFindRelated(LumifyApi lumifyApi) throws ApiException {
        ClientApiVertexFindRelatedResponse related = lumifyApi.getVertexApi().findRelated(joeFernerVertexId);
        assertEquals(2, related.getCount());
        assertEquals(2, related.getVertices().size());

        boolean foundAltamiraCorporation = false;
        boolean foundRdfDocument = false;
        for (ClientApiVertex v : related.getVertices()) {
            if (v.getId().equals(altamiraCorporationVertexId)) {
                foundAltamiraCorporation = true;
            }
            if (v.getId().equals(artifactVertexId)) {
                foundRdfDocument = true;
            }
        }
        assertTrue("could not find AltamiraCorporation in related", foundAltamiraCorporation);
        assertTrue("could not find rdf in related", foundRdfDocument);
    }

    private void assertSearch(LumifyApi lumifyApi) throws ApiException {
        ClientApiVertexSearchResponse searchResults = lumifyApi.getVertexApi().vertexSearch("*");
        LOGGER.info("searchResults (user2): %s", searchResults);
        assertEquals(4, searchResults.getVertices().size());
    }

    private void assertGetEdges(LumifyApi lumifyApi) throws ApiException {
        ClientApiVertexEdges artifactEdges = lumifyApi.getVertexApi().getEdges(artifactVertexId, null, null, null);
        assertEquals(3, artifactEdges.getTotalReferences());
        assertEquals(3, artifactEdges.getRelationships().size());

        for (ClientApiVertexEdges.Edge e : artifactEdges.getRelationships()) {
            String edgeId = e.getRelationship().getId();
            ClientApiEdgeWithVertexData edge = lumifyApi.getEdgeApi().getByEdgeId(edgeId);
            LOGGER.info("edge: %s", edge.toString());
        }
    }

    private void assertFindPath(LumifyApi lumifyApi) throws ApiException {
        ClientApiLongRunningProcessSubmitResponse longRunningProcessResponse = lumifyApi.getVertexApi().findPath(joeFernerVertexId, daveSingleyVertexId, 2);
        ClientApiLongRunningProcess longRunningProcess = lumifyApi.getLongRunningProcessApi().waitById(longRunningProcessResponse.getId());
        String resultsString = longRunningProcess.getResultsString();
        assertNotNull("resultsString was null", resultsString);
        ClientApiVertexFindPathResponse paths = ClientApiConverter.toClientApi(resultsString, ClientApiVertexFindPathResponse.class);

        LOGGER.info("paths: %s", paths.toString());
        assertEquals(2, paths.getPaths().size());
        boolean foundAltamiraCorporation = false;
        boolean foundRdfDocument = false;
        for (List<ClientApiVertex> path : paths.getPaths()) {
            assertEquals(3, path.size());
            if (path.get(1).getId().equals(altamiraCorporationVertexId)) {
                foundAltamiraCorporation = true;
            }
            if (path.get(1).getId().equals(artifactVertexId)) {
                foundRdfDocument = true;
            }
        }
        assertTrue("could not find AltamiraCorporation in path", foundAltamiraCorporation);
        assertTrue("could not find rdf in path", foundRdfDocument);
    }

    private void assertWorkspace(LumifyApi lumifyApi) throws ApiException {
        addAllVerticesExceptArtifactToWorkspace(lumifyApi);
        assertWorkspaceVertices(lumifyApi);
        assertWorkspaceEdges(lumifyApi);
    }

    private void addAllVerticesExceptArtifactToWorkspace(LumifyApi lumifyApi) throws ApiException {
        ClientApiVertexSearchResponse vertices = lumifyApi.getVertexApi().vertexSearch("*");
        ClientApiWorkspaceUpdateData workspaceUpdateData = new ClientApiWorkspaceUpdateData();
        for (ClientApiVertex v : vertices.getVertices()) {
            if (v.getId().equals(artifactVertexId)) {
                continue;
            }
            ClientApiWorkspaceUpdateData.EntityUpdate entityUpdate = new ClientApiWorkspaceUpdateData.EntityUpdate();
            entityUpdate.setVertexId(v.getId());
            entityUpdate.setGraphPosition(new GraphPosition(10, 10));
            workspaceUpdateData.getEntityUpdates().add(entityUpdate);
        }
        lumifyApi.getWorkspaceApi().update(workspaceUpdateData);
    }

    private void assertWorkspaceVertices(LumifyApi lumifyApi) throws ApiException {
        ClientApiWorkspaceVertices vertices = lumifyApi.getWorkspaceApi().getVertices();
        LOGGER.info("workspace vertices: %s", vertices.toString());
        assertEquals(3, vertices.getVertices().size());
    }

    private void assertWorkspaceEdges(LumifyApi lumifyApi) throws ApiException {
        List<String> additionalIds = new ArrayList<>();
        additionalIds.add(artifactVertexId);
        ClientApiWorkspaceEdges edges = lumifyApi.getWorkspaceApi().getEdges(additionalIds);
        LOGGER.info("workspace edges: %s", edges.toString());
        assertEquals(5, edges.getEdges().size());
    }

    private void assertGetEdgesWithVisibilities() throws ApiException {
        int existingEdgeCount = 0;
        long existingEdgeTotalCount = 0;
        String altamiraCorporationVertexId = null;

        LumifyApi lumifyApi = login(USERNAME_TEST_USER_1);

        ClientApiVertexSearchResponse vertices = lumifyApi.getVertexApi().vertexSearch("*");
        boolean foundAndChangedVertexVisibility = false;
        for (ClientApiVertex v : vertices.getVertices()) {
            if (v.getId().contains("PERSON_Joe_Ferner")) {
                lumifyApi.getVertexApi().setVisibility(v.getId(), "auth2");
                foundAndChangedVertexVisibility = true;
            } else if (v.getId().contains("COMPANY_Altamira_Corporation")) {
                altamiraCorporationVertexId = v.getId();
                ClientApiVertexEdges existingEdges = lumifyApi.getVertexApi().getEdges(altamiraCorporationVertexId, null, 0, 100);
                existingEdgeCount = existingEdges.getRelationships().size();
                existingEdgeTotalCount = existingEdges.getTotalReferences();
            }
        }
        assertTrue("could not find or change vertex visibility", foundAndChangedVertexVisibility);
        assertNotNull("altamiraCorporationVertexId was null", altamiraCorporationVertexId);
        assertEquals(3, existingEdgeCount);
        assertEquals(3, existingEdgeTotalCount);

        lumifyApi.logout();

        lumifyApi = login(USERNAME_TEST_USER_2);

        ClientApiVertexEdges existingEdges = lumifyApi.getVertexApi().getEdges(altamiraCorporationVertexId, null, 0, 100);
        assertEquals(2, existingEdges.getRelationships().size());
        assertEquals(2, existingEdges.getTotalReferences());

        existingEdges = lumifyApi.getVertexApi().getEdges(altamiraCorporationVertexId, null, 0, 1);
        assertEquals(1, existingEdges.getRelationships().size());
        assertEquals(2, existingEdges.getTotalReferences());

        lumifyApi.logout();
    }
}
