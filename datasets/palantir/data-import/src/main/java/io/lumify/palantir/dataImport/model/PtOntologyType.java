package io.lumify.palantir.dataImport.model;

import io.lumify.core.exception.LumifyException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

public abstract class PtOntologyType {
    private static final DocumentBuilder dBuilder;
    private String config;
    private String uri;
    private List<String> displayFormulas;

    static {
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            dBuilder = dbFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new LumifyException("Could not create document builder", e);
        }
    }

    public String getConfig() {
        return config;
    }

    public void setConfig(String config) {
        this.config = config;
        try {
            Document doc = dBuilder.parse(new ByteArrayInputStream(config.getBytes()));
            this.uri = parseUriFromXml(doc);
            this.displayFormulas = parseDisplayFormulasFromXml(doc);
        } catch (Exception e) {
            throw new LumifyException("Could not parse config: " + config, e);
        }
    }

    public String getUri() {
        return this.uri;
    }

    public List<String> getDisplayFormulas() {
        return displayFormulas;
    }

    private List<String> parseDisplayFormulasFromXml(Document doc) {
        ArrayList<String> results = new ArrayList<String>();
        Element displayElement = getChildByTagName(doc.getDocumentElement(), "display");
        if (displayElement == null) {
            return results;
        }

        Element argsElement = getChildByTagName(displayElement, "args");
        if (argsElement == null) {
            return results;
        }

        NodeList argElements = argsElement.getChildNodes();
        for (int i = 0; i < argElements.getLength(); i++) {
            Node argElement = argElements.item(i);
            if (!(argElement instanceof Element)) {
                continue;
            }
            if (!((Element) argElement).getTagName().equals("arg")) {
                continue;
            }
            String arg = argElement.getTextContent();
            if (arg.startsWith("prettyprint=")) {
                continue;
            }
            if (arg.startsWith("tokens=")) {
                results.add(arg.substring("tokens=".length()));
                continue;
            }
            throw new LumifyException("Could not parse arg formula " + arg);
        }

        return results;
    }

    private String parseUriFromXml(Document doc) {
        Element uriElement = getChildByTagName(doc.getDocumentElement(), "uri");
        if (uriElement == null) {
            throw new LumifyException("Could not find uri");
        }

        return uriElement.getTextContent();
    }

    private Element getChildByTagName(Element element, String tagName) {
        Element result = null;
        NodeList childNodes = element.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node n = childNodes.item(i);
            if (!(n instanceof Element)) {
                continue;
            }
            if (((Element) n).getTagName().equals(tagName)) {
                if (result != null) {
                    throw new LumifyException("Too many elements with tag name " + tagName);
                }
                result = (Element) n;
            }
        }
        return result;
    }
}
