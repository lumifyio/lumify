package com.altamiracorp.lumify.core.ingest.term.extraction;

import static com.altamiracorp.lumify.core.model.properties.LumifyProperties.DISPLAY_NAME;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import com.altamiracorp.lumify.core.model.termMention.TermMentionModel;
import com.altamiracorp.securegraph.Text;
import com.altamiracorp.securegraph.Vertex;
import java.util.List;

import com.altamiracorp.securegraph.Visibility;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class TermRegexFinderTest {
    @Mock
    private Vertex concept;

    @Mock
    private Visibility visibility;

    @Before
    public void setup() {
        when(concept.getId()).thenReturn("1");
        when(concept.getPropertyValue(DISPLAY_NAME.getKey())).thenReturn(new Text("testConcept"));
    }

    @Test
    public void testFind() {
        String artifactId = "1234";
        String text = "@item1 is a @item2";
        String regex = "(@(\\w+))";
        List<TermMentionModel> terms = TermRegexFinder.find(artifactId, concept, text, regex, visibility);
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
        List<TermMentionModel> terms = TermRegexFinder.find(artifactId, concept, text, regex, visibility);
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
