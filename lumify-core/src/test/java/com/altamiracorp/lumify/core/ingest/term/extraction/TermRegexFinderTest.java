package com.altamiracorp.lumify.core.ingest.term.extraction;

import com.altamiracorp.lumify.core.model.graph.GraphVertex;
import com.altamiracorp.lumify.core.model.ontology.PropertyName;
import com.altamiracorp.lumify.core.model.termMention.TermMentionModel;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TermRegexFinderTest {
    @Mock
    private GraphVertex concept;

    @Before
    public void setup() {
        when(concept.getId()).thenReturn("1");
        when(concept.getProperty(PropertyName.DISPLAY_NAME)).thenReturn("testConcept");
    }

    @Test
    public void testFind() {
        String artifactId = "1234";
        String text = "@item1 is a @item2";
        String regex = "(@(\\w+))";
        List<TermMentionModel> terms = TermRegexFinder.find(artifactId, concept, text, regex);
        assertEquals(2, terms.size());

        TermMentionModel termMention1 = terms.get(0);
        assertEquals(0, termMention1.getRowKey().getStartOffset());
        assertEquals(6, termMention1.getRowKey().getEndOffset());
        assertEquals("item1", termMention1.getMetadata().getSign());

        TermMentionModel termMention2 = terms.get(1);
        assertEquals(12, termMention2.getRowKey().getStartOffset());
        assertEquals(18, termMention2.getRowKey().getEndOffset());
        assertEquals("item2", termMention2.getMetadata().getSign());
    }

    @Test
    public void testFindSameCaptureGroup() {
        String artifactId = "1234";
        String text = "@item1 is a @item2";
        String regex = "((@\\w+))";
        List<TermMentionModel> terms = TermRegexFinder.find(artifactId, concept, text, regex);
        assertEquals(2, terms.size());

        TermMentionModel termMention1 = terms.get(0);
        assertEquals(0, termMention1.getRowKey().getStartOffset());
        assertEquals(6, termMention1.getRowKey().getEndOffset());
        assertEquals("@item1", termMention1.getMetadata().getSign());

        TermMentionModel termMention2 = terms.get(1);
        assertEquals(12, termMention2.getRowKey().getStartOffset());
        assertEquals(18, termMention2.getRowKey().getEndOffset());
        assertEquals("@item2", termMention2.getMetadata().getSign());
    }
}
