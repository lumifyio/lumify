package io.lumify.core.model.textHighlighting;

import io.lumify.web.clientapi.model.SandboxStatus;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public abstract class OffsetItem implements Comparable {
    public static final int VIDEO_TRANSCRIPT_INDEX_BITS = 12; // duplicated in io.lumify.web.clientapi.codegen.EntityApiExt
    public static final int VIDEO_TRANSCRIPT_OFFSET_BITS = 20; // duplicated in io.lumify.web.clientapi.codegen.EntityApiExt

    public abstract long getStart();

    public int getVideoTranscriptEntryIndex() {
        return (int) (getStart() >> VIDEO_TRANSCRIPT_OFFSET_BITS);
    }

    public abstract long getEnd();

    public abstract String getType();

    public abstract String getId();

    public abstract String getProcess();

    public String getSourceVertexId() {
        return null;
    }

    public String getResolvedToVertexId() {
        return null;
    }

    public String getResolvedToEdgeId() {
        return null;
    }

    public abstract SandboxStatus getSandboxStatus();

    public JSONObject getInfoJson() {
        try {
            JSONObject infoJson = new JSONObject();
            infoJson.put("id", getId());
            infoJson.put("start", getStart());
            infoJson.put("end", getEnd());
            infoJson.put("sourceVertexId", getSourceVertexId());
            infoJson.put("sandboxStatus", getSandboxStatus().toString());
            if (getResolvedToVertexId() != null) {
                infoJson.put("resolvedToVertexId", getResolvedToVertexId());
            }
            if (getResolvedToEdgeId() != null) {
                infoJson.put("resolvedToEdgeId", getResolvedToEdgeId());
            }
            infoJson.put("type", getType());
            infoJson.put("process", getProcess());
            return infoJson;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public List<String> getCssClasses() {
        ArrayList<String> classes = new ArrayList<String>();
        if (getResolvedToVertexId() != null) {
            classes.add("resolved");
        }
        return classes;
    }

    public JSONObject toJson() {
        try {
            JSONObject json = new JSONObject();
            json.put("info", getInfoJson());

            JSONArray cssClasses = new JSONArray();
            for (String cssClass : getCssClasses()) {
                cssClasses.put(cssClass);
            }
            json.put("cssClasses", cssClasses);
            return json;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean shouldHighlight() {
        return true;
    }

    public String getTitle() {
        return null;
    }

    @Override
    public String toString() {
        return "id: " + getId() + ", start: " + getStart() + ", end: " + getEnd() + ", title: " + getTitle();
    }

    @Override
    public int compareTo(Object o) {
        if (!(o instanceof OffsetItem)) {
            return -1;
        }

        OffsetItem other = (OffsetItem) o;

        if (getOffset(getStart()) != getOffset(other.getStart())) {
            return getOffset(getStart()) < getOffset(other.getStart()) ? -1 : 1;
        }

        if (getOffset(getEnd()) != getOffset(other.getEnd())) {
            return getOffset(getEnd()) < getOffset(other.getEnd()) ? -1 : 1;
        }

        if (getResolvedToVertexId() == null && other.getResolvedToVertexId() == null) {
            return 0;
        }

        if (getResolvedToVertexId() == null) {
            return 1;
        }

        if (other.getResolvedToVertexId() == null) {
            return -1;
        }

        return getResolvedToVertexId().compareTo(other.getResolvedToVertexId());
    }

    public static long getOffset(long offset) {
        return offset & ((2 << (OffsetItem.VIDEO_TRANSCRIPT_OFFSET_BITS - 1)) - 1L);
    }
}
