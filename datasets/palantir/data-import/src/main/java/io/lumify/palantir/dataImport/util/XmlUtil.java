package io.lumify.palantir.dataImport.util;

import io.lumify.core.exception.LumifyException;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class XmlUtil {
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
                    throw new LumifyException("Too many elements with tag name " + tagName);
                }
                result = (Element) n;
            }
        }
        return result;
    }
}
