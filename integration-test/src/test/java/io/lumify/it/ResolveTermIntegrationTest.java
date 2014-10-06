package io.lumify.it;

import io.lumify.core.model.properties.LumifyProperties;
import io.lumify.tikaTextExtractor.TikaTextExtractorGraphPropertyWorker;
import io.lumify.web.clientapi.LumifyApi;
import io.lumify.web.clientapi.codegen.ApiException;
import io.lumify.web.clientapi.codegen.model.*;
import io.lumify.web.clientapi.model.ArtifactImportResponse;
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
        assertPublishAll(lumifyApi, 2);
        lumifyTestCluster.processGraphPropertyQueue();
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

        assertPublishAll(lumifyApi, 14);

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
        assertTrue("highlightedText did not contain string: " + highlightedText, highlightedText.contains("resolvedToVertexId&quot;:&quot;" + joeFernerVertex.getId() + "&quot;"));
    }

    public void assertDiff(LumifyApi lumifyApi) throws ApiException {
        WorkspaceDiff diff;
        diff = lumifyApi.getWorkspaceApi().getDiff();
        LOGGER.info("assertDiff: %s", diff.toString());
        assertEquals(2, diff.getDiffs().size());
        String edgeId = null;
        boolean foundEdgeDiffItem = false;
        boolean foundEdgeVisibilityJsonDiffItem = false;
        for (WorkspaceDiffItem workspaceDiffItem : diff.getDiffs()) {
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
    }

    private void assertHighlightedTextDoesNotContainResolvedEntityForOtherUser(LumifyApi lumifyApi) throws ApiException {
        String highlightedText = lumifyApi.getArtifactApi().getHighlightedText(artifactVertexId, TikaTextExtractorGraphPropertyWorker.MULTI_VALUE_KEY);
        LOGGER.info("%s", highlightedText);
        assertFalse("highlightedText contained string: " + highlightedText, highlightedText.contains("resolvedToVertexId&quot;:&quot;" + joeFernerVertex.getId() + "&quot;"));
    }

    private void assertHighlightedTextContainResolvedEntityForOtherUser(LumifyApi lumifyApi) throws ApiException {
        String highlightedText = lumifyApi.getArtifactApi().getHighlightedText(artifactVertexId, TikaTextExtractorGraphPropertyWorker.MULTI_VALUE_KEY);
        LOGGER.info("%s", highlightedText);
        assertTrue("highlightedText does not contain string: " + highlightedText, highlightedText.contains("resolvedToVertexId&quot;:&quot;" + joeFernerVertex.getId() + "&quot;"));
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
        LOGGER.info("termMentions: %s", termMentions.toString());
        assertEquals(4, termMentions.getTermMentions().size());
        Element davidSingleyTermMention = findDavidSingleyTermMention(termMentions);
        LOGGER.info("termMention: %s", davidSingleyTermMention.toString());

        String highlightedText = lumifyApi.getArtifactApi().getHighlightedText(artifactVertexId, TikaTextExtractorGraphPropertyWorker.MULTI_VALUE_KEY);
        LOGGER.info("highlightedText: %s", highlightedText);
        Property davidSingleyEdgeId = getProperty(davidSingleyTermMention.getProperties(), "", "http://lumify.io/termMention#resolvedEdgeId");
        String davidSingleyEdgeIdValue = (String) davidSingleyEdgeId.getValue();
        assertTrue("highlightedText invalid: " + highlightedText, highlightedText.contains(">David Singley<") && highlightedText.contains(davidSingleyEdgeIdValue));

        lumifyApi.getEntityApi().unresolveTerm(davidSingleyTermMention.getId());

        termMentions = lumifyApi.getVertexApi().getTermMentions(artifactVertexId, TikaTextExtractorGraphPropertyWorker.MULTI_VALUE_KEY, LumifyProperties.TEXT.getPropertyName());
        LOGGER.info("termMentions: %s", termMentions.toString());
        assertEquals(3, termMentions.getTermMentions().size());

        highlightedText = lumifyApi.getArtifactApi().getHighlightedText(artifactVertexId, TikaTextExtractorGraphPropertyWorker.MULTI_VALUE_KEY);
        LOGGER.info("highlightedText: %s", highlightedText);
        assertTrue("highlightedText invalid: " + highlightedText, highlightedText.contains(">David Singley<") && !highlightedText.contains(davidSingleyEdgeIdValue));
    }

    private Element findDavidSingleyTermMention(TermMentions termMentions) {
        for (Element termMention : termMentions.getTermMentions()) {
            for (Property property : termMention.getProperties()) {
                if (property.getName().equals(LumifyProperties.TERM_MENTION_TITLE.getPropertyName())) {
                    if ("David Singley".equals(property.getValue())) {
                        return termMention;
                    }
                }
            }
        }
        throw new RuntimeException("Could not find 'David Singley' in term mentions");
    }
}
