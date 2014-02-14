package com.altamiracorp.lumify.core.model.textHighlighting;

import static com.altamiracorp.lumify.core.model.properties.EntityLumifyProperties.GEO_LOCATION;
import static com.altamiracorp.lumify.core.model.properties.LumifyProperties.GLYPH_ICON;

import com.altamiracorp.lumify.core.model.ontology.OntologyRepository;
import com.altamiracorp.lumify.core.model.termMention.TermMentionModel;
import com.altamiracorp.securegraph.Vertex;
import com.altamiracorp.securegraph.type.GeoPoint;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONException;
import org.json.JSONObject;

public class TermMentionOffsetItem extends OffsetItem {

    private final TermMentionModel termMention;
    private final Vertex graphVertex;

    public TermMentionOffsetItem(TermMentionModel termMention, Vertex graphVertex) {
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
        return OntologyRepository.TYPE_ENTITY;
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
        return (String) graphVertex.getPropertyValue(GLYPH_ICON.getKey());
    }

    @Override
    public String getGraphVertexId() {
        return termMention.getMetadata().getGraphVertexId();
    }

    public String getTitle() {
        return termMention.getMetadata().getSign();
    }


    private Double getLatitude() {
        GeoPoint point = GEO_LOCATION.getPropertyValue(graphVertex);
        return point != null ? point.getLatitude() : null;
    }

    private Double getLongitude() {
        GeoPoint point = GEO_LOCATION.getPropertyValue(graphVertex);
        return point != null ? point.getLongitude() : null;
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
