package io.lumify.core;

import io.lumify.core.ingest.video.VideoFrameInfo;
import io.lumify.core.ingest.video.VideoPropertyHelper;
import io.lumify.core.ingest.video.VideoTranscript;
import io.lumify.core.model.textHighlighting.OffsetItem;
import io.lumify.core.model.textHighlighting.VertexOffsetItem;
import io.lumify.web.clientapi.model.SandboxStatus;
import io.lumify.core.util.GraphUtil;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.securegraph.Authorizations;
import org.securegraph.Vertex;

import java.util.*;

public class EntityHighlighter {
    public String getHighlightedText(String text, Iterable<Vertex> termMentions, String workspaceId, Authorizations authorizations) {
        List<OffsetItem> offsetItems = convertTermMentionsToOffsetItems(termMentions, workspaceId, authorizations);
        return getHighlightedText(text, offsetItems);
    }

    // TODO: change to use an InputStream?
    public static String getHighlightedText(String text, List<OffsetItem> offsetItems) throws JSONException {
        Collections.sort(offsetItems);
        StringBuilder result = new StringBuilder();
        PriorityQueue<Integer> endOffsets = new PriorityQueue<>();
        int lastStart = 0;
        for (int i = 0; i < offsetItems.size(); i++) {
            OffsetItem offsetItem = offsetItems.get(i);

            boolean overlapsPreviousItem = false;
            if (offsetItem instanceof VertexOffsetItem) {
                for (int j = 0; j < i; j++) {
                    OffsetItem compareItem = offsetItems.get(j);
                    if (compareItem instanceof VertexOffsetItem
                            && (OffsetItem.getOffset(compareItem.getEnd()) >= OffsetItem.getOffset(offsetItem.getEnd())
                            || OffsetItem.getOffset(compareItem.getEnd()) > OffsetItem.getOffset(offsetItem.getStart()))) {
                        overlapsPreviousItem = true;
                        offsetItems.remove(i--);
                        break;
                    }
                }
            }
            if (overlapsPreviousItem) {
                continue;
            }
            if (OffsetItem.getOffset(offsetItem.getStart()) < 0 || OffsetItem.getOffset(offsetItem.getEnd()) < 0) {
                continue;
            }
            if (!offsetItem.shouldHighlight()) {
                continue;
            }

            while (endOffsets.size() > 0 && endOffsets.peek() <= OffsetItem.getOffset(offsetItem.getStart())) {
                int end = endOffsets.poll();
                result.append(StringEscapeUtils.escapeHtml(safeSubstring(text, lastStart, end)));
                result.append("</span>");
                lastStart = end;
            }
            result.append(StringEscapeUtils.escapeHtml(safeSubstring(text, lastStart, (int) OffsetItem.getOffset(offsetItem.getStart()))));

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
            endOffsets.add((int) OffsetItem.getOffset(offsetItem.getEnd()));
            lastStart = (int) OffsetItem.getOffset(offsetItem.getStart());
        }

        while (endOffsets.size() > 0) {
            int end = endOffsets.poll();
            result.append(StringEscapeUtils.escapeHtml(safeSubstring(text, lastStart, end)));
            result.append("</span>");
            lastStart = end;
        }
        result.append(StringEscapeUtils.escapeHtml(safeSubstring(text, lastStart)));

        return result.toString().replaceAll("&nbsp;", " ");
    }

    public VideoTranscript getHighlightedVideoTranscript(VideoTranscript videoTranscript, Iterable<Vertex> termMentions, String workspaceId, Authorizations authorizations) {
        List<OffsetItem> offsetItems = convertTermMentionsToOffsetItems(termMentions, workspaceId, authorizations);
        return getHighlightedVideoTranscript(videoTranscript, offsetItems);
    }

    private VideoTranscript getHighlightedVideoTranscript(VideoTranscript videoTranscript, List<OffsetItem> offsetItems) {
        Map<Integer, List<OffsetItem>> videoTranscriptOffsetItems = convertOffsetItemsToVideoTranscriptOffsetItems(videoTranscript, offsetItems);
        return getHighlightedVideoTranscript(videoTranscript, videoTranscriptOffsetItems);
    }

    private VideoTranscript getHighlightedVideoTranscript(VideoTranscript videoTranscript, Map<Integer, List<OffsetItem>> videoTranscriptOffsetItems) {
        VideoTranscript result = new VideoTranscript();
        int entryIndex = 0;
        for (VideoTranscript.TimedText videoTranscriptEntry : videoTranscript.getEntries()) {
            VideoTranscript.TimedText entry = videoTranscript.getEntries().get(entryIndex);

            List<OffsetItem> offsetItems = videoTranscriptOffsetItems.get(entryIndex);
            String highlightedText;
            if (offsetItems == null) {
                highlightedText = entry.getText();
            } else {
                highlightedText = getHighlightedText(entry.getText(), offsetItems);
            }
            result.add(videoTranscriptEntry.getTime(), highlightedText);
            entryIndex++;
        }
        return result;
    }

    private Map<Integer, List<OffsetItem>> convertOffsetItemsToVideoTranscriptOffsetItems(VideoTranscript videoTranscript, List<OffsetItem> offsetItems) {
        Map<Integer, List<OffsetItem>> results = new HashMap<>();
        for (OffsetItem offsetItem : offsetItems) {
            Integer videoTranscriptEntryIndex = getVideoTranscriptEntryIndex(videoTranscript, offsetItem);

            List<OffsetItem> currentList = results.get(videoTranscriptEntryIndex);
            if (currentList == null) {
                currentList = new ArrayList<>();
                results.put(videoTranscriptEntryIndex, currentList);
            }
            currentList.add(offsetItem);
        }
        return results;
    }

    private static int getVideoTranscriptEntryIndex(VideoTranscript videoTranscript, OffsetItem offsetItem) {
        Integer videoTranscriptEntryIndex = null;
        VideoFrameInfo videoFrameInfo = VideoPropertyHelper.getVideoFrameInfo(offsetItem.getId());
        if (videoFrameInfo != null) {
            videoTranscriptEntryIndex = videoTranscript.findEntryIndexFromStartTime(videoFrameInfo.getFrameStartTime());
        }
        if (videoTranscriptEntryIndex == null) {
            videoTranscriptEntryIndex = offsetItem.getVideoTranscriptEntryIndex();
        }
        return videoTranscriptEntryIndex;
    }

    private static String safeSubstring(String text, int beginIndex) {
        beginIndex = Math.min(beginIndex, text.length());
        return text.substring(beginIndex);
    }

    private static String safeSubstring(String text, int beginIndex, int endIndex) {
        beginIndex = Math.min(beginIndex, text.length());
        endIndex = Math.min(endIndex, text.length());
        return text.substring(beginIndex, endIndex);
    }

    public List<OffsetItem> convertTermMentionsToOffsetItems(Iterable<Vertex> termMentions, String workspaceId, Authorizations authorizations) {
        ArrayList<OffsetItem> termMetadataOffsetItems = new ArrayList<>();
        for (Vertex termMention : termMentions) {
            String visibility = termMention.getVisibility().getVisibilityString();
            SandboxStatus sandboxStatus = GraphUtil.getSandboxStatusFromVisibilityString(visibility, workspaceId);
            termMetadataOffsetItems.add(new VertexOffsetItem(termMention, sandboxStatus, authorizations));
        }
        return termMetadataOffsetItems;
    }
}
