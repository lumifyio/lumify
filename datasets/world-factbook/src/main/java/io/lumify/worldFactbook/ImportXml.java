package io.lumify.worldFactbook;

import io.lumify.core.cmdline.CommandLineBase;
import io.lumify.core.model.ontology.OntologyLumifyProperties;
import io.lumify.core.model.properties.LumifyProperties;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.securegraph.VertexBuilder;
import org.securegraph.Visibility;
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
import java.io.IOException;

public class ImportXml extends CommandLineBase {
    private static final String CMD_OPT_INPUT = "in";
    private static final String ID_PREFIX = "WORLDFACTBOOK_COUNTRY_";
    private static final String MULTI_VALUE_KEY = ImportXml.class.getName();
    private XPathExpression countryXPath;
    private XPathExpression fieldsXPath;
    private Visibility visibility = new Visibility("");

    public static void main(String[] args) throws Exception {
        int res = new ImportXml().run(args);
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

        return opts;
    }

    @Override
    protected int run(CommandLine cmd) throws Exception {
        XPathFactory xPathfactory = XPathFactory.newInstance();
        countryXPath = xPathfactory.newXPath().compile("//country");
        fieldsXPath = xPathfactory.newXPath().compile("field");

        File inFile = new File(cmd.getOptionValue(CMD_OPT_INPUT));
        if (!inFile.exists()) {
            System.err.println("Input file " + inFile.getAbsolutePath() + " does not exist.");
            return 1;
        }

        importXml(inFile);

        getGraph().flush();

        return 0;
    }

    private void importXml(File inFile) throws ParserConfigurationException, SAXException, IOException, XPathExpressionException {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(inFile);

        NodeList countryNodes = (NodeList) countryXPath.evaluate(doc, XPathConstants.NODESET);
        for (int i = 0; i < countryNodes.getLength(); i++) {
            Node countryNode = countryNodes.item(i);
            importCountryNode(countryNode);
        }
    }

    private void importCountryNode(Node countryNode) throws XPathExpressionException {
        String id = getAttributeValue(countryNode, "id");
        String name = getAttributeValue(countryNode, "name");

        LOGGER.debug("importing %s:%s", id, name);

        VertexBuilder vertex = getGraph().prepareVertex(ID_PREFIX + id, visibility);
        OntologyLumifyProperties.CONCEPT_TYPE.setProperty(vertex, WorldFactbookOntology.CONCEPT_TYPE_COUNTRY, visibility);
        LumifyProperties.TITLE.addPropertyValue(vertex, MULTI_VALUE_KEY, name, visibility);

        NodeList fieldNodes = (NodeList) fieldsXPath.evaluate(countryNode, XPathConstants.NODESET);
        for (int i = 0; i < fieldNodes.getLength(); i++) {
            Node fieldNode = fieldNodes.item(i);
            String ref = getAttributeValue(fieldNode, "ref");
            String value = fieldNode.getTextContent();
            vertex.addPropertyValue(MULTI_VALUE_KEY, "http://lumify.io/worldfactbook#" + ref, value, visibility);
        }

        vertex.save(getAuthorizations());
    }

    private String getAttributeValue(Node node, String attributeName) {
        Node attribute = node.getAttributes().getNamedItem(attributeName);
        if (attribute == null) {
            return null;
        }
        return attribute.getNodeValue();
    }
}
