package io.lumify.worldFactbook;

import com.google.inject.Inject;
import io.lumify.core.cmdline.CommandLineBase;
import io.lumify.core.ingest.FileImport;
import io.lumify.core.model.ontology.OntologyRepository;
import io.lumify.core.model.properties.LumifyProperties;
import io.lumify.core.security.VisibilityTranslator;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.securegraph.Metadata;
import org.securegraph.Vertex;
import org.securegraph.VertexBuilder;
import org.securegraph.Visibility;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.File;

public class ImportXml extends CommandLineBase {
    private static final String CMD_OPT_INPUT = "in";
    private static final String CMD_OPT_INPUT_DIRECTORY = "indir";
    private static final String COUNTRY_ID_PREFIX = "WORLDFACTBOOK_COUNTRY_";
    private static final String FLAG_EDGE_ID_PREFIX = "WORLDFACTBOOK_HAS_FLAG_";
    private static final String MAP_EDGE_ID_PREFIX = "WORLDFACTBOOK_HAS_MAP_";
    private static final String MULTI_VALUE_KEY = ImportXml.class.getName();
    private XPathExpression countryXPath;
    private XPathExpression fieldsXPath;
    private Visibility visibility = new Visibility("");
    private VisibilityTranslator visibilityTranslator;
    private FileImport fileImport;
    private String visibilitySource = "";
    private String entityHasImageIri;

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

        opts.addOption(
                OptionBuilder
                        .withLongOpt(CMD_OPT_INPUT_DIRECTORY)
                        .withDescription("Worldfact Book Download expanded directory")
                        .hasArg()
                        .isRequired()
                        .create()
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

        File indirFile = new File(cmd.getOptionValue(CMD_OPT_INPUT_DIRECTORY));
        if (!inFile.exists()) {
            System.err.println("Input directory " + indirFile.getAbsolutePath() + " does not exist.");
            return 1;
        }

        entityHasImageIri = getOntologyRepository().getRequiredRelationshipIRIByIntent("entityHasImage");

        importXml(inFile, indirFile);

        getGraph().flush();

        return 0;
    }

    private void importXml(File inFile, File indirFile) throws Exception {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(inFile);

        NodeList countryNodes = (NodeList) countryXPath.evaluate(doc, XPathConstants.NODESET);
        for (int i = 0; i < countryNodes.getLength(); i++) {
            Node countryNode = countryNodes.item(i);
            String countryId = getAttributeValue(countryNode, "id");
            Vertex countryVertex = importCountryNode(countryId, countryNode);
            importCountryFlag(indirFile, countryId, countryVertex);
            importCountyMap(indirFile, countryId, countryVertex);
        }
    }

    private Vertex importCountryFlag(File indirFile, String countryId, Vertex countryVertex) throws Exception {
        File flagFileName = new File(indirFile, "graphics/flags/large/" + countryId + "-lgflag.gif");
        if (!flagFileName.exists()) {
            LOGGER.debug("Could not find flag file: %s", flagFileName);
            return null;
        }

        Vertex flagVertex = fileImport.importFile(flagFileName, false, visibilitySource, null, getUser(), getAuthorizations());

        String flagTitle = "Flag of " + LumifyProperties.TITLE.getPropertyValue(countryVertex);
        Metadata flagImageMetadata = new Metadata();
        LumifyProperties.CONFIDENCE.setMetadata(flagImageMetadata, 0.5, visibilityTranslator.getDefaultVisibility());
        LumifyProperties.TITLE.addPropertyValue(flagVertex, MULTI_VALUE_KEY, flagTitle, flagImageMetadata, visibility, getAuthorizations());

        getGraph().addEdge(FLAG_EDGE_ID_PREFIX + countryId, countryVertex, flagVertex, entityHasImageIri, visibility, getAuthorizations());
        LumifyProperties.ENTITY_IMAGE_VERTEX_ID.addPropertyValue(countryVertex, MULTI_VALUE_KEY, flagVertex.getId(), visibility, getAuthorizations());

        return flagVertex;
    }

    private Vertex importCountyMap(File indirFile, String countryId, Vertex countryVertex) throws Exception {
        File mapFileName = new File(indirFile, "graphics/maps/newmaps/" + countryId + "-map.gif");
        if (!mapFileName.exists()) {
            LOGGER.debug("Could not find map file: %s", mapFileName);
            return null;
        }

        Vertex mapVertex = fileImport.importFile(mapFileName, false, visibilitySource, null, getUser(), getAuthorizations());

        String flagTitle = "Map of " + LumifyProperties.TITLE.getPropertyValue(countryVertex);
        Metadata mapImageMetadata = new Metadata();
        LumifyProperties.CONFIDENCE.setMetadata(mapImageMetadata, 0.5, visibilityTranslator.getDefaultVisibility());
        LumifyProperties.TITLE.addPropertyValue(mapVertex, MULTI_VALUE_KEY, flagTitle, mapImageMetadata, visibility, getAuthorizations());

        getGraph().addEdge(MAP_EDGE_ID_PREFIX + countryId, countryVertex, mapVertex, entityHasImageIri, visibility, getAuthorizations());

        return mapVertex;
    }

    private Vertex importCountryNode(String id, Node countryNode) throws XPathExpressionException {
        String name = getAttributeValue(countryNode, "name");

        LOGGER.debug("importing %s:%s", id, name);

        VertexBuilder vertex = getGraph().prepareVertex(COUNTRY_ID_PREFIX + id, visibility);
        LumifyProperties.CONCEPT_TYPE.setProperty(vertex, WorldFactbookOntology.CONCEPT_TYPE_COUNTRY, visibility);
        LumifyProperties.TITLE.addPropertyValue(vertex, MULTI_VALUE_KEY, name, visibility);

        NodeList fieldNodes = (NodeList) fieldsXPath.evaluate(countryNode, XPathConstants.NODESET);
        for (int i = 0; i < fieldNodes.getLength(); i++) {
            Node fieldNode = fieldNodes.item(i);
            String ref = getAttributeValue(fieldNode, "ref");
            String value = fieldNode.getTextContent();
            vertex.addPropertyValue(MULTI_VALUE_KEY, "http://lumify.io/worldfactbook#" + ref, value, visibility);
        }

        return vertex.save(getAuthorizations());
    }

    private String getAttributeValue(Node node, String attributeName) {
        Node attribute = node.getAttributes().getNamedItem(attributeName);
        if (attribute == null) {
            return null;
        }
        return attribute.getNodeValue();
    }

    @Inject
    public void setFileImport(FileImport fileImport) {
        this.fileImport = fileImport;
    }

    @Inject
    public void setVisibilityTranslator(VisibilityTranslator visibilityTranslator) {
        this.visibilityTranslator = visibilityTranslator;
    }
}
