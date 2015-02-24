package io.lumify.palantir.ontologyToOwl;

import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DataTypeProperty implements OwlElement {
    private final Element datatypePropertyElement;
    private final Set<String> domainUris = new HashSet<>();
    private final List<Element> relatedPropertyElements = new ArrayList<>();
    private final String typeGroupUri;

    @SuppressWarnings({"UnusedDeclaration", "FieldCanBeLocal"}) // this field is here for debugging
    private final String propertyIri;

    public DataTypeProperty(String propertyIri, Element datatypePropertyElement, String typeGroupUri) {
        this.propertyIri = propertyIri;
        this.datatypePropertyElement = datatypePropertyElement;
        if (typeGroupUri != null) {
            typeGroupUri = typeGroupUri.trim();
            if (typeGroupUri.length() == 0) {
                typeGroupUri = null;
            }
        }
        this.typeGroupUri = typeGroupUri;
    }

    public void addDomain(String domainUri) {
        this.domainUris.add(domainUri);
    }

    public Set<String> getDomainUris() {
        return domainUris;
    }

    public List<Element> getRelatedPropertyElements() {
        return relatedPropertyElements;
    }

    public Element getElement() {
        return datatypePropertyElement;
    }

    public String getTypeGroupUri() {
        return typeGroupUri;
    }

    public void addRelatedPropertyElement(Element dependentPropertyElement) {
        this.relatedPropertyElements.add(dependentPropertyElement);
    }
}
