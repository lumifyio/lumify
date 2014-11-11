package io.lumify.analystsNotebook.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.lumify.core.model.ontology.OntologyProperty;
import io.lumify.core.model.ontology.OntologyRepository;
import org.securegraph.Property;
import org.securegraph.Vertex;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Attribute {
    @JacksonXmlProperty(isAttribute = true)
    private String attributeClass;

    @JacksonXmlProperty(isAttribute = true)
    private String value;

    public Attribute() {

    }

    public Attribute(String attributeClass, String value) {
        this.attributeClass = attributeClass;
        this.value = value;
    }

    public String getAttributeClass() {
        return attributeClass;
    }

    public void setAttributeClass(String attributeClass) {
        this.attributeClass = attributeClass;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public static List<Attribute> createCollectionFromVertex(Vertex vertex, OntologyRepository ontologyRepository) {
        List<Attribute> collection = new ArrayList<Attribute>();
        for (Property property : vertex.getProperties()) {
            OntologyProperty ontologyProperty = ontologyRepository.getPropertyByIRI(property.getName());
            if (ontologyProperty.getUserVisible()) {
                String name = ontologyProperty.getTitle();
                Map<String, String> possibleValues = ontologyProperty.getPossibleValues();
                String value = property.getValue().toString();
                if (possibleValues != null && possibleValues.size() > 0 && possibleValues.containsKey(value)) {
                    value = possibleValues.get(value);
                }
                Attribute attribute = new Attribute(name, value);
                collection.add(attribute);
            }
        }
        return collection;
    }
}
