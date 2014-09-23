package io.lumify.it;

import io.lumify.web.clientapi.LumifyApi;
import io.lumify.web.clientapi.UserNameOnlyLumifyApi;
import io.lumify.web.clientapi.codegen.ApiException;
import io.lumify.web.clientapi.codegen.model.ArtifactImportResponse;
import io.lumify.web.clientapi.codegen.model.Element;
import io.lumify.web.clientapi.codegen.model.Property;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(MockitoJUnitRunner.class)
public class UploadFileIntegrationTest extends TestBase {
    @Test
    public void testIt() throws IOException, ApiException {
        LumifyApi lumifyApi = new UserNameOnlyLumifyApi("https://localhost:" + HTTPS_PORT, username);
        lumifyApi.loginAndGetCurrentWorkspace();
        lumifyApi.getAdminApi().uploadOntology(getClass().getResourceAsStream("test.owl"));

        addUserAuth(lumifyApi, username, "auth1");

        ArtifactImportResponse artifact = lumifyApi.getArtifactApi().importFile("auth1", "test.txt", new ByteArrayInputStream("Joe Ferner knows David Singley.".getBytes()));
        assertEquals(1, artifact.getVertexIds().size());
        String artifactVertexId = artifact.getVertexIds().get(0);
        assertNotNull(artifactVertexId);

        lumifyTestCluster.processGraphPropertyQueue();

        Element artifactVertex = lumifyApi.getVertexApi().getByVertexId(artifactVertexId);
        assertEquals(artifactVertexId, artifactVertex.getId());
        assertEquals(10, artifactVertex.getProperties().size());

        for (Property property : artifactVertex.getProperties()) {
            LOGGER.info("property: %s", property.toString());
        }

        String highlightedText = lumifyApi.getArtifactApi().getHighlightedText(artifactVertexId, "io.lumify.tikaTextExtractor.TikaTextExtractorGraphPropertyWorker");
        assertNotNull(highlightedText);
        LOGGER.info("highlightedText: %s", highlightedText);
    }
}
