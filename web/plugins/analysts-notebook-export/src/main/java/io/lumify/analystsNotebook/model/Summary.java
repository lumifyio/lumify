package io.lumify.analystsNotebook.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import java.util.List;

public class Summary {
    @JacksonXmlElementWrapper(localName = "CustomPropertyCollection")
    @JacksonXmlProperty(localName = "CustomProperty")
    private List<CustomProperty> customPropertyCollection;

    public List<CustomProperty> getCustomPropertyCollection() {
        return customPropertyCollection;
    }

    public void setCustomPropertyCollection(List<CustomProperty> customPropertyCollection) {
        this.customPropertyCollection = customPropertyCollection;
    }
}
