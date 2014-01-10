package com.altamiracorp.lumify.core.model.artifactHighlighting;

import com.altamiracorp.lumify.core.model.ontology.OntologyRepository;
import com.altamiracorp.lumify.core.model.ontology.PropertyName;
import com.altamiracorp.lumify.core.model.termMention.TermMention;
import com.altamiracorp.securegraph.Vertex;
import com.altamiracorp.securegraph.type.GeoPoint;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class TermMentionOffsetItem extends OffsetItem {

    private final TermMention termMention;
    private final Vertex graphVertex;

    public TermMentionOffsetItem(TermMention termMention, Vertex graphVertex) {
        this.termMention = termMention;
        this.graphVertex = graphVertex;
    }

    @Override
    public long getStart() {
        return termMention.getRowKey().getStartOffset();
    }

    @Override
    public long getEnd() {
        return termMention.getRowKey().getEndOffset();
    }

    @Override
    public String getType() {
        return OntologyRepository.TYPE_ENTITY.toString();
    }

    public String getConceptGraphVertexId() {
        return termMention.getMetadata().getConceptGraphVertexId();
    }

    @Override
    public String getRowKey() {
        return termMention.getRowKey().toString();
    }

    @Override
    public String getGlyphIcon() {
        if (graphVertex == null) {
            return null;
        }
        return (String) graphVertex.getPropertyValue(PropertyName.GLYPH_ICON.toString(), 0);
    }

    @Override
    public String getGraphVertexId() {
        return termMention.getMetadata().getGraphVertexId();
    }

    public String getTitle() {
        return termMention.getMetadata().getSign();
    }


    private Double getLatitude() {
        if (graphVertex == null || graphVertex.getPropertyValue(PropertyName.GEO_LOCATION.toString(), 0) == null) {
            return null;
        }
        return ((GeoPoint) graphVertex.getPropertyValue(PropertyName.GEO_LOCATION.toString(), 0)).getLatitude();
    }

    private Double getLongitude() {
        if (graphVertex == null || graphVertex.getPropertyValue(PropertyName.GEO_LOCATION.toString(), 0) == null) {
            return null;
        }
        return ((GeoPoint) graphVertex.getPropertyValue(PropertyName.GEO_LOCATION.toString(), 0)).getLongitude();
    }

    @Override
    public boolean shouldHighlight() {
        if (!super.shouldHighlight()) {
            return false;
        }
        return true;
    }

    @Override
    public JSONObject getInfoJson() {
        try {
            JSONObject infoJson = super.getInfoJson();
            infoJson.put("title", getTitle());
            infoJson.put("start", getStart());
            infoJson.put("end", getEnd());
            if (getConceptGraphVertexId() != null) {
                infoJson.put("_conceptType", getConceptGraphVertexId());
            }
            if (getLongitude() != null && getLatitude() != null) {
                infoJson.put("longitude", getLongitude());
                infoJson.put("latitude", getLatitude());
            }
            return infoJson;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<String> getCssClasses() {
        List<String> classes = new ArrayList<String>();
        classes.add("entity");
        if (getGraphVertexId() != null) {
            classes.add("resolved");
        }
        if (getConceptGraphVertexId() != null) {
            classes.add("conceptType-" + getConceptGraphVertexId());
        }
        return classes;
    }
}
