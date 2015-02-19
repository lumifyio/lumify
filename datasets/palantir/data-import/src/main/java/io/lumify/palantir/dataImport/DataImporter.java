package io.lumify.palantir.dataImport;

import com.google.inject.Inject;
import com.sun.org.apache.xml.internal.serialize.OutputFormat;
import com.sun.org.apache.xml.internal.serialize.XMLSerializer;
import io.lumify.core.bootstrap.InjectHelper;
import io.lumify.core.exception.LumifyException;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.model.workspace.Workspace;
import io.lumify.core.user.User;
import io.lumify.core.util.AutoDependencyTreeRunner;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import io.lumify.palantir.dataImport.model.PtLinkType;
import io.lumify.palantir.dataImport.model.PtNodeDisplayType;
import io.lumify.palantir.dataImport.model.PtObjectType;
import io.lumify.palantir.dataImport.model.PtPropertyType;
import io.lumify.palantir.dataImport.model.protobuf.AWState;
import io.lumify.palantir.dataImport.sqlrunner.SqlRunner;
import org.apache.commons.io.FileUtils;
import org.securegraph.Authorizations;
import org.securegraph.Graph;
import org.securegraph.Visibility;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class DataImporter {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(DataImporter.class);
    private final Graph graph;
    private final String idPrefix;
    private final SqlRunner sqlRunner;
    private final File outputDirectory;
    private final Visibility visibility;
    private final Authorizations authorizations;
    private final Map<Long, PtObjectType> objectTypes = new HashMap<Long, PtObjectType>();
    private final Map<Long, PtPropertyType> propertyTypes = new HashMap<Long, PtPropertyType>();
    private final Map<Long, PtLinkType> linkTypes = new HashMap<Long, PtLinkType>();
    private final Map<Long, PtNodeDisplayType> nodeDisplayTypes = new HashMap<Long, PtNodeDisplayType>();
    private final Map<Long, User> users = new HashMap<Long, User>();
    private final Map<Long, Workspace> workspaces = new HashMap<Long, Workspace>();
    private final Map<Long, AWState.Wrapper1> awstateProtosByGraphId = new HashMap<Long, AWState.Wrapper1>();
    private final AutoDependencyTreeRunner tree;
    private User systemUser;
    private UserRepository userRepository;
    private String owlPrefix;
    private final String hasMediaConceptTypeIri;

    public DataImporter(DataImporterInitParameters p) {
        InjectHelper.inject(this);

        PtObjectTypeImporter ptObjectTypeImporter = InjectHelper.inject(new PtObjectTypeImporter(this));
        PtPropertyTypeImporter ptPropertyTypeImporter = InjectHelper.inject(new PtPropertyTypeImporter(this));
        PtLinkTypeImporter ptLinkTypeImporter = InjectHelper.inject(new PtLinkTypeImporter(this));
        PtNodeDisplayTypeImporter ptNodeDisplayTypeImporter = InjectHelper.inject(new PtNodeDisplayTypeImporter(this));
        PtImageInfoImporter ptImageInfoImporter = InjectHelper.inject(new PtImageInfoImporter(this));
        PtOntologyResourceImporter ptOntologyResourceImporter = InjectHelper.inject(new PtOntologyResourceImporter(this));
        PtLinkRelationImporter ptLinkRelationImporter = InjectHelper.inject(new PtLinkRelationImporter(this));

        PtUserImporter ptUserImporter = InjectHelper.inject(new PtUserImporter(this));
        PtGraphImporter ptGraphImporter = InjectHelper.inject(new PtGraphImporter(this));
        PtObjectImporter ptObjectImporter = InjectHelper.inject(new PtObjectImporter(this));
        PtGraphObjectImporter ptGraphObjectImporter = InjectHelper.inject(new PtGraphObjectImporter(this));
        PtPropertyAndValueImporter ptPropertyAndValueImporter = InjectHelper.inject(new PtPropertyAndValueImporter(this));
        PtObjectObjectImporter ptObjectObjectImporter = InjectHelper.inject(new PtObjectObjectImporter(this));
        PtMediaAndValueImporter ptMediaAndValueImporter = InjectHelper.inject(new PtMediaAndValueImporter(this));

        tree = new AutoDependencyTreeRunner();

        //tree.add(ptObjectTypeImporter);
        //tree.add(ptPropertyTypeImporter);
//        tree.add(ptLinkTypeImporter);
//        tree.add(ptNodeDisplayTypeImporter);
//        tree.add(ptImageInfoImporter);
//        tree.add(ptOntologyResourceImporter);
//        tree.add(ptLinkRelationImporter);

        if (!p.isOntologyExport()) {
//            tree.add(ptUserImporter);
//            tree.add(ptUserImporter, ptGraphImporter);
//            tree.add(ptObjectTypeImporter, ptObjectImporter);
//            tree.add(ptGraphImporter, ptGraphObjectImporter);
//            tree.add(ptLinkTypeImporter, ptObjectObjectImporter);
            tree.add(ptPropertyTypeImporter, ptPropertyAndValueImporter);
//            tree.add(ptMediaAndValueImporter);
        }

        File f = null;
        if (p.getOutputDirectory() != null) {
            f = new File(p.getOutputDirectory());
        }
        this.outputDirectory = f;

        this.systemUser = userRepository.getSystemUser();
        this.visibility = p.getVisibility();
        this.authorizations = p.getAuthorizations();
        sqlRunner = new SqlRunner(
                p.getConnectionString(),
                p.getUsername(),
                p.getPassword(),
                p.getTableNamespace());
        this.idPrefix = p.getIdPrefix();
        this.owlPrefix = p.getOwlPrefix();
        this.hasMediaConceptTypeIri = p.getHasMediaConceptTypeIri();
        this.graph = p.getGraph();
    }

    public void run() throws ClassNotFoundException, SQLException, IOException, ExecutionException {
        long startTime = System.currentTimeMillis();
        sqlRunner.connect();
        try {
            tree.run();
        } finally {
            sqlRunner.close();
        }
        long endTime = System.currentTimeMillis();
        LOGGER.debug("complete (time: %dms)", endTime - startTime);
    }

    public SqlRunner getSqlRunner() {
        return this.sqlRunner;
    }


    public Map<Long, PtObjectType> getObjectTypes() {
        return objectTypes;
    }

    public Map<Long, PtPropertyType> getPropertyTypes() {
        return propertyTypes;
    }

    public Map<Long, PtLinkType> getLinkTypes() {
        return linkTypes;
    }

    public Map<Long, PtNodeDisplayType> getNodeDisplayTypes() {
        return nodeDisplayTypes;
    }

    public Map<Long, User> getUsers() {
        return users;
    }

    public Map<Long, Workspace> getWorkspacesByGraphId() {
        return workspaces;
    }

    public Graph getGraph() {
        return this.graph;
    }

    public Visibility getVisibility() {
        return this.visibility;
    }

    public void writeOntologyXmlFile(String uri, String data) {
        String fileName = "OntologyXML/" + uri.replace('.', '/') + ".xml";
        writeFile(fileName, data);
    }

    public void writeFile(String fileName, String data) {
        try {
            if (this.outputDirectory != null) {
                data = tryXmlFormatting(data);

                File f = new File(this.outputDirectory, fileName);
                if (!f.getParentFile().exists() && !f.getParentFile().mkdirs()) {
                    throw new LumifyException("Could not create directory: " + f.getParentFile().getAbsolutePath());
                }
                FileUtils.write(f, data);
            }
        } catch (IOException ex) {
            throw new LumifyException("Could not write file: " + fileName + " with contents: " + data, ex);
        }
    }

    private String tryXmlFormatting(String data) {
        Document doc;
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            InputSource is = new InputSource(new StringReader(data));
            doc = db.parse(is);
        } catch (Exception ex) {
            // not an xml document probably
            return data;
        }

        try {
            OutputFormat format = new OutputFormat(doc);
            format.setLineWidth(65);
            format.setIndenting(true);
            format.setIndent(2);
            Writer out = new StringWriter();
            XMLSerializer serializer = new XMLSerializer(out, format);
            serializer.serialize(doc);

            return out.toString();
        } catch (Exception ex) {
            LOGGER.warn("Could not pretty print xml: %s", data);
            return data;
        }
    }

    public Authorizations getAuthorizations() {
        return this.authorizations;
    }

    public String getOwlPrefix() {
        return this.owlPrefix;
    }

    public String getIdPrefix() {
        return idPrefix;
    }

    public String getHasMediaConceptTypeIri() {
        return hasMediaConceptTypeIri;
    }

    @Inject
    public void setUserRepository(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User getSystemUser() {
        return systemUser;
    }

    public Map<Long, AWState.Wrapper1> getAwstateProtosByGraphId() {
        return awstateProtosByGraphId;
    }
}
