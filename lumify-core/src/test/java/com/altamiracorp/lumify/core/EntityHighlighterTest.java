package com.altamiracorp.lumify.core;

import com.altamiracorp.lumify.core.model.termMention.TermMentionModel;
import com.altamiracorp.lumify.core.model.termMention.TermMentionRowKey;
import com.altamiracorp.lumify.core.model.textHighlighting.OffsetItem;
import com.altamiracorp.lumify.core.model.textHighlighting.TermMentionOffsetItem;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.securegraph.Authorizations;
import com.altamiracorp.securegraph.Graph;
import com.altamiracorp.securegraph.Vertex;
import com.altamiracorp.securegraph.Visibility;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class EntityHighlighterTest {

    @Mock
    Graph graph;

    @Mock
    private User user;

    @Mock
    private Authorizations authorizations;

    @Mock
    private Visibility visibility;

    @Test
    public void testGetHighlightedText() throws Exception {
        when(graph.getVertices((Iterable<Object>) any(), eq(authorizations))).thenReturn(new ArrayList<Vertex>());

        ArrayList<TermMentionModel> terms = new ArrayList<TermMentionModel>();
        terms.add(createTermMention("joe ferner", 18, 28, "1"));
        terms.add(createTermMention("jeff kunkle", 33, 44, "1"));
        List<OffsetItem> termAndTermMetadata = new EntityHighlighter().convertTermMentionsToOffsetItems(terms);
        String highlightText = EntityHighlighter.getHighlightedText("Test highlight of Joe Ferner and Jeff Kunkle.", 0, termAndTermMetadata);
        assertEquals("Test highlight of <span class=\"entity\" title=\"joe ferner\" data-info=\"{&quot;title&quot;:&quot;joe ferner&quot;,&quot;start&quot;:18,&quot;_rowKey&quot;:&quot;1:0000000000000028:0000000000000018&quot;,&quot;type&quot;:&quot;http://www.w3.org/2002/07/owl#Thing&quot;,&quot;end&quot;:28}\">Joe Ferner</span> and <span class=\"entity\" title=\"jeff kunkle\" data-info=\"{&quot;title&quot;:&quot;jeff kunkle&quot;,&quot;start&quot;:33,&quot;_rowKey&quot;:&quot;1:0000000000000044:0000000000000033&quot;,&quot;type&quot;:&quot;http://www.w3.org/2002/07/owl#Thing&quot;,&quot;end&quot;:44}\">Jeff Kunkle</span>.", highlightText);
    }

    private TermMentionModel createTermMention(String sign, int start, int end, String artifactGraphVertexId) {
        TermMentionModel termMention = new TermMentionModel(new TermMentionRowKey(artifactGraphVertexId, start, end));
        termMention.getMetadata().setSign(sign, visibility);
        return termMention;
    }

    public void testGetHighlightedTextOverlaps() throws Exception {
        ArrayList<TermMentionModel> terms = new ArrayList<TermMentionModel>();
        terms.add(createTermMention("joe ferner", 18, 28, "1"));
        terms.add(createTermMention("jeff kunkle", 18, 21, "1"));
        List<OffsetItem> termAndTermMetadata = new EntityHighlighter().convertTermMentionsToOffsetItems(terms);
        String highlightText = EntityHighlighter.getHighlightedText("Test highlight of Joe Ferner.", 0, termAndTermMetadata);
        assertEquals("Test highlight of <span class=\"entity person\" term-key=\"joe ferner\\x1Fee\\x1Fperson\"><span class=\"entity person\" term-key=\"joe\\x1Fee\\x1Fperson\">Joe</span> Ferner</span>.", highlightText);
    }

    @Test
    public void testGetHighlightedTextNestedEntity() throws Exception {
        String text = "This is a test sentence";
        List<OffsetItem> offsetItems = new ArrayList<OffsetItem>();

        OffsetItem mockEntity1 = mock(TermMentionOffsetItem.class);
        when(mockEntity1.getStart()).thenReturn(0l);
        when(mockEntity1.getEnd()).thenReturn(4l);
        when(mockEntity1.getGraphVertexId()).thenReturn("0");
        when(mockEntity1.getCssClasses()).thenReturn(asList(new String[]{"first"}));
        when(mockEntity1.shouldHighlight()).thenReturn(true);
        when(mockEntity1.getInfoJson()).thenReturn(new JSONObject("{\"data\":\"attribute\"}"));
        offsetItems.add(mockEntity1);

        OffsetItem mockEntity2 = mock(TermMentionOffsetItem.class);
        when(mockEntity2.getStart()).thenReturn(0l);
        when(mockEntity2.getEnd()).thenReturn(4l);
        when(mockEntity2.getGraphVertexId()).thenReturn("1");
        when(mockEntity2.getCssClasses()).thenReturn(asList(new String[]{"second"}));
        when(mockEntity2.shouldHighlight()).thenReturn(true);
        when(mockEntity2.getInfoJson()).thenReturn(new JSONObject("{\"data\":\"attribute\"}"));
        offsetItems.add(mockEntity2);

        OffsetItem mockEntity3 = mock(TermMentionOffsetItem.class);
        when(mockEntity3.getStart()).thenReturn(0l);
        when(mockEntity3.getEnd()).thenReturn(7l);
        when(mockEntity3.getCssClasses()).thenReturn(asList(new String[]{"third"}));
        when(mockEntity3.shouldHighlight()).thenReturn(true);
        when(mockEntity3.getInfoJson()).thenReturn(new JSONObject("{\"data\":\"attribute\"}"));
        offsetItems.add(mockEntity3);

        OffsetItem mockEntity4 = mock(TermMentionOffsetItem.class);
        when(mockEntity4.getStart()).thenReturn(5l);
        when(mockEntity4.getEnd()).thenReturn(9l);
        when(mockEntity4.getCssClasses()).thenReturn(asList(new String[]{"fourth"}));
        when(mockEntity4.shouldHighlight()).thenReturn(true);
        when(mockEntity4.getInfoJson()).thenReturn(new JSONObject("{\"data\":\"attribute\"}"));
        offsetItems.add(mockEntity4);

        OffsetItem mockEntity5 = mock(TermMentionOffsetItem.class);
        when(mockEntity5.getStart()).thenReturn(15l);
        when(mockEntity5.getEnd()).thenReturn(23l);
        when(mockEntity5.getCssClasses()).thenReturn(asList(new String[]{"fifth"}));
        when(mockEntity5.shouldHighlight()).thenReturn(true);
        when(mockEntity5.getInfoJson()).thenReturn(new JSONObject("{\"data\":\"attribute\"}"));
        offsetItems.add(mockEntity5);

        String highlightedText = EntityHighlighter.getHighlightedText(text, 0, offsetItems);
        Assert.assertEquals("<span class=\"first\" data-info=\"{&quot;data&quot;:&quot;attribute&quot;}\">This</span> " +
                        "<span class=\"fourth\" data-info=\"{&quot;data&quot;:&quot;attribute&quot;}\">is a</span> test <span " +
                        "class=\"fifth\" data-info=\"{&quot;data&quot;:&quot;attribute&quot;}\">sentence</span>",
                highlightedText
        );
    }

    private List<String> asList(String[] strings) {
        List<String> results = new ArrayList<String>();
        for (String s : strings) {
            results.add(s);
        }
        return results;
    }
}
