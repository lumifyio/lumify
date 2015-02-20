package io.lumify.palantir.util;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.util.ArrayList;
import java.util.List;

public class XmlUtil {
    private static XPath xPath;
    private static NamespaceContext namespaceContext;

    static {
        XPathFactory xPathfactory = XPathFactory.newInstance();
        xPath = xPathfactory.newXPath();
        namespaceContext = new OwlNamespaceContext();
        xPath.setNamespaceContext(namespaceContext);
    }

    public static Element getChildByTagName(Element element, String tagName) {
        Element result = null;
        NodeList childNodes = element.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node n = childNodes.item(i);
            if (!(n instanceof Element)) {
                continue;
            }
            if (((Element) n).getTagName().equals(tagName)) {
                if (result != null) {
                    throw new RuntimeException("Too many elements with tag name " + tagName);
                }
                result = (Element) n;
            }
        }
        return result;
    }

    public static String getXmlString(Node inXml, String xpath) {
        if (!xpath.contains("/")) {
            NodeList elements = ((Element) inXml).getElementsByTagName(xpath);
            if (elements.getLength() == 1) {
                return elements.item(0).getTextContent();
            }
        }

        try {
            return xPath.evaluate(xpath, inXml);
        } catch (Exception ex) {
            throw new RuntimeException("Could not run xpath: " + xpath, ex);
        }
    }

    public static List<Element> getXmlElements(Document inXml, String xpath) {
        try {
            NodeList nodeList = (NodeList) xPath.evaluate(xpath, inXml, XPathConstants.NODESET);
            List<Element> results = new ArrayList<>();
            for (int i = 0; i < nodeList.getLength(); i++) {
                results.add((Element) nodeList.item(i));
            }
            return results;
        } catch (Exception ex) {
            throw new RuntimeException("Could not run xpath: " + xpath, ex);
        }
    }

    public static NamespaceContext getNamespaceContext() {
        return namespaceContext;
    }
}
