package com.altamiracorp.lumify.storm.textHighlighting;

import com.altamiracorp.lumify.core.model.artifactHighlighting.OffsetItem;
import com.altamiracorp.lumify.core.model.artifactHighlighting.TermMentionOffsetItem;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.core.model.graph.GraphRepository;
import com.altamiracorp.lumify.core.model.graph.GraphVertex;
import com.altamiracorp.lumify.core.model.termMention.TermMentionModel;
import com.google.inject.Inject;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.*;

public class EntityHighlighter {
    private GraphRepository graphRepository;

    @Inject
    public EntityHighlighter(GraphRepository graphRepository) {
        this.graphRepository = graphRepository;
    }

    public String getHighlightedText(String text, List<TermMentionModel> termMentions, User user) {
        List<OffsetItem> offsetItems = convertTermMentionsToOffsetItems(termMentions, user);
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

    public List<OffsetItem> convertTermMentionsToOffsetItems(Collection<TermMentionModel> termMentions, User user) {
        ArrayList<OffsetItem> termMetadataOffsetItems = new ArrayList<OffsetItem>();
        for (TermMentionModel termMention : termMentions) {
            GraphVertex glyphVertex = null;
            String graphVertexId = termMention.getMetadata().getGraphVertexId();
            if (graphVertexId != null) {
                glyphVertex = graphRepository.findVertex(graphVertexId, user);
            }
            termMetadataOffsetItems.add(new TermMentionOffsetItem(termMention, glyphVertex));
        }
        return termMetadataOffsetItems;
    }
}
