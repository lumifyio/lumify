package io.lumify.palantir.model;

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
    private transient Document configXml;

    static {
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            dBuilder = dbFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException("Could not create document builder", e);
        }
    }

    public String getConfig() {
        return config;
    }

    public void setConfig(String config) {
        this.config = config;
    }

    public Document getConfigXml() {
        if (configXml != null) {
            return configXml;
        }
        try {
            return configXml = dBuilder.parse(new ByteArrayInputStream(getConfig().getBytes()));
        } catch (Exception e) {
            throw new RuntimeException("Could not parse config: " + getConfig(), e);
        }
    }

    @Override
    public void write(DataOutput out) throws IOException {
        byte[] configBytes = getConfig().getBytes();
        out.writeInt(configBytes.length);
        out.write(configBytes);
    }

    @Override
    public void readFields(DataInput in) throws IOException {
        int configBytesLen = in.readInt();
        byte[] configBytes = new byte[configBytesLen];
        in.readFully(configBytes, 0, configBytesLen);
        setConfig(new String(configBytes));
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
            throw new RuntimeException("Could not find uri");
        }

        return uriElement.getTextContent();
    }
}
