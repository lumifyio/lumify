package io.lumify.palantir.dataImport;

import io.lumify.core.exception.LumifyException;
import io.lumify.core.model.properties.LumifyProperties;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import io.lumify.palantir.dataImport.model.PtObject;
import io.lumify.palantir.dataImport.model.PtObjectType;
import io.lumify.palantir.dataImport.sqlrunner.SqlRunner;
import org.securegraph.Authorizations;
import org.securegraph.Graph;
import org.securegraph.VertexBuilder;
import org.securegraph.Visibility;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class DataImporter {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(DataImporter.class);
    private final Graph graph;
    private final String idPrefix;
    private Connection connection;
    private final SqlRunner sqlRunner;
    private final Visibility visibility;
    private final Authorizations authorizations;
    private final Map<Long, PtObjectType> objectTypes = new HashMap<Long, PtObjectType>();
    private String conceptTypePrefix;

    public DataImporter(String connectionString, String username, String password, String tableNamespace, String idPrefix, String conceptTypePrefix, Graph graph, Visibility visibility, Authorizations authorizations) {
        this.visibility = visibility;
        this.authorizations = authorizations;
        sqlRunner = new SqlRunner(connectionString, username, password, tableNamespace);
        this.idPrefix = idPrefix;
        this.conceptTypePrefix = conceptTypePrefix;
        this.graph = graph;
    }

    public void run() throws ClassNotFoundException, SQLException {
        sqlRunner.connect();
        try {
            loadObjectTypeCache();
            loadObjects();
        } finally {
            sqlRunner.close();
        }
    }

    private void loadObjectTypeCache() {
        Iterable<PtObjectType> ptObjectTypes = sqlRunner.select("select * from {namespace}.PT_OBJECT_TYPE", PtObjectType.class);
        for (PtObjectType ptObjectType : ptObjectTypes) {
            objectTypes.put(ptObjectType.getType(), ptObjectType);
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

    private String getConceptTypeUri(String uri) {
        return conceptTypePrefix + uri;
    }

    private String getObjectId(PtObject ptObject) {
        return idPrefix + ptObject.getObjectId();
    }
}
