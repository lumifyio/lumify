package io.lumify.analystsNotebook.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.lumify.analystsNotebook.AnalystsNotebookImageUtil;
import io.lumify.core.model.ontology.Concept;
import io.lumify.core.model.ontology.OntologyRepository;
import io.lumify.core.model.properties.LumifyProperties;
import org.securegraph.Vertex;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomImage {
    @JacksonXmlProperty(isAttribute = true)
    private String id;

    @JacksonXmlProperty(isAttribute = true)
    private String data;

    @JacksonXmlProperty(isAttribute = true)
    private int dataLength;

    public CustomImage() {

    }

    public CustomImage(String id, byte[] data) {
        this.id = id;
        data = AnalystsNotebookImageUtil.convertImageFormat(data);
        this.data = AnalystsNotebookImageUtil.base64EncodedImageBytes(data);
        dataLength = data.length;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public int getDataLength() {
        return dataLength;
    }

    public void setDataLength(int dataLength) {
        this.dataLength = dataLength;
    }

    public static List<CustomImage> createForVertices(Iterable<Vertex> vertices, OntologyRepository ontologyRepository) {
        Map<String, CustomImage> customImageMap = new HashMap<String, CustomImage>();
        for (Vertex vertex : vertices) {
            String conceptType = LumifyProperties.CONCEPT_TYPE.getPropertyValue(vertex);
            if (!customImageMap.containsKey(conceptType)) {
                CustomImage customImage = null;
                Concept concept = ontologyRepository.getConceptByIRI(conceptType);
                byte[] glyphIcon = getGlyphIcon(concept, ontologyRepository);
                if (glyphIcon != null) {
                    customImage = new CustomImage(conceptType, glyphIcon);
                }
                customImageMap.put(conceptType, customImage);
            }
        }

        List<CustomImage> customImages = new ArrayList<CustomImage>();
        for (CustomImage customImage : customImageMap.values()) {
            if (customImage != null) {
                customImages.add(customImage);
            }
        }
        return customImages;
    }

    private static byte[] getGlyphIcon(Concept concept, OntologyRepository ontologyRepository) {
        if (concept.hasGlyphIconResource()) {
            return concept.getGlyphIcon();
        } else {
            concept = ontologyRepository.getParentConcept(concept);
            if (concept != null) {
                return getGlyphIcon(concept, ontologyRepository);
            } else {
                return null;
            }
        }
    }
}
