package io.lumify.palantir.ontologyToOwl;

import io.lumify.core.exception.LumifyException;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OntologyToOwl {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(OntologyToOwl.class);
    public static final String INVERSE_SUFFIX = "_inverse";
    private String baseIri;
    private final File outFile;
    private Element exportRootElement;
    private Document exportDoc;
    private XPathFactory xPathfactory;
    private Document linkRelationsXml;
    private Document imageInfoXml;
    private DocumentBuilder docBuilder;
    private XPath xPath;
    private NamespaceContext ns;
    private TitleFormulaMaker titleFormulaMaker;
    private Map<String, ObjectProperty> objectProperties = new HashMap<String, ObjectProperty>();
    private Map<String, DataTypeProperty> dataTypeProperties = new HashMap<String, DataTypeProperty>();
    private Map<String, OwlClass> owlClasses = new HashMap<String, OwlClass>();
    private Map<String, List<OwlElement>> iconMapping = new HashMap<String, List<OwlElement>>();

    public OntologyToOwl(String baseIri, File outFile) {
        this.baseIri = baseIri;
        this.outFile = outFile;
        titleFormulaMaker = new TitleFormulaMaker(baseIri);
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 3) {
            System.out.println("Required two arguments <inDir> <baseIri> <outFile>");
            System.exit(-1);
            return;
        }

        File inDir = new File(args[0]);
        String baseIri = args[1];
        File outFile = new File(args[2]);

        if (baseIri.endsWith("#")) {
            baseIri = baseIri.substring(0, baseIri.length() - 1);
        }

        new OntologyToOwl(baseIri, outFile).run(inDir);
    }

    private void run(File inDir) throws Exception {
        if (!inDir.exists()) {
            throw new LumifyException("inDir (" + inDir + ") does not exist");
        }

        xPathfactory = XPathFactory.newInstance();
        xPath = xPathfactory.newXPath();
        ns = new OwlNamespaceContext();
        xPath.setNamespaceContext(ns);

        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        docFactory.setNamespaceAware(true);
        docBuilder = docFactory.newDocumentBuilder();
        exportDoc = docBuilder.newDocument();

        exportRootElement = exportDoc.createElementNS(ns.getNamespaceURI("rdf"), "rdf:RDF");
        exportRootElement.setAttribute("xmlns:rdf", ns.getNamespaceURI("rdf"));
        exportRootElement.setAttribute("xmlns:owl", ns.getNamespaceURI("owl"));
        exportRootElement.setAttribute("xmlns:rdfs", ns.getNamespaceURI("rdfs"));
        exportRootElement.setAttribute("xmlns:lumify", ns.getNamespaceURI("lumify"));
        exportDoc.appendChild(exportRootElement);

        Element ontologyElement = exportDoc.createElementNS(ns.getNamespaceURI("owl"), "owl:Ontology");
        ontologyElement.setAttributeNS(ns.getNamespaceURI("rdf"), "rdf:about", baseIri);
        exportRootElement.appendChild(ontologyElement);

        Element importsElement = exportDoc.createElementNS(ns.getNamespaceURI("owl"), "owl:imports");
        importsElement.setAttributeNS(ns.getNamespaceURI("rdf"), "rdf:resource", "http://lumify.io");
        ontologyElement.appendChild(importsElement);

        runOnDir(inDir);

        if (linkRelationsXml == null) {
            LOGGER.warn("Could not find link relations xml.");
        } else {
            runOnLinkRelationsXml(linkRelationsXml);
            writeLinkRelations();
        }

        if (imageInfoXml == null) {
            LOGGER.warn("Could not find image info xml.");
        } else {
            runOnImageInfoXml(imageInfoXml);
        }

        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        DOMSource source = new DOMSource(exportDoc);
        StreamResult result = new StreamResult(outFile);
        transformer.transform(source, result);
    }

    private void runOnImageInfoXml(Document imageInfoXml) {
        List<Element> imageInfoConfigs = getXmlElements(imageInfoXml, "/image_infos/image_info_config");
        for (Element imageInfoConfig : imageInfoConfigs) {
            try {
                runOnImageInfoConfig(imageInfoConfig);
            } catch (Exception ex) {
                LOGGER.error("Could not process: %s", imageInfoConfig.toString(), ex);
            }
        }
    }

    private void runOnImageInfoConfig(Element imageInfoConfig) {
        String uri = getXmlString(imageInfoConfig, "uri");
        String path = getXmlString(imageInfoConfig, "path");

        if (path.startsWith("/")) {
            path = path.substring(1);
        }

        File expectedFile = new File(outFile.getParent(), path);
        if (!expectedFile.exists()) {
            throw new LumifyException("Could not find file for uri " + uri + " with path " + expectedFile.getAbsolutePath());
        }

        List<OwlElement> owlElements = iconMapping.get(uri);
        if (owlElements == null) {
            LOGGER.warn("Could not find owl elements for icon mapping with uri: %s", uri);
            return;
        }

        for (OwlElement owlElement : owlElements) {
            Element glyphIconFileNameElement = exportDoc.createElementNS(ns.getNamespaceURI("lumify"), "lumify:glyphIconFileName");
            glyphIconFileNameElement.appendChild(exportDoc.createTextNode(path));
            owlElement.getElement().appendChild(glyphIconFileNameElement);
        }
    }

    private void writeLinkRelations() {
        writeObjectPropertyLinkRelations();
        writeDataTypePropertyLinkRelations();
    }

    private void writeDataTypePropertyLinkRelations() {
        for (DataTypeProperty dataTypeProperty : dataTypeProperties.values()) {
            for (String domainUri : dataTypeProperty.getDomainUris()) {
                Element domainElement = exportDoc.createElementNS(ns.getNamespaceURI("rdfs"), "rdfs:domain");
                domainElement.setAttributeNS(ns.getNamespaceURI("rdf"), "rdf:resource", domainUri);
                dataTypeProperty.getElement().appendChild(domainElement);
            }
        }
    }

    private void writeObjectPropertyLinkRelations() {
        for (ObjectProperty objectProperty : objectProperties.values()) {
            for (String domainUri : objectProperty.getDomainUris()) {
                Element domainElement = exportDoc.createElementNS(ns.getNamespaceURI("rdfs"), "rdfs:domain");
                domainElement.setAttributeNS(ns.getNamespaceURI("rdf"), "rdf:resource", domainUri);
                objectProperty.getElement().appendChild(domainElement);
            }

            for (String rangeUri : objectProperty.getRangeUris()) {
                Element rangeElement = exportDoc.createElementNS(ns.getNamespaceURI("rdfs"), "rdfs:range");
                rangeElement.setAttributeNS(ns.getNamespaceURI("rdf"), "rdf:resource", rangeUri);
                objectProperty.getElement().appendChild(rangeElement);
            }
        }
    }

    private void runOnDir(File inDir) {
        File[] files = inDir.listFiles();
        if (files == null) {
            return;
        }
        for (File f : files) {
            if (f.isDirectory()) {
                runOnDir(f);
            } else {
                try {
                    runOnFile(f);
                } catch (Throwable e) {
                    LOGGER.error("Could not process: %s", f.getAbsolutePath(), e);
                }
            }
        }
    }

    private void runOnFile(File inFile) throws IOException, SAXException {
        if (!inFile.getAbsolutePath().toLowerCase().endsWith(".xml")) {
            LOGGER.warn("skipping file: %s", inFile.getAbsolutePath());
            return;
        }

        Document inXml = docBuilder.parse(inFile);

        LOGGER.info("processing %s xml: %s", inXml.getDocumentElement().getNodeName(), inFile.getAbsolutePath());
        if (inXml.getDocumentElement().getNodeName().equals("pt_object_type_config")) {
            runOnObjectTypeConfig(inXml);
        } else if (inXml.getDocumentElement().getNodeName().equals("ontology_resource_config")) {
            runOnOntologyResourceConfig(inXml);
        } else if (inXml.getDocumentElement().getNodeName().equals("ontologyExportInfo")) {
            // skip
        } else if (inXml.getDocumentElement().getNodeName().equals("link_type_config")) {
            runOnLinkTypeConfig(inXml);
        } else if (inXml.getDocumentElement().getNodeName().equals("property_type_config")) {
            runOnPropertyTypeConfig(inXml);
        } else if (inXml.getDocumentElement().getNodeName().equals("node_display_type_config")) {
            // skip
        } else if (inXml.getDocumentElement().getNodeName().equals("type_group_config")) {
            // skip
        } else if (inXml.getDocumentElement().getNodeName().equals("link_relations")) {
            linkRelationsXml = inXml;
        } else if (inXml.getDocumentElement().getNodeName().equals("image_infos")) {
            imageInfoXml = inXml;
        } else {
            throw new LumifyException("Invalid xml root node name: " + inXml.getDocumentElement().getNodeName());
        }
    }

    private void runOnOntologyResourceConfig(Document inXml) throws IOException {
        String path = getXmlString(inXml, "/ontology_resource_config/path");
        String contents = getXmlString(inXml, "/ontology_resource_config/contents");

        if (path.startsWith("/")) {
            path = path.substring(1);
        }

        byte[] data = Base64.decodeBase64(contents);

        File file = new File(outFile.getParent(), path);
        file.getParentFile().mkdirs();

        FileUtils.writeByteArrayToFile(file, data);
    }

    private void runOnLinkRelationsXml(Document linkRelationsXml) {
        List<Element> linkRelationConfigs = getXmlElements(linkRelationsXml, "/link_relations/link_relation_config");
        for (Element linkRelationConfig : linkRelationConfigs) {
            try {
                runOnLinkRelationConfig(linkRelationConfig);
            } catch (Exception ex) {
                LOGGER.error("Could not process: %s", linkRelationConfig.toString(), ex);
            }
        }
    }

    private void runOnLinkRelationConfig(Element linkRelationConfig) {
        String uri1 = getXmlString(linkRelationConfig, "uri1");
        String uri2 = getXmlString(linkRelationConfig, "uri2");
        String linkUri = getXmlString(linkRelationConfig, "linkUri");

        LOGGER.info("processing %s, %s, %s", uri1, uri2, linkUri);

        // TODO linkStatus ???
        // TODO hidden ???
        // TODO tableType1 or tableType2 ???

        if ("com.palantir.link.Simple".equals(linkUri)) {
            DataTypeProperty dataTypeProperty = dataTypeProperties.get(uri2);
            if (dataTypeProperty != null) {
                dataTypeProperty.addDomain(uriToIri(uri1));
                return;
            }
        }

        ObjectProperty objectProperty = objectProperties.get(linkUri);
        if (objectProperty != null) {
            objectProperty.addDomain(uriToIri(uri2));
            objectProperty.addRange(uriToIri(uri1));

            ObjectProperty objectPropertyInverse = objectProperties.get(linkUri + INVERSE_SUFFIX);
            if (objectPropertyInverse != null) {
                objectPropertyInverse.addDomain(uriToIri(uri2));
                objectPropertyInverse.addRange(uriToIri(uri1));
            }

            return;
        }

        throw new LumifyException("Could not find a object property or data type property matching '" + linkUri + "' or '" + uri2 + "'");
    }

    private void runOnPropertyTypeConfig(Document inXml) {
        String uri = getXmlString(inXml, "/property_type_config/uri");
        String label = getXmlString(inXml, "/property_type_config/type/displayName");
        String comment = getXmlString(inXml, "/property_type_config/description");
        List<Element> enumerationEntries = getXmlElements(inXml, "/property_type_config/type/enumeration/entry");

        Element datatypePropertyElement = exportDoc.createElementNS(ns.getNamespaceURI("owl"), "owl:DatatypeProperty");
        datatypePropertyElement.setAttributeNS(ns.getNamespaceURI("rdf"), "rdf:about", uriToIri(uri));
        exportRootElement.appendChild(datatypePropertyElement);

        Element labelElement = exportDoc.createElementNS(ns.getNamespaceURI("rdfs"), "rdfs:label");
        labelElement.setAttributeNS(ns.getNamespaceURI("xml"), "xml:lang", "en");
        labelElement.appendChild(exportDoc.createTextNode(label));
        datatypePropertyElement.appendChild(labelElement);

        Element rangeElement = exportDoc.createElementNS(ns.getNamespaceURI("rdfs"), "rdfs:range");
        rangeElement.setAttributeNS(ns.getNamespaceURI("rdf"), "rdf:resource", "http://www.w3.org/2001/XMLSchema#string");
        datatypePropertyElement.appendChild(rangeElement);

        if (comment != null && comment.length() > 0) {
            Element commentElement = exportDoc.createElementNS(ns.getNamespaceURI("rdfs"), "rdfs:comment");
            commentElement.appendChild(exportDoc.createTextNode(comment));
            datatypePropertyElement.appendChild(commentElement);
        }

        if (enumerationEntries.size() > 0) {
            JSONObject possibleValues = new JSONObject();
            for (Element enumerationEntry : enumerationEntries) {
                String key = getXmlString(enumerationEntry, "key");
                String value = getXmlString(enumerationEntry, "value");
                possibleValues.put(key, value);
            }
            Element possibleValuesElement = exportDoc.createElementNS(ns.getNamespaceURI("lumify"), "lumify:possibleValues");
            possibleValuesElement.appendChild(exportDoc.createTextNode("\n" + indent(possibleValues.toString(2), "      ") + "\n    "));
            datatypePropertyElement.appendChild(possibleValuesElement);
        }

        dataTypeProperties.put(uri, new DataTypeProperty(datatypePropertyElement));
    }

    private String indent(String stringToIndent, String indentChars) {
        return indentChars + stringToIndent.replaceAll("\n", "\n" + indentChars);
    }

    private void runOnLinkTypeConfig(Document inXml) {
        String uri = getXmlString(inXml, "/link_type_config/uri");
        String label = getXmlString(inXml, "/link_type_config/displayName");
        List<Element> asymmetricElements = getXmlElements(inXml, "/link_type_config/asymmetric");

        // TODO edgeIconUri ???
        // TODO infoIconUri ???
        // TODO objectToObject ???
        // TODO objectToProperty ???
        // TODO systemProperty ???
        // TODO typeGroups ???

        Element objectPropertyElement = exportDoc.createElementNS(ns.getNamespaceURI("owl"), "owl:ObjectProperty");
        objectPropertyElement.setAttributeNS(ns.getNamespaceURI("rdf"), "rdf:about", uriToIri(uri));
        exportRootElement.appendChild(objectPropertyElement);

        if (asymmetricElements != null && asymmetricElements.size() > 0) {
            if (asymmetricElements.size() > 1) {
                throw new LumifyException("To many 'asymmetric' elements found. Expected 0 or 1, found " + asymmetricElements.size());
            }
            String parentToChildLabel = getXmlString(asymmetricElements.get(0), "parentToChild/displayName");
            String childToParentLabel = getXmlString(asymmetricElements.get(0), "childToParent/displayName");

            if (parentToChildLabel != null && parentToChildLabel.length() > 0) {
                label = parentToChildLabel;
            }

            String inverseLabel = label;
            if (childToParentLabel != null && childToParentLabel.length() > 0) {
                inverseLabel = childToParentLabel;
            }

            Element inverseObjectPropertyElement = exportDoc.createElementNS(ns.getNamespaceURI("owl"), "owl:ObjectProperty");
            inverseObjectPropertyElement.setAttributeNS(ns.getNamespaceURI("rdf"), "rdf:about", uriToIri(uri + INVERSE_SUFFIX));
            exportRootElement.appendChild(inverseObjectPropertyElement);

            Element labelElement = exportDoc.createElementNS(ns.getNamespaceURI("rdfs"), "rdfs:label");
            labelElement.setAttributeNS(ns.getNamespaceURI("xml"), "xml:lang", "en");
            labelElement.appendChild(exportDoc.createTextNode(inverseLabel));
            inverseObjectPropertyElement.appendChild(labelElement);

            Element inverseOfElement = exportDoc.createElementNS(ns.getNamespaceURI("owl"), "owl:inverseOf");
            inverseOfElement.setAttributeNS(ns.getNamespaceURI("rdf"), "rdf:resource", uriToIri(uri + INVERSE_SUFFIX));
            objectPropertyElement.appendChild(inverseOfElement);

            inverseOfElement = exportDoc.createElementNS(ns.getNamespaceURI("owl"), "owl:inverseOf");
            inverseOfElement.setAttributeNS(ns.getNamespaceURI("rdf"), "rdf:resource", uriToIri(uri));
            inverseObjectPropertyElement.appendChild(inverseOfElement);

            objectProperties.put(uri + INVERSE_SUFFIX, new ObjectProperty(inverseObjectPropertyElement));
        }

        Element labelElement = exportDoc.createElementNS(ns.getNamespaceURI("rdfs"), "rdfs:label");
        labelElement.setAttributeNS(ns.getNamespaceURI("xml"), "xml:lang", "en");
        labelElement.appendChild(exportDoc.createTextNode(label));
        objectPropertyElement.appendChild(labelElement);

        objectProperties.put(uri, new ObjectProperty(objectPropertyElement));
    }

    private void addIconMapping(String iconUri, OwlElement owlElement) {
        List<OwlElement> list = iconMapping.get(iconUri);
        if (list == null) {
            list = new ArrayList<OwlElement>();
            iconMapping.put(iconUri, list);
        }
        list.add(owlElement);
    }

    private void runOnObjectTypeConfig(Document inXml) {
        String uri = getXmlString(inXml, "/pt_object_type_config/uri");
        String label = getXmlString(inXml, "/pt_object_type_config/displayName");
        String parentTypeUri = getXmlString(inXml, "/pt_object_type_config/parentType");
        String infoIconUri = getXmlString(inXml, "/pt_object_type_config/display/infoIconUri");
        String comment = getXmlString(inXml, "/pt_object_type_config/description");
        List<Element> titleArgs = getXmlElements(inXml, "/pt_object_type_config/title/args/arg");

        // TODO baseType ???
        // TODO commitable ???
        // TODO intrinsic ???
        // TODO guessers ???
        // TODO typeGroups ???
        // TODO display/defaultNodeDisplayTypeUri ???
        // TODO display/edgeIconUri ???
        // TODO labelPropertyUri ???

        Element classElement = exportDoc.createElementNS(ns.getNamespaceURI("owl"), "owl:Class");
        classElement.setAttributeNS(ns.getNamespaceURI("rdf"), "rdf:about", uriToIri(uri));
        exportRootElement.appendChild(classElement);

        Element labelElement = exportDoc.createElementNS(ns.getNamespaceURI("rdfs"), "rdfs:label");
        labelElement.setAttributeNS(ns.getNamespaceURI("xml"), "xml:lang", "en");
        labelElement.appendChild(exportDoc.createTextNode(label));
        classElement.appendChild(labelElement);

        if (parentTypeUri != null && parentTypeUri.length() > 0 && !parentTypeUri.equals(uri)) {
            Element subClassOfElement = exportDoc.createElementNS(ns.getNamespaceURI("rdfs"), "rdfs:subClassOf");
            subClassOfElement.setAttributeNS(ns.getNamespaceURI("rdf"), "rdf:resource", uriToIri(parentTypeUri));
            classElement.appendChild(subClassOfElement);
        }

        if (comment != null && comment.length() > 0) {
            Element commentElement = exportDoc.createElementNS(ns.getNamespaceURI("rdfs"), "rdfs:comment");
            commentElement.appendChild(exportDoc.createTextNode(comment));
            classElement.appendChild(commentElement);
        }

        if (titleArgs != null && titleArgs.size() > 0) {
            String titleFormula = titleFormulaMaker.create(titleArgs);
            if (titleFormula.trim().length() > 0) {
                Element titleFormulaElement = exportDoc.createElementNS(ns.getNamespaceURI("lumify"), "lumify:titleFormula");
                titleFormulaElement.appendChild(exportDoc.createCDATASection("\n" + indent(titleFormula, "      ") + "\n    "));
                classElement.appendChild(titleFormulaElement);
            }
        }

        OwlClass owlClass = new OwlClass(classElement);
        owlClasses.put(uri, owlClass);

        if (infoIconUri != null && infoIconUri.length() > 0) {
            addIconMapping(infoIconUri, owlClass);
        }
    }

    private String getXmlString(Node inXml, String xpath) {
        if (!xpath.contains("/")) {
            NodeList elements = ((Element) inXml).getElementsByTagName(xpath);
            if (elements.getLength() == 1) {
                return elements.item(0).getTextContent();
            }
        }

        try {
            return xPath.evaluate(xpath, inXml);
        } catch (Exception ex) {
            throw new LumifyException("Could not run xpath: " + xpath, ex);
        }
    }

    private List<Element> getXmlElements(Document inXml, String xpath) {
        try {
            NodeList nodeList = (NodeList) xPath.evaluate(xpath, inXml, XPathConstants.NODESET);
            List<Element> results = new ArrayList<Element>();
            for (int i = 0; i < nodeList.getLength(); i++) {
                results.add((Element) nodeList.item(i));
            }
            return results;
        } catch (Exception ex) {
            throw new LumifyException("Could not run xpath: " + xpath, ex);
        }
    }

    private String uriToIri(String uri) {
        return uriToIri(baseIri, uri);
    }

    public static String uriToIri(String baseIri, String uri) {
        return baseIri + '#' + uri;
    }
}
