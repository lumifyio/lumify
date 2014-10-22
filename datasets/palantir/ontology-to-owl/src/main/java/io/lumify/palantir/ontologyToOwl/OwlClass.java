package io.lumify.palantir.ontologyToOwl;

import org.w3c.dom.Element;

public class OwlClass implements OwlElement {
    private final Element classElement;

    public OwlClass(Element classElement) {
        this.classElement = classElement;
    }

    public Element getElement() {
        return classElement;
    }
}
