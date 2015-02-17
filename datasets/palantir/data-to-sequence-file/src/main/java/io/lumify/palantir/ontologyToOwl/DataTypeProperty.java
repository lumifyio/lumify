package io.lumify.palantir.ontologyToOwl;

import org.w3c.dom.Element;

import java.util.HashSet;
import java.util.Set;

public class DataTypeProperty implements OwlElement {
    private final Element datatypePropertyElement;
    private final Set<String> domainUris = new HashSet<String>();

    public DataTypeProperty(Element datatypePropertyElement) {
        this.datatypePropertyElement = datatypePropertyElement;
    }

    public void addDomain(String domainUri) {
        this.domainUris.add(domainUri);
    }

    public Set<String> getDomainUris() {
        return domainUris;
    }

    public Element getElement() {
        return datatypePropertyElement;
    }
}
