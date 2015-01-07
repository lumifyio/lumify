package io.lumify.palantir.ontologyToOwl;

import javax.xml.namespace.NamespaceContext;
import java.util.Iterator;

public class OwlNamespaceContext implements NamespaceContext {
    public static final String XML_NS_RDF = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
    public static final String XML_NS_OWL = "http://www.w3.org/2002/07/owl#";
    public static final String XML_NS_RDFS = "http://www.w3.org/2000/01/rdf-schema#";
    public static final String XML_NS_LUMIFY = "http://lumify.io#";
    public static final String XML_NS_XML = "http://www.w3.org/XML/1998/namespace";

    @Override
    public String getNamespaceURI(String prefix) {
        if ("rdf".equals(prefix)) {
            return XML_NS_RDF;
        }
        if ("rdfs".equals(prefix)) {
            return XML_NS_RDFS;
        }
        if ("owl".equals(prefix)) {
            return XML_NS_OWL;
        }
        if ("lumify".equals(prefix)) {
            return XML_NS_LUMIFY;
        }
        if ("xml".equals(prefix)) {
            return XML_NS_XML;
        }
        return null;
    }

    @Override
    public String getPrefix(String namespaceURI) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterator getPrefixes(String namespaceURI) {
        throw new UnsupportedOperationException();
    }
}
