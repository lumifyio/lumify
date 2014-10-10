package io.lumify.it;

import io.lumify.csv.CsvOntology;
import io.lumify.web.clientapi.LumifyApi;
import io.lumify.web.clientapi.codegen.ApiException;
import io.lumify.web.clientapi.model.ArtifactImportResponse;
import io.lumify.web.clientapi.model.Vertex;
import io.lumify.web.clientapi.model.VertexSearchResponse;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static junit.framework.TestCase.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class UploadCsvIntegrationTest extends TestBase {
    private static final String FILE_CONTENTS = getResourceString("sample.csv");
    private static final String MAPPING_JSON = getResourceString("sample.csv.mapping.json");
    private String artifactVertexId;

    @Test
    public void testUploadCsv() throws IOException, ApiException {
        uploadAndProcessCsv();
        assertUser2CanSeeCsvVertices();
    }

    public void uploadAndProcessCsv() throws ApiException, IOException {
        LumifyApi lumifyApi = login(USERNAME_TEST_USER_1);
        addUserAuth(lumifyApi, USERNAME_TEST_USER_1, "auth1");

        ArtifactImportResponse artifact = lumifyApi.getVertexApi().importFile("auth1", "sample.csv", new ByteArrayInputStream(FILE_CONTENTS.getBytes()));
        artifactVertexId = artifact.getVertexIds().get(0);

        lumifyApi.getVertexApi().setProperty(artifactVertexId, "", CsvOntology.MAPPING_JSON.getPropertyName(), MAPPING_JSON, "", "", null, null);

        lumifyTestCluster.processGraphPropertyQueue();

        assertPublishAll(lumifyApi, 46);

        VertexSearchResponse searchResults = lumifyApi.getVertexApi().vertexSearch("*");
        LOGGER.info("searchResults (user1): %s", searchResults);
        assertEquals(8, searchResults.getVertices().size());
        for (Vertex v : searchResults.getVertices()) {
            assertEquals("auth1", v.getVisibilitySource());
        }

        lumifyApi.logout();
    }

    private void assertUser2CanSeeCsvVertices() throws ApiException {
        LumifyApi lumifyApi = login(USERNAME_TEST_USER_2);
        addUserAuth(lumifyApi, USERNAME_TEST_USER_2, "auth1");

        VertexSearchResponse searchResults = lumifyApi.getVertexApi().vertexSearch("*");
        LOGGER.info("searchResults (user2): %s", searchResults);
        assertEquals(8, searchResults.getVertices().size());

        lumifyApi.logout();
    }
}
