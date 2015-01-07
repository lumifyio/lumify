package io.lumify.palantir.dataImport.model;

import io.lumify.core.exception.LumifyException;
import io.lumify.palantir.dataImport.util.XmlUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;

public abstract class PtOntologyType {
    private static final DocumentBuilder dBuilder;
    private String config;
    private String uri;
    private DisplayFormula displayFormula;

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

            Element displayElement = XmlUtil.getChildByTagName(doc.getDocumentElement(), "display");
            this.displayFormula = new DisplayFormula(displayElement);
        } catch (Exception e) {
            throw new LumifyException("Could not parse config: " + config, e);
        }
    }

    public String getUri() {
        return this.uri;
    }

    public DisplayFormula getDisplayFormula() {
        return displayFormula;
    }

    private String parseUriFromXml(Document doc) {
        Element uriElement = XmlUtil.getChildByTagName(doc.getDocumentElement(), "uri");
        if (uriElement == null) {
            throw new LumifyException("Could not find uri");
        }

        return uriElement.getTextContent();
    }
}
