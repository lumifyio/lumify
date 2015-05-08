package io.lumify.it;

import static junit.framework.TestCase.assertEquals;

import io.lumify.web.clientapi.LumifyApi;
import io.lumify.web.clientapi.VertexApiExt.FileForImport;
import io.lumify.web.clientapi.codegen.ApiException;
import io.lumify.web.clientapi.model.ClientApiArtifactImportResponse;
import io.lumify.web.clientapi.model.ClientApiVertex;
import io.lumify.web.clientapi.model.ClientApiVertexSearchResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import org.junit.Test;

public class UploadCsvIT extends TestBase {
    private static final Charset UTF8 = Charset.forName("UTF8");
    private static final String FILE_CONTENTS = getResourceString("sample.csv");
    private static final String MAPPING_JSON = getResourceString("sample.csv.mapping.json");

    @Test
    public void testUploadCsv() throws IOException, ApiException {
        uploadAndProcessCsv();
        assertUser2CanSeeCsvVertices();
    }

    public void uploadAndProcessCsv() throws ApiException, IOException {
        LumifyApi lumifyApi = login(USERNAME_TEST_USER_1);
        addUserAuths(lumifyApi, USERNAME_TEST_USER_1, "auth1");

        FileForImport csvFile = new FileForImport("auth1", "sample.csv", new ByteArrayInputStream(FILE_CONTENTS.getBytes(UTF8)));
        FileForImport mappingFile = new FileForImport("auth1", "sample.csv.mapping.json", new ByteArrayInputStream(MAPPING_JSON.getBytes(UTF8)));
        ClientApiArtifactImportResponse artifact = lumifyApi.getVertexApi().importFiles(csvFile, mappingFile);

        lumifyTestCluster.processGraphPropertyQueue();

        assertPublishAll(lumifyApi, 45);

        ClientApiVertexSearchResponse searchResults = lumifyApi.getVertexApi().vertexSearch("*");
        LOGGER.info("searchResults (user1): %s", searchResults);
        assertEquals(8, searchResults.getVertices().size());
        for (ClientApiVertex v : searchResults.getVertices()) {
            assertEquals("auth1", v.getVisibilitySource());
        }

        lumifyApi.logout();
    }

    private void assertUser2CanSeeCsvVertices() throws ApiException {
        LumifyApi lumifyApi = login(USERNAME_TEST_USER_2);
        addUserAuths(lumifyApi, USERNAME_TEST_USER_2, "auth1");

        ClientApiVertexSearchResponse searchResults = lumifyApi.getVertexApi().vertexSearch("*");
        LOGGER.info("searchResults (user2): %s", searchResults);
        assertEquals(8, searchResults.getVertices().size());

        lumifyApi.logout();
    }
}
