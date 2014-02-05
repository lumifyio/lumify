package com.altamiracorp.lumify.core.cmdline;

import static com.altamiracorp.lumify.core.model.properties.LumifyProperties.GLYPH_ICON;
import static com.altamiracorp.lumify.core.model.properties.LumifyProperties.MAP_GLYPH_ICON;

import com.altamiracorp.lumify.core.model.ontology.Concept;
import com.altamiracorp.lumify.core.model.ontology.OntologyRepository;
import com.altamiracorp.lumify.core.model.ontology.PropertyType;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.securegraph.Graph;
import com.altamiracorp.securegraph.Vertex;
import com.altamiracorp.securegraph.property.StreamingPropertyValue;
import com.google.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.io.IOUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class OwlImport extends CommandLineBase {
    public static final String NS_XML_URI = "http://www.w3.org/XML/1998/namespace";

    private OntologyRepository ontologyRepository;
    private Graph graph;
    private String inFileName;
    private File inDir;

    public static void main(String[] args) throws Exception {
        int res = new OwlImport().run(args);
        if (res != 0) {
            System.exit(res);
        }
    }

    @Override
    protected Options getOptions() {
        Options options = super.getOptions();

        options.addOption(
                OptionBuilder
                        .withLongOpt("in")
                        .withDescription("The input OWL file")
                        .isRequired()
                        .hasArg(true)
                        .withArgName("fileName")
                        .create("i")
        );

        return options;
    }

    @Override
    protected void processOptions(CommandLine cmd) throws Exception {
        super.processOptions(cmd);
        this.inFileName = cmd.getOptionValue("in");
    }

    @Override
    protected int run(CommandLine cmd) throws Exception {
        File inFile = new File(inFileName);
        importFile(inFile, getUser());
        return 0;
    }

    public void importFile(File inFile, User user) throws ParserConfigurationException, SAXException, IOException {
        ensureLoggerInitialized();
        inDir = inFile.getParentFile();

        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        dbFactory.setNamespaceAware(true);
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(inFile);

        Element rootElem = doc.getDocumentElement();
        NodeList rootElemChildNodes = rootElem.getChildNodes();
        for (int i = 0; i < rootElemChildNodes.getLength(); i++) {
            Node child = rootElemChildNodes.item(i);
            if (child instanceof Element) {
                Element childElem = (Element) child;
                if (childElem.getNamespaceURI().equals("http://www.w3.org/2002/07/owl#") && childElem.getLocalName().equals("Class")) {
                    importClassElement(childElem, user);
                } else if (childElem.getNamespaceURI().equals("http://www.w3.org/2002/07/owl#") && childElem.getLocalName().equals("DatatypeProperty")) {
                    importDatatypePropertyElement(childElem, user);
                } else if (childElem.getNamespaceURI().equals("http://www.w3.org/2002/07/owl#") && childElem.getLocalName().equals("ObjectProperty")) {
                    importObjectPropertyElement(childElem, user);
                }
            }
        }
    }

    private void importClassElement(Element classElem, User user) {
        String about = getName(classElem.getAttributeNS("http://www.w3.org/1999/02/22-rdf-syntax-ns#", "about"));
        Element labelElem = getEnglishLanguageLabel(classElem);
        String labelText = labelElem.getTextContent().trim();
        Element subClassOf = getSingleChildElement(classElem, "http://www.w3.org/2000/01/rdf-schema#", "subClassOf");
        String subClassOfResource = subClassOf.getAttributeNS("http://www.w3.org/1999/02/22-rdf-syntax-ns#", "resource");
        List<Element> propertyElems = getChildElements(classElem, "http://altamiracorp.com/ontology#", "property");

        String parentName = getName(subClassOfResource);

        LOGGER.info("importClassElement: about: " + about + ", labelText: " + labelText + ", parentName: " + parentName);
        Concept parent = ontologyRepository.getConceptByName(parentName);
        if (parent == null) {
            throw new RuntimeException("Could not find parent " + parentName + " for " + about);
        }
        Concept concept = ontologyRepository.getOrCreateConcept(parent, about, labelText);

        for (Element propertyElem : propertyElems) {
            String propertyName = propertyElem.getAttributeNS("http://altamiracorp.com/ontology#", "name");
            Object propertyValue = propertyElem.getTextContent().trim();
            LOGGER.info("  " + propertyName + " = " + propertyValue);
            if (propertyName.equals("glyphIconFileName")) {
                propertyName = GLYPH_ICON.getKey();
                propertyValue = importGlyphIconFile(propertyValue.toString(), user);
            } else if (propertyName.equals("mapGlyphIconFileName")) {
                propertyName = MAP_GLYPH_ICON.getKey();
                propertyValue = importGlyphIconFile(propertyValue.toString(), user);
            }
            concept.setProperty(propertyName, propertyValue, OntologyRepository.DEFAULT_VISIBILITY);
        }
        graph.flush();
    }

    private Element getEnglishLanguageLabel(Element elem) {
        List<Element> childElems = getChildElements(elem, "http://www.w3.org/2000/01/rdf-schema#", "label");
        for (Element childElem : childElems) {
            String attr = childElem.getAttributeNS(NS_XML_URI, "lang");
            if (attr.equals("en")) {
                return childElem;
            }
        }
        throw new RuntimeException("Could not find english label on element " + elem.getTagName());
    }

    private StreamingPropertyValue importGlyphIconFile(String fileName, User user) {
        File f = new File(inDir, fileName);
        FileInputStream in = null;
        try {
            in = new FileInputStream(f.getAbsoluteFile());
            if (!fileName.endsWith(".png")) {
                throw new RuntimeException("Unhandled content type: " + fileName);
            }

            ByteArrayOutputStream imgOut = new ByteArrayOutputStream();
            IOUtils.copy(in, imgOut);

            byte[] rawImg = imgOut.toByteArray();

            StreamingPropertyValue raw = new StreamingPropertyValue(new ByteArrayInputStream(rawImg), byte[].class);
            raw.searchIndex(false);
            return raw;
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Could not import file: " + fileName, e);
        } catch (IOException e) {
            throw new RuntimeException("invalid stream for glyph icon");
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    throw new RuntimeException("Could not import file: " + fileName, e);
                }
            }
        }
    }

    private void importDatatypePropertyElement(Element datatypePropertyElem, User user) {
        String about = datatypePropertyElem.getAttributeNS("http://www.w3.org/1999/02/22-rdf-syntax-ns#", "about");
        Element labelElem = getEnglishLanguageLabel(datatypePropertyElem);
        String labelText = labelElem.getTextContent().trim();
        Element domainElem = getSingleChildElement(datatypePropertyElem, "http://www.w3.org/2000/01/rdf-schema#", "domain");
        String domainResource = domainElem.getAttributeNS("http://www.w3.org/1999/02/22-rdf-syntax-ns#", "resource");
        Element rangeElem = getSingleChildElement(datatypePropertyElem, "http://www.w3.org/2000/01/rdf-schema#", "range");
        String rangeResource = rangeElem.getAttributeNS("http://www.w3.org/1999/02/22-rdf-syntax-ns#", "resource");

        String domainResourceName = getName(domainResource);
        String rangeResourceName = getName(rangeResource);

        LOGGER.info("importDatatypePropertyElement: about: " + about + ", labelText: " + labelText + ", domainResourceName: " + domainResourceName + ", rangeResourceName: " + rangeResourceName);
        Vertex domain = ontologyRepository.getGraphVertexByTitle(domainResourceName);
        if (domain == null) {
            throw new RuntimeException("Could not find domain: " + domainResourceName);
        }
        PropertyType propertyType = PropertyType.convert(rangeResourceName);
        graph.flush();

        ontologyRepository.addPropertyTo(domain, about, labelText, propertyType);
    }

    private void importObjectPropertyElement(Element objectPropertyElem, User user) {
        String about = objectPropertyElem.getAttributeNS("http://www.w3.org/1999/02/22-rdf-syntax-ns#", "about");
        Element labelElem = getEnglishLanguageLabel(objectPropertyElem);
        String labelText = labelElem.getTextContent().trim();
        Element domainElem = getSingleChildElement(objectPropertyElem, "http://www.w3.org/2000/01/rdf-schema#", "domain");
        String domainResource = domainElem.getAttributeNS("http://www.w3.org/1999/02/22-rdf-syntax-ns#", "resource");
        Element rangeElem = getSingleChildElement(objectPropertyElem, "http://www.w3.org/2000/01/rdf-schema#", "range");
        String rangeResource = rangeElem.getAttributeNS("http://www.w3.org/1999/02/22-rdf-syntax-ns#", "resource");

        String domainResourceName = getName(domainResource);
        String rangeResourceName = getName(rangeResource);

        LOGGER.info("importObjectPropertyElement: about: " + about + ", labelText: " + labelText + ", domainResourceName: " + domainResourceName + ", rangeResourceName: " + rangeResourceName);
        Concept domain = ontologyRepository.getConceptByName(domainResourceName);
        Concept range = ontologyRepository.getConceptByName(rangeResourceName);

        ontologyRepository.getOrCreateRelationshipType(domain, range, about, labelText);
    }

    private Element getSingleChildElement(Element elem, String ns, String localName) {
        List<Element> childElems = getChildElements(elem, ns, localName);
        if (childElems.size() == 0) {
            return null;
        }
        if (childElems.size() > 1) {
            throw new RuntimeException("More than one child found with " + ns + ":" + localName + " on element " + elem.getTagName());
        }
        return childElems.get(0);
    }

    private List<Element> getChildElements(Element elem, String ns, String localName) {
        List<Element> childElems = new ArrayList<Element>();
        NodeList children = elem.getElementsByTagNameNS(ns, localName);
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child instanceof Element) {
                if (child.getLocalName().equals(localName)) {
                    childElems.add((Element) child);
                }
            }
        }
        return childElems;
    }

    private String getName(String s) {
        if (s.startsWith("#")) {
            return s.substring("#".length());
        }

        int lastSlash = s.lastIndexOf('/');
        if (lastSlash > 0) {
            return s.substring(lastSlash + 1);
        }

        return s;
    }

    @Inject
    public void setOntologyRepository(OntologyRepository ontologyRepository) {
        this.ontologyRepository = ontologyRepository;
    }

    @Inject
    public void setGraph(Graph graph) {
        this.graph = graph;
    }
}
