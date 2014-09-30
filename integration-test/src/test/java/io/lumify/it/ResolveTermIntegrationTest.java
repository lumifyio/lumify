package io.lumify.it;

import io.lumify.core.model.properties.LumifyProperties;
import io.lumify.tikaTextExtractor.TikaTextExtractorGraphPropertyWorker;
import io.lumify.web.clientapi.LumifyApi;
import io.lumify.web.clientapi.codegen.ApiException;
import io.lumify.web.clientapi.codegen.model.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class ResolveTermIntegrationTest extends TestBase {
    private String artifactVertexId;
    private Element joeFernerVertex;

    @Test
    public void testResolveTerm() throws IOException, ApiException {
        setupData();

        LumifyApi lumifyApi = login(USERNAME_TEST_USER_1);
        resolveTerm(lumifyApi);
        assertHighlightedTextUpdatedWithResolvedEntity(lumifyApi);
        assertDiff(lumifyApi);
        lumifyApi.logout();

        lumifyApi = login(USERNAME_TEST_USER_2);
        addUserAuth(lumifyApi, USERNAME_TEST_USER_2, "auth1");
        assertHighlightedTextDoesNotContainResolvedEntityForOtherUser(lumifyApi);
        lumifyApi.logout();

        lumifyApi = login(USERNAME_TEST_USER_1);
        publishResolvedTerm(lumifyApi);
        lumifyApi.logout();

        lumifyApi = login(USERNAME_TEST_USER_2);
        assertHighlightedTextContainResolvedEntityForOtherUser(lumifyApi);
        lumifyApi.logout();

        lumifyApi = login(USERNAME_TEST_USER_1);
        resolveAndUnresolveTerm(lumifyApi);
        lumifyApi.logout();
    }

    public void setupData() throws ApiException, IOException {
        LumifyApi lumifyApi = login(USERNAME_TEST_USER_1);
        addUserAuth(lumifyApi, USERNAME_TEST_USER_1, "auth1");

        ArtifactImportResponse artifact = lumifyApi.getArtifactApi().importFile("auth1", "test.txt", new ByteArrayInputStream("Joe Ferner knows David Singley.".getBytes()));
        assertEquals(1, artifact.getVertexIds().size());
        artifactVertexId = artifact.getVertexIds().get(0);
        assertNotNull(artifactVertexId);

        lumifyTestCluster.processGraphPropertyQueue();

        joeFernerVertex = lumifyApi.getVertexApi().create(CONCEPT_TEST_PERSON, "auth1");
        lumifyApi.getVertexApi().setProperty(joeFernerVertex.getId(), TEST_MULTI_VALUE_KEY, LumifyProperties.TITLE.getPropertyName(), "Joe Ferner", "auth1", "test", null, null);

        lumifyTestCluster.processGraphPropertyQueue();

        WorkspaceDiff diff = lumifyApi.getWorkspaceApi().getDiff();
        PublishResponse publishResults = lumifyApi.getWorkspaceApi().publishAll(diff.getDiffs());
        assertEquals(0, publishResults.getFailures().size());
        assertTrue(publishResults.getSuccess());

        diff = lumifyApi.getWorkspaceApi().getDiff();
        assertEquals(0, diff.getDiffs().size());

        lumifyApi.logout();
    }

    public void resolveTerm(LumifyApi lumifyApi) throws ApiException {
        int entityStartOffset = "".length();
        int entityEndOffset = entityStartOffset + "Joe Ferner".length();
        lumifyApi.getEntityApi().resolveTerm(
                artifactVertexId,
                TikaTextExtractorGraphPropertyWorker.MULTI_VALUE_KEY,
                entityStartOffset, entityEndOffset,
                "Joe Ferner",
                CONCEPT_TEST_PERSON,
                "auth1",
                joeFernerVertex.getId(),
                "test",
                null);
    }

    public void assertHighlightedTextUpdatedWithResolvedEntity(LumifyApi lumifyApi) throws ApiException {
        String highlightedText = lumifyApi.getArtifactApi().getHighlightedText(artifactVertexId, TikaTextExtractorGraphPropertyWorker.MULTI_VALUE_KEY);
        LOGGER.info("%s", highlightedText);
        assertTrue("highlightedText did not contain string: " + highlightedText, highlightedText.contains("graphVertexId&quot;:&quot;" + joeFernerVertex.getId() + "&quot;"));
    }

    public void assertDiff(LumifyApi lumifyApi) throws ApiException {
        WorkspaceDiff diff;
        diff = lumifyApi.getWorkspaceApi().getDiff();
        LOGGER.info("%s", diff.toString());
        assertEquals(3, diff.getDiffs().size());
        String edgeId = null;
        boolean foundEdgeDiffItem = false;
        boolean foundEdgeVisibilityJsonDiffItem = false;
        boolean foundResolvedToRowKey = false;
        for (WorkspaceDiffItem workspaceDiffItem : diff.getDiffs()) {
            if (workspaceDiffItem.getType().equals("PropertyDiffItem")
                    && workspaceDiffItem.getElementId().equals(joeFernerVertex.getId())) {
                foundResolvedToRowKey = true;
            }
            if (workspaceDiffItem.getType().equals("EdgeDiffItem")) {
                foundEdgeDiffItem = true;
                edgeId = workspaceDiffItem.getEdgeId();
            }
        }
        for (WorkspaceDiffItem workspaceDiffItem : diff.getDiffs()) {
            if (workspaceDiffItem.getType().equals("PropertyDiffItem") && workspaceDiffItem.getElementId().equals(edgeId)) {
                foundEdgeVisibilityJsonDiffItem = true;
            }
        }
        assertTrue("foundEdgeDiffItem", foundEdgeDiffItem);
        assertTrue("foundEdgeVisibilityJsonDiffItem", foundEdgeVisibilityJsonDiffItem);
        assertTrue("foundResolvedToRowKey", foundResolvedToRowKey);
    }

    private void assertHighlightedTextDoesNotContainResolvedEntityForOtherUser(LumifyApi lumifyApi) throws ApiException {
        String highlightedText = lumifyApi.getArtifactApi().getHighlightedText(artifactVertexId, TikaTextExtractorGraphPropertyWorker.MULTI_VALUE_KEY);
        LOGGER.info("%s", highlightedText);
        assertFalse("highlightedText contained string: " + highlightedText, highlightedText.contains("graphVertexId&quot;:&quot;" + joeFernerVertex.getId() + "&quot;"));
    }

    private void publishResolvedTerm(LumifyApi lumifyApi) throws ApiException {
        WorkspaceDiff diff = lumifyApi.getWorkspaceApi().getDiff();
        LOGGER.info("publishResolvedTerm: diff: %s", diff.toString());
        PublishResponse publishResults = lumifyApi.getWorkspaceApi().publishAll(diff.getDiffs());
        LOGGER.info("publishResolvedTerm: publish response: %s", publishResults.toString());
        assertEquals(0, publishResults.getFailures().size());
        assertTrue(publishResults.getSuccess());

        diff = lumifyApi.getWorkspaceApi().getDiff();
        LOGGER.info("publishResolvedTerm: diff after publish: %s", diff.toString());
        //TODO add this back in when we switch TermMentions to graph:
        // assertEquals("too many diffs.", 0, diff.getDiffs().size());

        lumifyTestCluster.processGraphPropertyQueue();
    }

    private void assertHighlightedTextContainResolvedEntityForOtherUser(LumifyApi lumifyApi) throws ApiException {
        String highlightedText = lumifyApi.getArtifactApi().getHighlightedText(artifactVertexId, TikaTextExtractorGraphPropertyWorker.MULTI_VALUE_KEY);
        LOGGER.info("%s", highlightedText);
        assertTrue("highlightedText does not contain string: " + highlightedText, highlightedText.contains("graphVertexId&quot;:&quot;" + joeFernerVertex.getId() + "&quot;"));
    }

    private void resolveAndUnresolveTerm(LumifyApi lumifyApi) throws ApiException {
        int entityStartOffset = "Joe Ferner knows ".length();
        int entityEndOffset = entityStartOffset + "David Singley".length();
        String sign = "David Singley";
        lumifyApi.getEntityApi().resolveTerm(
                artifactVertexId,
                TikaTextExtractorGraphPropertyWorker.MULTI_VALUE_KEY,
                entityStartOffset, entityEndOffset,
                sign,
                CONCEPT_TEST_PERSON,
                "auth1",
                joeFernerVertex.getId(),
                "test",
                null);

        TermMentions termMentions = lumifyApi.getVertexApi().getTermMentions(artifactVertexId, TikaTextExtractorGraphPropertyWorker.MULTI_VALUE_KEY, LumifyProperties.TEXT.getPropertyName());
        LOGGER.info("%s", termMentions.toString());
        assertEquals(4, termMentions.getTermMentions().size());
        TermMention termMention = findDavidSingleyTermMention(termMentions);
        LOGGER.info("%s", termMention.toString());

        String highlightedText = lumifyApi.getArtifactApi().getHighlightedText(artifactVertexId, TikaTextExtractorGraphPropertyWorker.MULTI_VALUE_KEY);
        LOGGER.info("%s", highlightedText);
        assertTrue("highlightedText invalid: " + highlightedText, highlightedText.contains(">David Singley<") && highlightedText.contains(termMention.getMetadata().getEdgeId()));

        lumifyApi.getEntityApi().unresolveTerm(artifactVertexId, TikaTextExtractorGraphPropertyWorker.MULTI_VALUE_KEY, entityStartOffset, entityEndOffset, sign, CONCEPT_TEST_PERSON, termMention.getMetadata().getEdgeId());

        termMentions = lumifyApi.getVertexApi().getTermMentions(artifactVertexId, TikaTextExtractorGraphPropertyWorker.MULTI_VALUE_KEY, LumifyProperties.TEXT.getPropertyName());
        LOGGER.info("%s", termMentions.toString());
        assertEquals(3, termMentions.getTermMentions().size());

        highlightedText = lumifyApi.getArtifactApi().getHighlightedText(artifactVertexId, TikaTextExtractorGraphPropertyWorker.MULTI_VALUE_KEY);
        LOGGER.info("%s", highlightedText);
        assertTrue("highlightedText invalid: " + highlightedText, highlightedText.contains(">David Singley<") && !highlightedText.contains(termMention.getMetadata().getEdgeId()));
    }

    private TermMention findDavidSingleyTermMention(TermMentions termMentions) {
        for (TermMention termMention : termMentions.getTermMentions()) {
            if (termMention.getMetadata() == null) {
                continue;
            }
            if (termMention.getMetadata().getSign().equals("David Singley") && termMention.getMetadata().getGraphVertexId() != null) {
                return termMention;
            }
        }
        throw new RuntimeException("Could not find 'David Singley' in term mentions");
    }
}
