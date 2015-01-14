package io.lumify.it;

import com.google.common.collect.ImmutableList;
import io.lumify.web.clientapi.LumifyApi;
import io.lumify.web.clientapi.codegen.ApiException;
import io.lumify.web.clientapi.codegen.VertexApi;
import io.lumify.web.clientapi.model.ClientApiVertex;
import io.lumify.web.clientapi.model.ClientApiVertexMultipleResponse;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

@Category(BenchmarkCategory.class)
public class VertexBenchmarkTest extends VertextTestBase {
    private TimedExecution timedExecution;

    @Rule
    public TestClassAndMethod testClassAndMethod = new TestClassAndMethod();

    @Before
    public void setUp() throws ApiException {
        super.setUp();
        timedExecution = new TimedExecution(testClassAndMethod);
    }

    @Test
    public void benchmarkFindMultiple10Vertices10Properties() {
        benchmarkFindMultipleVertices(10, 10, 60);
    }

    @Test
    public void benchmarkFindMultiple100Vertices10Properties() {
        benchmarkFindMultipleVertices(100, 10, 250);
    }

    private void benchmarkFindMultipleVertices(int numVertices, int numPropertiesPerVertex, long maxTimeMillis) {
        try {
            final List<String> allVertexVisibilities = ImmutableList.of("a", "b", "c");
            final List<String> allPropertyVisibilities = ImmutableList.of("x", "y", "z");
            final List<String> allVertexIds = createVertices(
                    numVertices, allVertexVisibilities,
                    numPropertiesPerVertex, allPropertyVisibilities);
            String setupWorkspaceId = setupLumifyApi.getCurrentWorkspaceId();
            LumifyApi lumifyApi = login(USERNAME_TEST_USER_2);
            List<String> userVertexAuthorizations = allVertexVisibilities.subList(0, 2);
            List<String> userPropertyAuthorizations = allPropertyVisibilities.subList(0, 2);
            List<String> allUserAuthorizations = new ArrayList<>();
            allUserAuthorizations.addAll(userVertexAuthorizations);
            allUserAuthorizations.addAll(userPropertyAuthorizations);
            allUserAuthorizations.add(setupWorkspaceId);
            addUserAuths(lumifyApi, USERNAME_TEST_USER_2,
                    allUserAuthorizations.toArray(new String[allUserAuthorizations.size()]));
            final VertexApi vertexApi = lumifyApi.getVertexApi();

            TimedExecution.Result<ClientApiVertexMultipleResponse> timedResponse = timedExecution.call(
                    new Callable<ClientApiVertexMultipleResponse>() {
                        public ClientApiVertexMultipleResponse call() throws Exception {
                            return vertexApi.findMultiple(allVertexIds, false);
                        }
                    });
            List<ClientApiVertex> vertices = timedResponse.result.getVertices();

            int expectedVertexCount = (numVertices * userVertexAuthorizations.size() / allVertexVisibilities.size()) +
                    numVertices % allVertexVisibilities.size();
            assertEquals(expectedVertexCount, vertices.size());
            assertThat(timedResponse.timeMillis, lessThanOrEqualTo(maxTimeMillis));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
