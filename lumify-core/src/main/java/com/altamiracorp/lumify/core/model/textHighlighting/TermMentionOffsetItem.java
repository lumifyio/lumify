package com.altamiracorp.lumify.core.model.textHighlighting;

import com.altamiracorp.lumify.core.model.ontology.OntologyRepository;
import com.altamiracorp.lumify.core.model.termMention.TermMentionModel;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class TermMentionOffsetItem extends OffsetItem {
    private final TermMentionModel termMention;

    public TermMentionOffsetItem(TermMentionModel termMention) {
        this.termMention = termMention;
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
        return OntologyRepository.ENTITY_CONCEPT_IRI;
    }

    public String getConceptGraphVertexId() {
        return termMention.getMetadata().getConceptGraphVertexId();
    }

    @Override
    public String getRowKey() {
        return termMention.getRowKey().toString();
    }

    @Override
    public String getGraphVertexId() {
        return termMention.getMetadata().getGraphVertexId();
    }

    @Override
    public String getEdgeId() {
        return termMention.getMetadata().getEdgeId();
    }

    public String getTitle() {
        return termMention.getMetadata().getSign();
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
                infoJson.put("http://lumify.io#conceptType", getConceptGraphVertexId());
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
        if (getGraphVertexId() != null && !getGraphVertexId().equals("")) {
            classes.add("resolved");
        }
        return classes;
    }
}
