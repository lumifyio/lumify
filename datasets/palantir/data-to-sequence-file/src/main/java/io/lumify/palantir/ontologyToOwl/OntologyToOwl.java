package io.lumify.palantir.ontologyToOwl;

import io.lumify.core.exception.LumifyException;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import io.lumify.palantir.DataToSequenceFile;
import io.lumify.palantir.service.Exporter;
import org.apache.commons.codec.binary.Base64;
import org.apache.hadoop.fs.*;
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
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OntologyToOwl implements Exporter {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(OntologyToOwl.class);
    public static final String INVERSE_SUFFIX = "_inverse";
    private final String baseIri;
    private Element exportRootElement;
    private Document exportDoc;
    private Document linkRelationsXml;
    private Document imageInfoXml;
    private DocumentBuilder docBuilder;
    private XPath xPath;
    private NamespaceContext ns;
    private TitleFormulaMaker titleFormulaMaker;
    private Map<String, ObjectProperty> objectProperties = new HashMap<>();
    private Map<String, DataTypeProperty> dataTypeProperties = new HashMap<>();
    private Map<String, OwlClass> owlClasses = new HashMap<>();
    private Map<String, List<OwlElement>> iconMapping = new HashMap<>();

    public OntologyToOwl(String baseIri) {
        if (baseIri.endsWith("#")) {
            baseIri = baseIri.substring(0, baseIri.length() - 1);
        }
        this.baseIri = baseIri;
        this.titleFormulaMaker = new TitleFormulaMaker(baseIri);
    }

    @Override
    public void run(ExporterSource exporterSource) throws Exception {
        run(exporterSource.getFileSystem(), exporterSource.getDestinationPath());
    }

    public void run(FileSystem fs, Path destinationPath) throws Exception {
        Path outPath = new Path(destinationPath, "owl");

        XPathFactory xPathfactory = XPathFactory.newInstance();
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

        runOnDir(fs, outPath, new Path(destinationPath, DataToSequenceFile.ONTOLOGY_XML_DIR_NAME));

        if (linkRelationsXml == null) {
            LOGGER.warn("Could not find link relations xml.");
        } else {
            runOnLinkRelationsXml(linkRelationsXml);
            writeLinkRelations();
        }

        if (imageInfoXml == null) {
            LOGGER.warn("Could not find image info xml.");
        } else {
            runOnImageInfoXml(fs, destinationPath, imageInfoXml);
        }

        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        DOMSource source = new DOMSource(exportDoc);
        try (OutputStream out = fs.create(new Path(outPath, "palantir.owl"), true)) {
            StreamResult result = new StreamResult(out);
            transformer.transform(source, result);
        }
    }

    private void runOnImageInfoXml(FileSystem fs, Path destinationPath, Document imageInfoXml) {
        List<Element> imageInfoConfigs = getXmlElements(imageInfoXml, "/image_infos/image_info_config");
        for (Element imageInfoConfig : imageInfoConfigs) {
            try {
                runOnImageInfoConfig(fs, destinationPath, imageInfoConfig);
            } catch (Exception ex) {
                LOGGER.error("Could not process: %s", imageInfoConfig.toString(), ex);
            }
        }
    }

    private void runOnImageInfoConfig(FileSystem fs, Path destinationPath, Element imageInfoConfig) throws IOException {
        String uri = getXmlString(imageInfoConfig, "uri");
        String path = getXmlString(imageInfoConfig, "path");

        if (path.startsWith("/")) {
            path = path.substring(1);
        }

        Path expectedFilePath = new Path(destinationPath, path);
        if (!fs.exists(expectedFilePath)) {
            throw new LumifyException("Could not find file for uri " + uri + " with path " + expectedFilePath.toString());
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

    private void runOnDir(FileSystem fs, Path outPath, Path inDir) throws IOException {
        RemoteIterator<LocatedFileStatus> files = fs.listFiles(inDir, true);
        if (files == null) {
            return;
        }
        while (files.hasNext()) {
            LocatedFileStatus f = files.next();
            try {
                runOnFile(fs, outPath, f);
            } catch (Throwable e) {
                LOGGER.error("Could not process: %s", f.toString(), e);
            }
        }
    }

    private void runOnFile(FileSystem fs, Path outPath, LocatedFileStatus inFile) throws IOException, SAXException {
        if (!inFile.getPath().getName().toLowerCase().endsWith(".xml")) {
            LOGGER.warn("skipping file: %s", inFile.toString());
            return;
        }

        Document inXml;
        try (FSDataInputStream in = fs.open(inFile.getPath())) {
            inXml = docBuilder.parse(in);
        }

        LOGGER.info("processing %s xml: %s", inXml.getDocumentElement().getNodeName(), inFile.toString());
        if (inXml.getDocumentElement().getNodeName().equals("pt_object_type_config")) {
            runOnObjectTypeConfig(inXml);
        } else if (inXml.getDocumentElement().getNodeName().equals("ontology_resource_config")) {
            runOnOntologyResourceConfig(fs, outPath, inXml);
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

    private void runOnOntologyResourceConfig(FileSystem fs, Path outPath, Document inXml) throws IOException {
        String path = getXmlString(inXml, "/ontology_resource_config/path");
        String contents = getXmlString(inXml, "/ontology_resource_config/contents");

        if (path.startsWith("/")) {
            path = path.substring(1);
        }

        byte[] data = Base64.decodeBase64(contents);

        try (FSDataOutputStream out = fs.create(new Path(outPath, path))) {
            out.write(data);
        }
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
            list = new ArrayList<>();
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
            List<Element> results = new ArrayList<>();
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
