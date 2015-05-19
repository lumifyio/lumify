package io.lumify.mapping.column;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.lumify.core.model.ontology.Concept;
import io.lumify.core.model.ontology.OntologyRepository;
import io.lumify.core.model.properties.LumifyProperties;
import io.lumify.mapping.column.AbstractColumnDocumentMapping.Row;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.securegraph.Vertex;

/**
 * A relationship mapping whose label is dependent on the concepts of
 * the source and/or target entities.
 */
@JsonTypeName("conceptMapped")
@JsonPropertyOrder({ "source", "target", "labelMappings" })
public class ConceptMappedColumnRelationshipMapping extends AbstractColumnRelationshipMapping {
    /**
     * The ontology repository.
     */
    @JacksonInject
    private OntologyRepository ontologyRepository;

    /**
     * The concept mappings.
     */
    private final List<ConceptMapping> labelMappings;

    /**
     * The concept->relationship label mapping for inputs only
     * requiring a source concept.
     */
    private final Map<String, String> sourceMap;

    /**
     * The concept->relationship label mapping for inputs only
     * requiring a target concept.
     */
    private final Map<String, String> targetMap;

    /**
     * The concept->relationship label mapping for inputs requiring
     * both source and target concepts.
     */
    private final Map<String, Map<String, String>> sourceTargetMap;

    /**
     * Create a new EntityConceptMappedCsvRelationshipMapping.
     * @param srcKey the source entity key
     * @param tgtKey the target entity key
     * @param lblMaps the list of source and/or target concepts and the relationship labels they map to
     */
    @JsonCreator
    public ConceptMappedColumnRelationshipMapping(@JsonProperty("source") final String srcKey,
            @JsonProperty("target") final String tgtKey,
            @JsonProperty("labelMappings") final List<ConceptMapping> lblMaps) {
        super(srcKey, tgtKey);
        checkNotNull(lblMaps, "Label mappings must be provided.");
        checkArgument(!lblMaps.isEmpty(), "Label mappings must contain at least one entry.");

        this.labelMappings = Collections.unmodifiableList(new ArrayList<>(lblMaps));

        Map<String, String> srcMap = new HashMap<>();
        Map<String, String> tgtMap = new HashMap<>();
        Map<String, Map<String, String>> srcTgtMap = new HashMap<>();

        String sCon;
        String tCon;
        Map<String, String> subMap;
        for (ConceptMapping mapping : this.labelMappings) {
            sCon = mapping.getSourceConcept();
            tCon = mapping.getTargetConcept();
            if (sCon != null && tCon != null) {
                subMap = srcTgtMap.get(sCon);
                if (subMap == null) {
                    subMap = new HashMap<>();
                    srcTgtMap.put(sCon, subMap);
                }
                subMap.put(tCon, mapping.getRelationshipLabel());
            } else if (sCon != null) {
                srcMap.put(sCon, mapping.getRelationshipLabel());
            } else {
                tgtMap.put(tCon, mapping.getRelationshipLabel());
            }
        }
        Map<String, Map<String, String>> immutableStMap = new HashMap<>();
        for (Map.Entry<String, Map<String, String>> entry : srcTgtMap.entrySet()) {
            immutableStMap.put(entry.getKey(), Collections.unmodifiableMap(entry.getValue()));
        }
        sourceMap = Collections.unmodifiableMap(srcMap);
        targetMap = Collections.unmodifiableMap(tgtMap);
        sourceTargetMap = Collections.unmodifiableMap(immutableStMap);
    }

    @JsonProperty("labelMappings")
    public final List<ConceptMapping> getLabelMappings() {
        return labelMappings;
    }

    @Override
    protected String getLabel(final Vertex source, final Vertex target, final Row row) {
        Concept srcConcept = null;
        Concept tgtConcept = null;
        if ((!sourceMap.isEmpty() || !sourceTargetMap.isEmpty()) && source != null) {
            srcConcept = ontologyRepository.getConceptByIRI(LumifyProperties.CONCEPT_TYPE.getPropertyValue(source));
        }
        if ((!targetMap.isEmpty() || !sourceTargetMap.isEmpty()) && target != null) {
            tgtConcept = ontologyRepository.getConceptByIRI(LumifyProperties.CONCEPT_TYPE.getPropertyValue(target));
        }

        // attempt to resolve both source and target first
        String label = findSourceTargetLabel(srcConcept, tgtConcept);
        // if unresolved, resolve source only defaults
        if (label == null) {
            label = findLabel(sourceMap, srcConcept);
        }
        // if still unresolved, resolve target only defaults
        if (label == null) {
            label = findLabel(targetMap, tgtConcept);
        }
        return label;
    }

