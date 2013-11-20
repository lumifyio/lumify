package com.altamiracorp.lumify.textExtraction;

import info.bliki.wiki.filter.PlainTextConverter;
import info.bliki.wiki.model.WikiModel;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.AbstractParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.XHTMLContentHandler;
import org.w3c.dom.Document;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.Set;

public class WikipediaPageXmlTikaParser extends AbstractParser {
    private static final Set<MediaType> SUPPORTED_TYPES = Collections.singleton(MediaType.application("wikipedia.page+xml"));
    private static final String MIME_TYPE = "application/wikipedia.page+xml";
    private DocumentBuilder documentBuilder;
    private XPathExpression textXPath;
    private XPathExpression titleXPath;

    public WikipediaPageXmlTikaParser() throws XPathExpressionException, ParserConfigurationException {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilder = documentBuilderFactory.newDocumentBuilder();

        XPathFactory xPathfactory = XPathFactory.newInstance();
        XPath xpath = xPathfactory.newXPath();
        textXPath = xpath.compile("/page/revision/text/text()");
        titleXPath = xpath.compile("/page/title/text()");
    }

    @Override
    public Set<MediaType> getSupportedTypes(ParseContext context) {
        return SUPPORTED_TYPES;
    }

    @Override
    public void parse(InputStream stream, ContentHandler handler, Metadata metadata, ParseContext context) throws IOException, SAXException, TikaException {
        try {
            Document pageXml = documentBuilder.parse(stream);
            String title = (String) titleXPath.evaluate(pageXml, XPathConstants.STRING);
            String wikiMarkup = (String) textXPath.evaluate(pageXml, XPathConstants.STRING);

            WikiModel wikiModel = new WikiModel("http://en.wikipedia.org/wiki/${image}", "http://en.wikipedia.org/wiki/${title}");
            String plainStr = wikiModel.render(new PlainTextConverter(), wikiMarkup);

            metadata.set(Metadata.CONTENT_TYPE, MIME_TYPE);
            metadata.set(TikaCoreProperties.TITLE, title);
            String extUrl = getExtUrl(title);
            metadata.set("url", extUrl);
            metadata.set("atc:result-url", extUrl);

            XHTMLContentHandler xhtml = new XHTMLContentHandler(handler, metadata);
            xhtml.startDocument();
            xhtml.startElement("p");
            xhtml.characters(plainStr);
            xhtml.endElement("p");
            xhtml.endDocument();
        } catch (UnsupportedEncodingException e) {
            throw new TikaException("", e);
        } catch (XPathExpressionException e) {
            throw new TikaException("", e);
        }
    }

    private String getExtUrl(String title) throws UnsupportedEncodingException {
        title = title.replace(' ', '_');
        title = URLEncoder.encode(title, "UTF-8");
        return "http://en.wikipedia.org/wiki/" + title;
    }
}
