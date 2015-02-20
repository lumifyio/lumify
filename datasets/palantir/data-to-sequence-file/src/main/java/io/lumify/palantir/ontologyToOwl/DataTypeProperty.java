package io.lumify.palantir.ontologyToOwl;

import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DataTypeProperty implements OwlElement {
    private final Element datatypePropertyElement;
    private final Set<String> domainUris = new HashSet<>();
    private final List<Element> dependentPropertyElements = new ArrayList<>();
    private final String propertyIri;

    public DataTypeProperty(String propertyIri, Element datatypePropertyElement) {
        this.propertyIri = propertyIri;
        this.datatypePropertyElement = datatypePropertyElement;
    }

    public void addDomain(String domainUri) {
        this.domainUris.add(domainUri);
    }

    public Set<String> getDomainUris() {
        return domainUris;
    }

    public List<Element> getDependentPropertyElements() {
        return dependentPropertyElements;
    }

    public Element getElement() {
        return datatypePropertyElement;
    }

    public void addDependentPropertyElement(Element dependentPropertyElement) {
        this.dependentPropertyElements.add(dependentPropertyElement);
    }
}
