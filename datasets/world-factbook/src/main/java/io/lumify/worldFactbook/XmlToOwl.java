package io.lumify.worldFactbook;

import io.lumify.core.cmdline.CommandLineBase;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.io.IOUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class XmlToOwl extends CommandLineBase {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(XmlToOwl.class);
    private static final String CMD_OPT_INPUT = "in";
    private static final String CMD_OPT_OUTPUT = "out";
    private static final String OWL_FILE_TEMPLATE_CONTENTS = "<!-- CONTENTS -->";
    private XPathExpression fieldXPath;
    private XPathExpression descriptionXPath;

    public static void main(String[] args) throws Exception {
        int res = new XmlToOwl().run(args);
        if (res != 0) {
            System.exit(res);
        }
    }

    @Override
    protected Options getOptions() {
        Options opts = super.getOptions();

        opts.addOption(
                OptionBuilder
                        .withLongOpt(CMD_OPT_INPUT)
                        .withDescription("XML Input file")
                        .hasArg()
                        .isRequired()
                        .create('i')
        );

        opts.addOption(
                OptionBuilder
                        .withLongOpt(CMD_OPT_OUTPUT)
                        .withDescription("Output .owl file")
                        .hasArg()
                        .isRequired()
                        .create('o')
        );

        return opts;
    }

    @Override
    protected int run(CommandLine cmd) throws Exception {
        XPathFactory xPathfactory = XPathFactory.newInstance();
        fieldXPath = xPathfactory.newXPath().compile("//field");
        descriptionXPath = xPathfactory.newXPath().compile("description/text()");

        File inFile = new File(cmd.getOptionValue(CMD_OPT_INPUT));
        if (!inFile.exists()) {
            System.err.println("Input file " + inFile.getAbsolutePath() + " does not exist.");
            return 1;
        }

        File outFile = new File(cmd.getOptionValue(CMD_OPT_OUTPUT));

        String owlTemplate = readOwlTemplate();
        String contents = createOwlContents(inFile);

        String owl = owlTemplate.replace(OWL_FILE_TEMPLATE_CONTENTS, contents);

        writeOwl(owl, outFile);

        return 0;
    }

    private String createOwlContents(File inFile) throws ParserConfigurationException, IOException, SAXException, XPathExpressionException {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(inFile);
        return createOwlContents(doc);
    }

    private String createOwlContents(Document doc) throws XPathExpressionException {

        NodeList fieldNodes = (NodeList) fieldXPath.evaluate(doc, XPathConstants.NODESET);

        StringBuilder results = new StringBuilder();
        for (int i = 0; i < fieldNodes.getLength(); i++) {
            Node fieldNode = fieldNodes.item(i);
            String xml = createDataPropertyOwlForField(fieldNode);
            if (xml == null) {
                continue;
            }
            results.append(xml);
            results.append("\n");
        }
        return results.toString();
    }

    private String createDataPropertyOwlForField(Node fieldNode) throws XPathExpressionException {
        String id = getAttributeValue(fieldNode, "id");
        if (id == null) {
            return null;
        }
        String name = getAttributeValue(fieldNode, "name");
        String description = (String) descriptionXPath.evaluate(fieldNode, XPathConstants.STRING);
        LOGGER.debug("creating field %s: %s", id, name);

        StringBuilder xml = new StringBuilder();
        xml.append("\t<owl:DatatypeProperty rdf:about=\"http://lumify.io/worldfactbook#" + id + "\">\n");
        xml.append("\t\t<rdfs:label xml:lang=\"en\">" + name + "</rdfs:label>\n");
        xml.append("\t\t<rdfs:comment xml:lang=\"en\">" + description + "</rdfs:comment>\n");
        xml.append("\t\t<rdfs:domain rdf:resource=\"http://lumify.io/worldfactbook#country\"/>\n");
        xml.append("\t\t<rdfs:range rdf:resource=\"&xsd;string\"/>\n");
        xml.append("\t</owl:DatatypeProperty>\n");
        return xml.toString();
    }

    private String getAttributeValue(Node node, String attributeName) {
        Node attribute = node.getAttributes().getNamedItem(attributeName);
        if (attribute == null) {
            return null;
        }
        return attribute.getNodeValue();
    }

    private void writeOwl(String owl, File outFile) throws IOException {
        FileOutputStream out = new FileOutputStream(outFile);
        try {
            out.write(owl.getBytes());
        } finally {
            out.close();
        }
    }

    private String readOwlTemplate() throws IOException {
        InputStream in = this.getClass().getResourceAsStream("owlFileTemplate.owl");
        try {
            return IOUtils.toString(in);
        } finally {
            in.close();
        }
    }
}
