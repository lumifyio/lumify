package io.lumify.core.model.textHighlighting;

import io.lumify.core.model.ontology.OntologyRepository;
import io.lumify.core.model.properties.LumifyProperties;
import io.lumify.core.model.termMention.TermMentionFor;
import io.lumify.core.model.termMention.TermMentionRepository;
import io.lumify.web.clientapi.model.SandboxStatus;
import org.json.JSONException;
import org.json.JSONObject;
import org.securegraph.Authorizations;
import org.securegraph.Direction;
import org.securegraph.Vertex;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static org.securegraph.util.IterableUtils.singleOrDefault;

public class VertexOffsetItem extends OffsetItem {
    private final Vertex termMention;
    private final SandboxStatus sandboxStatus;
    private final Authorizations authorizations;

    public VertexOffsetItem(Vertex termMention, SandboxStatus sandboxStatus, Authorizations authorizations) {
        this.termMention = termMention;
        this.sandboxStatus = sandboxStatus;
        this.authorizations = authorizations;

        String[] authArray = this.authorizations.getAuthorizations();
        boolean hasTermMentionAuth = false;
        for (String auth : authArray) {
            if (TermMentionRepository.VISIBILITY_STRING.equals(auth)) {
                hasTermMentionAuth = true;
            }
        }
        checkArgument(hasTermMentionAuth, TermMentionRepository.VISIBILITY_STRING + " is a required auth");
    }

    @Override
    public long getStart() {
        return LumifyProperties.TERM_MENTION_START_OFFSET.getPropertyValue(termMention, 0);
    }

    @Override
    public long getEnd() {
        return LumifyProperties.TERM_MENTION_END_OFFSET.getPropertyValue(termMention, 0);
    }

    @Override
    public String getType() {
        return OntologyRepository.ENTITY_CONCEPT_IRI;
    }

    public String getConceptIri() {
        return LumifyProperties.TERM_MENTION_CONCEPT_TYPE.getPropertyValue(termMention);
    }

    @Override
    public String getId() {
        return termMention.getId();
    }

    @Override
    public String getProcess() {
        return LumifyProperties.TERM_MENTION_PROCESS.getPropertyValue(termMention);
    }

    @Override
    public String getSourceVertexId() {
        return singleOrDefault(termMention.getVertexIds(Direction.IN, LumifyProperties.TERM_MENTION_LABEL_HAS_TERM_MENTION, this.authorizations), null);
    }

    @Override
    public String getResolvedToVertexId() {
        return singleOrDefault(termMention.getVertexIds(Direction.OUT, LumifyProperties.TERM_MENTION_LABEL_RESOLVED_TO, this.authorizations), null);
    }

    @Override
    public String getResolvedToEdgeId() {
        return LumifyProperties.TERM_MENTION_RESOLVED_EDGE_ID.getPropertyValue(termMention);
    }

    @Override
    public TermMentionFor getTermMentionFor() {
        return LumifyProperties.TERM_MENTION_FOR_TYPE.getPropertyValue(termMention);
    }

    @Override
    public SandboxStatus getSandboxStatus() {
        return sandboxStatus;
    }

    public String getTitle() {
        return LumifyProperties.TERM_MENTION_TITLE.getPropertyValue(termMention);
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
            if (getConceptIri() != null) {
                infoJson.put("http://lumify.io#conceptType", getConceptIri());
            }
            return infoJson;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<String> getCssClasses() {
        List<String> classes = new ArrayList<>();
        TermMentionFor termMentionFor = getTermMentionFor();
        if (termMentionFor == null) {
            termMentionFor = TermMentionFor.VERTEX;
        }
        classes.add(termMentionFor.toString().toLowerCase());
        if (getResolvedToVertexId() != null) {
            classes.add("resolved");
        }
        return classes;
    }
}
