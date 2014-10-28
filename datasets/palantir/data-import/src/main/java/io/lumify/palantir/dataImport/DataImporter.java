package io.lumify.palantir.dataImport;

import io.lumify.core.exception.LumifyException;
import io.lumify.palantir.dataImport.model.PtLinkType;
import io.lumify.palantir.dataImport.model.PtNodeDisplayType;
import io.lumify.palantir.dataImport.model.PtObjectType;
import io.lumify.palantir.dataImport.model.PtPropertyType;
import io.lumify.palantir.dataImport.sqlrunner.SqlRunner;
import org.apache.commons.io.FileUtils;
import org.securegraph.Authorizations;
import org.securegraph.Graph;
import org.securegraph.Visibility;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class DataImporter {
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
    private String owlPrefix;
    private final List<PtImporterBase> importers = new ArrayList<PtImporterBase>();

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
//        importers.add(new PtObjectTypeImporter(this));
//        importers.add(new PtPropertyTypeImporter(this));
        importers.add(new PtLinkTypeImporter(this));
//        importers.add(new PtNodeDisplayTypeImporter(this));
//        importers.add(new PtImageInfoImporter(this));
//        importers.add(new PtOntologyResourceImporter(this));
//        importers.add(new PtLinkRelationImporter(this));
//        importers.add(new PtObjectImporter(this));
//        importers.add(new PtPropertyAndValueImporter(this));
        importers.add(new PtObjectObjectImporter(this));

        File f = null;
        if (outputDirectory != null) {
            f = new File(outputDirectory);
        }
        this.outputDirectory = f;

        this.visibility = visibility;
        this.authorizations = authorizations;
        sqlRunner = new SqlRunner(connectionString, username, password, tableNamespace);
        this.idPrefix = idPrefix;
        this.owlPrefix = owlPrefix;
        this.graph = graph;
    }

    public void run() throws ClassNotFoundException, SQLException, IOException, ExecutionException {
        sqlRunner.connect();
        try {
            for (PtImporterBase importer : importers) {
                importer.run();
            }
        } finally {
            sqlRunner.close();
        }
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

    public Authorizations getAuthorizations() {
        return this.authorizations;
    }

    public String getOwlPrefix() {
        return this.owlPrefix;
    }

    public String getIdPrefix() {
        return idPrefix;
    }
}
