package io.lumify.it;

import io.lumify.clavin.ClavinTermMentionFilter;
import io.lumify.core.model.properties.LumifyProperties;
import io.lumify.web.clientapi.LumifyApi;
import io.lumify.web.clientapi.codegen.ApiException;
import io.lumify.web.clientapi.model.*;
import io.lumify.zipCodeResolver.ZipCodeResolverTermMentionFilter;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

import static org.junit.Assert.*;

public class WorkspaceUndoIntegrationTest extends TestBase {
    public static final String FILE_CONTENTS = "Susan Feng knows Jeff Kunkle. They both worked in Reston, VA, 20191";

    @Test
    public void testWorkspaceUndo() throws IOException, ApiException {
        importArtifact();
        undoAll();
    }

    private void importArtifact() throws IOException, ApiException {
        LumifyApi lumifyApi = login(USERNAME_TEST_USER_1);
        addUserAuths(lumifyApi, USERNAME_TEST_USER_1, "auth1");
        ClientApiArtifactImportResponse artifact = lumifyApi.getVertexApi().importFile("auth1", "test.txt", new ByteArrayInputStream(FILE_CONTENTS.getBytes()));
        assertEquals(1, artifact.getVertexIds().size());
        String artifactVertexId = artifact.getVertexIds().get(0);
        assertNotNull(artifactVertexId);

        lumifyTestCluster.processGraphPropertyQueue();

        ClientApiElement susanFengVertex = lumifyApi.getVertexApi().create(TestOntology.CONCEPT_PERSON, "");
        lumifyApi.getVertexApi().setProperty(susanFengVertex.getId(), TEST_MULTI_VALUE_KEY, LumifyProperties.TITLE.getPropertyName(), "Susan Feng", "", "test", null, null);

        ClientApiElement jeffKunkleVertex = lumifyApi.getVertexApi().create(TestOntology.CONCEPT_PERSON, "");
        lumifyApi.getVertexApi().setProperty(jeffKunkleVertex.getId(), TEST_MULTI_VALUE_KEY, LumifyProperties.TITLE.getPropertyName(), "Jeff Kunkle", "", "test", null, null);

        ClientApiEdgeWithVertexData clientApiEdgeWithVertexData = lumifyApi.getEdgeApi().create(susanFengVertex.getId(), jeffKunkleVertex.getId(), TestOntology.EDGE_LABEL_WORKS_FOR, "");
        lumifyApi.getEdgeApi().setProperty(clientApiEdgeWithVertexData.getId(), "key1", "http://lumify.io/test#firstName", "edge property value", "", "");
        ClientApiVertexEdges edges = lumifyApi.getVertexApi().getEdges(susanFengVertex.getId(), null, null, null);
        assertEquals(1, edges.getRelationships().size());
        List<ClientApiProperty> edgeProperties = edges.getRelationships().get(0).getRelationship().getProperties();
        assertEquals(7, edgeProperties.size());
        boolean foundFirstNameEdgeProperty = false;
        for (ClientApiProperty edgeProperty : edgeProperties) {
            if (edgeProperty.getKey().equals("key1") && edgeProperty.getName().equals("http://lumify.io/test#firstName")) {
                assertEquals("edge property value", edgeProperty.getValue().toString());
                foundFirstNameEdgeProperty = true;
            }
        }
        assertTrue(foundFirstNameEdgeProperty);

        edges = lumifyApi.getVertexApi().getEdges(artifactVertexId, null, null, null);
        assertEquals(2, edges.getRelationships().size());
        ClientApiElement restonVertex = edges.getRelationships().get(0).getVertex();
        assertHasProperty(restonVertex.getProperties(), ClavinTermMentionFilter.MULTI_VALUE_PROPERTY_KEY, LumifyProperties.CONCEPT_TYPE.getPropertyName(), TestOntology.CONCEPT_CITY);
        assertHasProperty(restonVertex.getProperties(), ClavinTermMentionFilter.MULTI_VALUE_PROPERTY_KEY, LumifyProperties.SOURCE.getPropertyName(), "CLAVIN");
        assertHasProperty(restonVertex.getProperties(), ClavinTermMentionFilter.MULTI_VALUE_PROPERTY_KEY, LumifyProperties.TITLE.getPropertyName(), "Reston (US, VA)");
        VisibilityJson visibilityJson = new VisibilityJson();
        visibilityJson.setSource("auth1");
        visibilityJson.addWorkspace(lumifyApi.getCurrentWorkspaceId());
        assertHasProperty(restonVertex.getProperties(), ClavinTermMentionFilter.MULTI_VALUE_PROPERTY_KEY, LumifyProperties.VISIBILITY_JSON.getPropertyName(), visibilityJson);

        ClientApiElement zipCodeVertex = edges.getRelationships().get(1).getVertex();
        assertHasProperty(zipCodeVertex.getProperties(), ZipCodeResolverTermMentionFilter.MULTI_VALUE_PROPERTY_KEY, LumifyProperties.CONCEPT_TYPE.getPropertyName(), TestOntology.CONCEPT_ZIP_CODE);
        assertHasProperty(zipCodeVertex.getProperties(), ZipCodeResolverTermMentionFilter.MULTI_VALUE_PROPERTY_KEY, LumifyProperties.SOURCE.getPropertyName(), "Zip Code Resolver");
        assertHasProperty(zipCodeVertex.getProperties(), ZipCodeResolverTermMentionFilter.MULTI_VALUE_PROPERTY_KEY, LumifyProperties.TITLE.getPropertyName(), "20191 - Reston, VA");
        visibilityJson = new VisibilityJson();
        visibilityJson.setSource("auth1");
        visibilityJson.addWorkspace(lumifyApi.getCurrentWorkspaceId());
        assertHasProperty(zipCodeVertex.getProperties(), ZipCodeResolverTermMentionFilter.MULTI_VALUE_PROPERTY_KEY, LumifyProperties.VISIBILITY_JSON.getPropertyName(), visibilityJson);

        lumifyApi.logout();
    }

    private void undoAll() throws IOException, ApiException {
        LumifyApi lumifyApi = login(USERNAME_TEST_USER_1);
        assertUndoAll(lumifyApi, 35);
        lumifyApi.logout();
    }
}
