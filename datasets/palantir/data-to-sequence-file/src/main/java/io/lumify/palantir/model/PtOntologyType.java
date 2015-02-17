package io.lumify.palantir.model;

import io.lumify.core.exception.LumifyException;
import io.lumify.palantir.util.XmlUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public abstract class PtOntologyType extends PtModelBase {
    private static final DocumentBuilder dBuilder;
    private String config;

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
    }

    @Override
    public void write(DataOutput out) throws IOException {
        out.writeBytes(getConfig());
    }

    @Override
    public void readFields(DataInput in) throws IOException {
        setConfig(in.readUTF());
    }

    public String getUri() {
        try {
            Document doc = dBuilder.parse(new ByteArrayInputStream(config.getBytes()));
            return parseUriFromXml(doc);
        } catch (Exception e) {
            throw new RuntimeException("Could not get uri", e);
        }
    }

    private String parseUriFromXml(Document doc) {
        Element uriElement = XmlUtil.getChildByTagName(doc.getDocumentElement(), "uri");
        if (uriElement == null) {
            throw new LumifyException("Could not find uri");
        }

        return uriElement.getTextContent();
    }
}