    /**
     * Search for a label mapping for this concept in the provided map, traversing up
     * the ontological hierarchy until the root node is reached or a label
     * is found for the provided concept or one of its ancestors.
     * @param concept the concept to search
     * @return the configured relationship label or null if it cannot be found
     */
    private String findLabel(final Map<String, String> labelMap, final Concept concept) {
        String label = null;
        if (labelMap != null && !labelMap.isEmpty()) {
            for (Concept con = concept; label == null && con != null; con = ontologyRepository.getParentConcept(con)) {
                label = labelMap.get(con.getTitle());
            }
        }
        return label;
    }

    /**
     * Search for a label mapping for this concept using both the source and target
     * concepts, traversing ontological ancestry for source, then target according
     * to the following algorithm:
     * <code>
for [Source Concept .. Root Concept]; do
  for [Target Concept .. Root Concept]; do
    check source/target->label exists
  done
done
     * </code>
     * @param srcConcept the source concept
     * @param tgtConcept the target concept
     * @return the mapped label if any combination of source and target concepts in the ontological ancestry
     * of the provided concepts has a mapping configured
     */
    private String findSourceTargetLabel(final Concept srcConcept, final Concept tgtConcept) {
        String label = null;
        for (Concept src = srcConcept; label == null && src != null; src = ontologyRepository.getParentConcept(src)) {
            label = findLabel(sourceTargetMap.get(src.getTitle()), tgtConcept);
        }
        return label;
    }

    public void setOntologyRepository(final OntologyRepository repo) {
        ontologyRepository = repo;
    }

    /**
     * A mapping of source and/or target concepts to a particular relationship label.
     */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @JsonPropertyOrder({ "source", "target", "label" })
    public static final class ConceptMapping {
        /**
         * The source concept.
         */
        private final String sourceConcept;

        /**
         * The target concept.
         */
        private final String targetConcept;

        /**
         * The relationship label.
         */
        private final String relationshipLabel;

        /**
         * Create a new ConceptMapping.
         * @param source the source concept
         * @param target the target concept
         * @param label the relationship label
         */
        public ConceptMapping(@JsonProperty(value="source", required=false) final String source,
                @JsonProperty(value="target", required=false) final String target,
                @JsonProperty(value="label") final String label) {
            String resSource = source != null && !source.trim().isEmpty() ? source.trim() : null;
            String resTarget = target != null && !target.trim().isEmpty() ? target.trim() : null;
            checkArgument(resSource != null || resTarget != null, "At least one of source or target must be specified.");
            checkNotNull(label, "relationship label must be provided");
            checkArgument(!label.trim().isEmpty(), "relationship label must be provided");
            this.sourceConcept = resSource;
            this.targetConcept = resTarget;
            this.relationshipLabel = label.trim();
        }

        @JsonProperty("source")
        public String getSourceConcept() {
            return sourceConcept;
        }

        @JsonProperty("target")
        public String getTargetConcept() {
            return targetConcept;
        }

        @JsonProperty("label")
        public String getRelationshipLabel() {
            return relationshipLabel;
        }

        @Override
        public String toString() {
            return String.format("%s::%s => %s", sourceConcept, targetConcept, relationshipLabel);
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 71 * hash + (this.sourceConcept != null ? this.sourceConcept.hashCode() : 0);
            hash = 71 * hash + (this.targetConcept != null ? this.targetConcept.hashCode() : 0);
            hash = 71 * hash + (this.relationshipLabel != null ? this.relationshipLabel.hashCode() : 0);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final ConceptMapping other = (ConceptMapping) obj;
            if ((this.sourceConcept == null) ? (other.sourceConcept != null) : !this.sourceConcept.equals(other.sourceConcept)) {
                return false;
            }
            if ((this.targetConcept == null) ? (other.targetConcept != null) : !this.targetConcept.equals(other.targetConcept)) {
                return false;
            }
            if ((this.relationshipLabel == null) ? (other.relationshipLabel != null) : !this.relationshipLabel.equals(other.relationshipLabel)) {
                return false;
            }
            return true;
        }
    }
}
