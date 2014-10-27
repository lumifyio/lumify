package io.lumify.palantir.dataImport;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import io.lumify.core.exception.LumifyException;
import io.lumify.core.model.properties.LumifyProperties;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import io.lumify.palantir.dataImport.model.*;
import io.lumify.palantir.dataImport.sqlrunner.SqlRunner;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.securegraph.Authorizations;
import org.securegraph.Graph;
import org.securegraph.VertexBuilder;
import org.securegraph.Visibility;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DataImporter {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(DataImporter.class);
    private static Pattern VALUE_BODY_PATTERN = Pattern.compile("^<VALUE>(.*)</VALUE>$", Pattern.DOTALL);
    private static Pattern UNPARSED_VALUE_BODY_PATTERN = Pattern.compile("^<UNPARSED_VALUE>(.*)</UNPARSED_VALUE>$", Pattern.DOTALL);
    private static Pattern VALUE_SUBSTITUTION = Pattern.compile("\\{(.*?)\\}");
    private final Graph graph;
    private final String idPrefix;
    private Connection connection;
    private final SqlRunner sqlRunner;
    private final File outputDirectory;
    private final Visibility visibility;
    private final Authorizations authorizations;
    private final Map<Long, PtObjectType> objectTypes = new HashMap<Long, PtObjectType>();
    private final Map<Long, PtPropertyType> propertyTypes = new HashMap<Long, PtPropertyType>();
    private final Map<Long, PtLinkType> linkTypes = new HashMap<Long, PtLinkType>();
    private final Map<Long, PtNodeDisplayType> nodeDisplayTypes = new HashMap<Long, PtNodeDisplayType>();
    private String owlPrefix;
    private static final DocumentBuilder dBuilder;

    static {
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            dBuilder = dbFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new LumifyException("Could not create document builder", e);
        }
    }

    public DataImporter(
            String connectionString,
            String username,
            String password,
            String tableNamespace,
            String idPrefix,
            String owlPrefix,
            String outputDirectory,
            Graph graph,
            Visibility visibility,
            Authorizations authorizations) {
        File f = null;
        if (outputDirectory != null) {
            f = new File(outputDirectory);
            f.mkdirs();
        }
        this.outputDirectory = f;

        this.visibility = visibility;
        this.authorizations = authorizations;
        sqlRunner = new SqlRunner(connectionString, username, password, tableNamespace);
        this.idPrefix = idPrefix;
        this.owlPrefix = owlPrefix;
        this.graph = graph;
    }

    public void run() throws ClassNotFoundException, SQLException, IOException {
        sqlRunner.connect();
        try {
            loadObjectTypeCache();
            loadPropertyTypeCache();
            loadLinkTypeCache();
            loadNodeDisplayTypeCache();
            loadImageInfos();
            loadOntologyResources();
            loadLinkRelations();

            loadObjects();
            loadProperties();
        } finally {
            sqlRunner.close();
        }
    }

    private void loadObjectTypeCache() throws IOException {
        LOGGER.info("loadObjectTypeCache");
        Iterable<PtObjectType> ptObjectTypes = sqlRunner.select("select * from {namespace}.PT_OBJECT_TYPE", PtObjectType.class);
        for (PtObjectType ptObjectType : ptObjectTypes) {
            objectTypes.put(ptObjectType.getType(), ptObjectType);

            if (this.outputDirectory != null) {
                String fileName = ptObjectType.getUri().replace('.', '/');
                File f = new File(this.outputDirectory, "OntologyXML/" + fileName + ".xml");
                f.getParentFile().mkdirs();
                FileUtils.write(f, ptObjectType.getConfig());
            }
        }
    }

    private void loadPropertyTypeCache() throws IOException {
        LOGGER.info("loadPropertyTypeCache");
        Iterable<PtPropertyType> ptPropertyTypes = sqlRunner.select("select * from {namespace}.PT_PROPERTY_TYPE", PtPropertyType.class);
        for (PtPropertyType ptPropertyType : ptPropertyTypes) {
            propertyTypes.put(ptPropertyType.getType(), ptPropertyType);

            if (this.outputDirectory != null) {
                String fileName = ptPropertyType.getUri().replace('.', '/');
                File f = new File(this.outputDirectory, "OntologyXML/" + fileName + ".xml");
                f.getParentFile().mkdirs();
                FileUtils.write(f, ptPropertyType.getConfig());
            }
        }
    }

    private void loadLinkTypeCache() throws IOException {
        LOGGER.info("loadLinkTypeCache");
        Iterable<PtLinkType> ptLinkTypes = sqlRunner.select("select * from {namespace}.PT_LINK_TYPE", PtLinkType.class);
        for (PtLinkType ptLinkType : ptLinkTypes) {
            linkTypes.put(ptLinkType.getType(), ptLinkType);

            if (this.outputDirectory != null) {
                String fileName = ptLinkType.getUri().replace('.', '/');
                File f = new File(this.outputDirectory, "OntologyXML/" + fileName + ".xml");
                f.getParentFile().mkdirs();
                FileUtils.write(f, ptLinkType.getConfig());
            }
        }
    }

    private void loadNodeDisplayTypeCache() throws IOException {
        LOGGER.info("loadNodeDisplayTypeCache");
        Iterable<PtNodeDisplayType> ptNodeDisplayTypes = sqlRunner.select("select * from {namespace}.PT_NODE_DISPLAY_TYPE", PtNodeDisplayType.class);
        for (PtNodeDisplayType ptNodeDisplayType : ptNodeDisplayTypes) {
            nodeDisplayTypes.put(ptNodeDisplayType.getId(), ptNodeDisplayType);

            if (this.outputDirectory != null) {
                String fileName = ptNodeDisplayType.getUri().replace('.', '/');
                File f = new File(this.outputDirectory, "OntologyXML/" + fileName + ".xml");
                f.getParentFile().mkdirs();
                FileUtils.write(f, ptNodeDisplayType.getConfig());
            }
        }
    }

    private void loadLinkRelations() throws IOException {
        LOGGER.info("loadLinkRelations");
        Iterable<PtLinkRelation> ptLinkRelations = sqlRunner.select("select * from {namespace}.PT_LINK_RELATION", PtLinkRelation.class);
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" ?>\n");
        xml.append("<link_relations>\n");
        for (PtLinkRelation ptLinkRelation : ptLinkRelations) {
            xml.append("  <link_relation_config>\n");
            xml.append("    <tableType1>" + ptLinkRelation.getTableTypeId1() + "</tableType1>\n");
            xml.append("    <uri1>" + ptLinkRelation.getUri1() + "</uri1>\n");
            xml.append("    <tableType2>" + ptLinkRelation.getTableTypeId2() + "</tableType2>\n");
            xml.append("    <uri2>" + ptLinkRelation.getUri2() + "</uri2>\n");
            xml.append("    <linkUri>" + ptLinkRelation.getLinkUri() + "</linkUri>\n");
            xml.append("    <linkStatus>" + ptLinkRelation.getLinkStatus() + "</linkStatus>\n");
            xml.append("    <hidden>" + ptLinkRelation.isHidden() + "</hidden>\n");
            xml.append("  </link_relation_config>\n");
        }
        xml.append("</link_relations>\n");

        File f = new File(this.outputDirectory, "pt_link_relation.xml");
        f.getParentFile().mkdirs();
        FileUtils.write(f, xml.toString());
    }

    private void loadImageInfos() throws IOException {
        LOGGER.info("loadImageInfos");
        Iterable<PtImageInfo> ptImageInfos = sqlRunner.select("select * from {namespace}.PT_IMAGE_INFO", PtImageInfo.class);
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" ?>\n");
        xml.append("<image_infos>\n");
        for (PtImageInfo ptImageInfo : ptImageInfos) {
            xml.append("  <image_info_config>\n");
            xml.append("    <name>" + ptImageInfo.getName() + "</name>\n");
            xml.append("    <uri>" + ptImageInfo.getUri() + "</uri>\n");
            xml.append("    <description>" + ptImageInfo.getDescription() + "</description>\n");
            xml.append("    <path>" + ptImageInfo.getPath() + "</path>\n");
            xml.append("  </image_info_config>\n");
        }
        xml.append("</image_infos>\n");

        File f = new File(this.outputDirectory, "pt_image_info.xml");
        f.getParentFile().mkdirs();
        FileUtils.write(f, xml.toString());
    }

    private void loadOntologyResources() throws IOException {
        LOGGER.info("loadOntologyResources");
        Iterable<PtOntologyResource> ptOntologyResources = sqlRunner.select("select * from {namespace}.PT_ONTOLOGY_RESOURCE", PtOntologyResource.class);
        for (PtOntologyResource ptOntologyResource : ptOntologyResources) {
            StringBuilder xml = new StringBuilder();

            String contentsBase64 = Base64.encodeBase64String(ptOntologyResource.getContents());
            contentsBase64 = Joiner.on('\n').join(Splitter.fixedLength(76).split(contentsBase64));

            xml.append("<?xml version=\"1.0\" ?>\n");
            xml.append("<ontology_resource_config>\n");
            xml.append("  <type>" + ptOntologyResource.getType() + "</type>\n");
            xml.append("  <path>" + ptOntologyResource.getPath() + "</path>\n");
            xml.append("  <deleted>" + ptOntologyResource.isDeleted() + "</deleted>\n");
            xml.append("  <contents>" + contentsBase64 + "</contents>\n");
            xml.append("</ontology_resource_config>\n");

            File f = new File(this.outputDirectory, "image/OntologyResource" + ptOntologyResource.getId() + ".xml");
            f.getParentFile().mkdirs();
            FileUtils.write(f, xml.toString());
        }
    }

    private void loadObjects() {
        int objectCount = 0;
        Iterable<PtObject> ptObjects = sqlRunner.select("select * from {namespace}.PT_OBJECT", PtObject.class);
        for (PtObject ptObject : ptObjects) {
            if (objectCount % 1000 == 0) {
                LOGGER.debug("Importing object: %d", objectCount);
                graph.flush();
            }

            PtObjectType ptObjectType = objectTypes.get(ptObject.getType());
            if (ptObjectType == null) {
                throw new LumifyException("Could not find object type: " + ptObject.getType());
            }
            String conceptTypeUri = getConceptTypeUri(ptObjectType.getUri());

            VertexBuilder v = graph.prepareVertex(getObjectId(ptObject), visibility);
            LumifyProperties.CONCEPT_TYPE.setProperty(v, conceptTypeUri, visibility);
            v.save(authorizations);

            objectCount++;
        }
        LOGGER.info("Imported %d objects", objectCount);
    }

    private void loadProperties() {
        int propertyValueCount = 0;
        Iterable<PtPropertyAndValue> ptPropertyAndValues = sqlRunner.select(
                "select p.ID" +
                        ", p.REALM_ID" +
                        ", p.LINK_OBJECT_ID" +
                        ", p.DATA_EVENT_ID" +
                        ", p.ORIGIN_DATA_EVENT_ID" +
                        ", p.DELETED" +
                        ", p.PROPERTY_VALUE_ID" +
                        ", p.CROSS_RESOLUTION_ID" +
                        ", p.ACCESS_CONTROL_LIST_ID" +
                        ", p.LAST_MODIFIED_BY" +
                        ", p.LAST_MODIFIED" +
                        ", pv.TYPE" +
                        ", pv.VALUE" +
                        ", pv.LINK_ROLE_ID" +
                        ", pv.LINK_TYPE" +
                        ", pv.PRIORITY" +
                        ", pv.USER_DISABLED_KEYWORD" +
                        ", pv.CUSTOM_KEYWORD_TERM" +
                        ", pv.GEOMETRY_XML" +
                        ", pv.TIME_START" +
                        ", pv.TIME_END" +
                        ", pv.PROPERTY_STATUS" +
                        ", pv.CREATED_BY" +
                        ", pv.TIME_CREATED" +
                        ", pv.GEOMETRY_GIS" +
                        " FROM CLEATPR.PT_PROPERTY p, CLEATPR.PT_PROPERTY_VALUE pv" +
                        " WHERE p.PROPERTY_VALUE_ID = pv.ID",
                PtPropertyAndValue.class);
        for (PtPropertyAndValue ptPropertyAndValue : ptPropertyAndValues) {
            if (propertyValueCount % 1000 == 0) {
                LOGGER.debug("Importing property: %d", propertyValueCount);
                graph.flush();
            }

            PtPropertyType propertyType = propertyTypes.get(ptPropertyAndValue.getType());
            if (propertyType == null) {
                throw new LumifyException("Could not find property type: " + ptPropertyAndValue.getType());
            }

            String propertyKey = idPrefix + ptPropertyAndValue.getPropertyValueId();
            String propertyName = getPropertyName(propertyType.getUri());
            Object propertyValue = toValue(ptPropertyAndValue.getValue(), propertyType);

            if (propertyValue == null) {
                // skip null values
            } else {
                VertexBuilder v = graph.prepareVertex(getObjectId(ptPropertyAndValue), visibility);
                v.addPropertyValue(propertyKey, propertyName, propertyValue, visibility);
                v.save(authorizations);
            }

            propertyValueCount++;
        }
        LOGGER.info("Imported %d property/value", propertyValueCount);
    }

    private Object toValue(String value, PtPropertyType propertyType) {
        if (value == null) {
            return null;
        }

        value = value.trim();

        if (value.equals("<null></null>")) {
            return null;
        }

        Matcher m = VALUE_BODY_PATTERN.matcher(value);
        if (m.matches()) {
            value = m.group(1).trim();
        }

        m = UNPARSED_VALUE_BODY_PATTERN.matcher(value);
        if (m.matches()) {
            return m.group(1).trim();
        }

        if (propertyType.getDisplayFormulas().size() > 0) {
            Map<String, String> values = getValuesFromValue(value);
            String formattedValue = formatValues(values, propertyType);
            if (formattedValue != null) {
                return formattedValue;
            }
        }

        return value;
    }

    private String formatValues(Map<String, String> values, PtPropertyType propertyType) {
        for (String displayFormula : propertyType.getDisplayFormulas()) {
            String r = formatValues(values, displayFormula);
            if (r != null) {
                return r;
            }
        }
        return null;
    }

    private String formatValues(Map<String, String> values, String displayFormula) {
        StringBuffer output = new StringBuffer();
        Matcher matcher = VALUE_SUBSTITUTION.matcher(displayFormula);
        while (matcher.find()) {
            String expr = matcher.group(1);
            String[] exprParts = expr.split(",");
            String v = values.get(exprParts[0]);
            if (v == null) {
                return null; // could not find a value to match replacement
            }
            matcher.appendReplacement(output, v);
        }
        matcher.appendTail(output);
        return output.toString();
    }

    private Map<String, String> getValuesFromValue(String value) {
        try {
            Document d = dBuilder.parse(new ByteArrayInputStream(("<v>" + value + "</v>").getBytes()));
            Map<String, String> values = new HashMap<String, String>();
            NodeList childNodes = d.getDocumentElement().getChildNodes();
            for (int i = 0; i < childNodes.getLength(); i++) {
                Node childNode = childNodes.item(i);
                if (childNode instanceof Element) {
                    Element e = (Element) childNode;
                    values.put(e.getTagName(), e.getTextContent());
                }
            }
            return values;
        } catch (Exception e) {
            throw new LumifyException("Could not parse value into values: " + value, e);
        }
    }

    private String getConceptTypeUri(String uri) {
        return owlPrefix + uri;
    }

    private String getPropertyName(String uri) {
        return owlPrefix + uri;
    }

    private String getObjectId(PtPropertyAndValue ptPropertyAndValue) {
        return idPrefix + ptPropertyAndValue.getLinkObjectId();
    }

    private String getObjectId(PtObject ptObject) {
        return idPrefix + ptObject.getObjectId();
    }
}
