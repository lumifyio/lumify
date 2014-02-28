package com.altamiracorp.lumify.core;

import com.altamiracorp.lumify.core.model.termMention.TermMentionGraphVertexIdIterable;
import com.altamiracorp.lumify.core.model.termMention.TermMentionModel;
import com.altamiracorp.lumify.core.model.textHighlighting.OffsetItem;
import com.altamiracorp.lumify.core.model.textHighlighting.TermMentionOffsetItem;
import com.altamiracorp.lumify.core.util.CollectionUtil;
import com.altamiracorp.securegraph.Authorizations;
import com.altamiracorp.securegraph.Graph;
import com.altamiracorp.securegraph.Vertex;
import com.google.inject.Inject;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.*;

import static com.altamiracorp.lumify.core.util.CollectionUtil.toMap;

public class EntityHighlighter {
    private final Graph graph;

    @Inject
    public EntityHighlighter(Graph graph) {
        this.graph = graph;
    }

    public String getHighlightedText(String text, Iterable<TermMentionModel> termMentions, Authorizations authorizations) {
        List<OffsetItem> offsetItems = convertTermMentionsToOffsetItems(termMentions, authorizations);
        return getHighlightedText(text, 0, offsetItems);
    }

    // TODO: change to use an InputStream?
    public static String getHighlightedText(String text, int textStartOffset, List<OffsetItem> offsetItems) throws JSONException {
        Collections.sort(offsetItems);
        StringBuilder result = new StringBuilder();
        PriorityQueue<Integer> endOffsets = new PriorityQueue<Integer>();
        int lastStart = textStartOffset;
        for (int i = 0; i < offsetItems.size(); i++) {
            OffsetItem offsetItem = offsetItems.get(i);

            boolean overlapsPreviousItem = false;
            if (offsetItem instanceof TermMentionOffsetItem) {
                for (int j = 0; j < i; j++) {
                    OffsetItem compareItem = offsetItems.get(j);
                    if (compareItem instanceof TermMentionOffsetItem && (compareItem.getEnd() >= offsetItem.getEnd()
                            || compareItem.getEnd() > offsetItem.getStart())) {
                        overlapsPreviousItem = true;
                        offsetItems.remove(i--);
                        break;
                    }
                }
            }
            if (overlapsPreviousItem) {
                continue;
            }
            if (offsetItem.getStart() < textStartOffset || offsetItem.getEnd() < textStartOffset) {
                continue;
            }
            if (!offsetItem.shouldHighlight()) {
                continue;
            }

            while (endOffsets.size() > 0 && endOffsets.peek() <= offsetItem.getStart()) {
                int end = endOffsets.poll();
                result.append(text.substring(lastStart - textStartOffset, end - textStartOffset));
                result.append("</span>");
                lastStart = end;
            }
            result.append(text.substring(lastStart - textStartOffset, (int) (offsetItem.getStart() - textStartOffset)));

            JSONObject infoJson = offsetItem.getInfoJson();

            result.append("<span");
            result.append(" class=\"");
            result.append(StringUtils.join(offsetItem.getCssClasses(), " "));
            result.append("\"");
            if (offsetItem.getTitle() != null) {
                result.append(" title=\"");
                result.append(StringEscapeUtils.escapeHtml(offsetItem.getTitle()));
                result.append("\"");
            }
            result.append(" data-info=\"");
            result.append(StringEscapeUtils.escapeHtml(infoJson.toString()));
            result.append("\"");
            result.append(">");
            endOffsets.add((int) offsetItem.getEnd());
            lastStart = (int) offsetItem.getStart();
        }

        while (endOffsets.size() > 0) {
            int end = endOffsets.poll();
            result.append(text.substring(lastStart - textStartOffset, end - textStartOffset));
            result.append("</span>");
            lastStart = end;
        }
        result.append(text.substring(lastStart - textStartOffset));

        return result.toString();
    }

    public List<OffsetItem> convertTermMentionsToOffsetItems(Iterable<TermMentionModel> termMentions, Authorizations authorizations) {
        Map<Object, Vertex> graphVertices = getGraphVertices(termMentions, authorizations);
        ArrayList<OffsetItem> termMetadataOffsetItems = new ArrayList<OffsetItem>();
        for (TermMentionModel termMention : termMentions) {
            String graphVertexId = termMention.getMetadata().getGraphVertexId();
            Vertex graphVertex = graphVertices.get(graphVertexId);
            termMetadataOffsetItems.add(new TermMentionOffsetItem(termMention, graphVertex));
        }
        return termMetadataOffsetItems;
    }

    private Map<Object, Vertex> getGraphVertices(final Iterable<TermMentionModel> termMentions, Authorizations authorizations) {
        Iterable<Object> graphVertexIds = new TermMentionGraphVertexIdIterable(termMentions);
        Iterable<Vertex> vertices = graph.getVertices(graphVertexIds, authorizations);
        return toMap(vertices, new CollectionUtil.ValueToKey<Vertex, Object>() {
            @Override
            public Object toKey(Vertex v) {
                return v.getId();
            }
        });
    }
}
