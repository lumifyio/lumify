package io.lumify.analystsNotebook.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.lumify.analystsNotebook.AnalystsNotebookVersion;
import io.lumify.core.model.ontology.Concept;
import io.lumify.core.model.ontology.OntologyRepository;
import io.lumify.core.model.properties.LumifyProperties;
import org.securegraph.Vertex;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EntityType {
    public static final String ONTOLOGY_CONCEPT_METADATA_KEY_SUFFIX = "#iconFile";

    @JacksonXmlProperty(isAttribute = true)
    private String name;

    @JacksonXmlProperty(isAttribute = true)
    private String iconFile;

    public EntityType() {

    }

    public EntityType(String name, String iconFile) {
        this.name = name;
        this.iconFile = iconFile;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIconFile() {
        return iconFile;
    }

    public void setIconFile(String iconFile) {
        this.iconFile = iconFile;
    }

    public static List<EntityType> createForVertices(Iterable<Vertex> vertices, OntologyRepository ontologyRepository, AnalystsNotebookVersion version) {
        Map<String, String> conceptTypeIconFileMap = new HashMap<String, String>();
        for (Vertex vertex : vertices) {
            String conceptType = LumifyProperties.CONCEPT_TYPE.getPropertyValue(vertex);
            if (!conceptTypeIconFileMap.containsKey(conceptType)) {
                Concept concept = ontologyRepository.getConceptByIRI(conceptType);
                String iconFile = getMetadataIconFile(concept, ontologyRepository, version);
                if (iconFile == null) {
                    iconFile = version.getDefaultIconFile();
                }
                conceptTypeIconFileMap.put(conceptType, iconFile);
            }
        }

        List<EntityType> entityTypes = new ArrayList<EntityType>();
        for (Map.Entry<String, String> entry : conceptTypeIconFileMap.entrySet()) {
            entityTypes.add(new EntityType(entry.getKey(), entry.getValue()));
        }
        return entityTypes;
    }

    private static String getMetadataIconFile(Concept concept, OntologyRepository ontologyRepository, AnalystsNotebookVersion version) {
        Map<String, String> metadata = concept.getMetadata();
        String ontologyConceptMetadataIconFileKey = version.getOntologyConceptMetadataKeyPrefix() + ONTOLOGY_CONCEPT_METADATA_KEY_SUFFIX;
        if (metadata.containsKey(ontologyConceptMetadataIconFileKey)) {
            return metadata.get(ontologyConceptMetadataIconFileKey);
        } else {
            concept = ontologyRepository.getParentConcept(concept);
            if (concept != null) {
                return getMetadataIconFile(concept, ontologyRepository, version);
            } else {
                return null;
            }
        }
    }
}
