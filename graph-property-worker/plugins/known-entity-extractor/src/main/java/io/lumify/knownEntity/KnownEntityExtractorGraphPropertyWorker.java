package io.lumify.knownEntity;

import io.lumify.core.ingest.graphProperty.GraphPropertyWorkData;
import io.lumify.core.ingest.graphProperty.GraphPropertyWorker;
import io.lumify.core.ingest.graphProperty.GraphPropertyWorkerPrepareData;
import io.lumify.core.model.audit.AuditAction;
import io.lumify.core.model.properties.LumifyProperties;
import io.lumify.core.model.termMention.TermMentionBuilder;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import io.lumify.web.clientapi.model.VisibilityJson;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.arabidopsis.ahocorasick.AhoCorasick;
import org.arabidopsis.ahocorasick.SearchResult;
import org.securegraph.*;
import org.supercsv.io.CsvListReader;
import org.supercsv.prefs.CsvPreference;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.securegraph.util.IterableUtils.singleOrDefault;

public class KnownEntityExtractorGraphPropertyWorker extends GraphPropertyWorker {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(KnownEntityExtractorGraphPropertyWorker.class);
    public static final String PATH_PREFIX_CONFIG = "termextraction.knownEntities.pathPrefix";
    public static final String DEFAULT_PATH_PREFIX = "/lumify/config/knownEntities/";
    private static final String PROCESS = KnownEntityExtractorGraphPropertyWorker.class.getName();
    private AhoCorasick tree;
    private String artifactHasEntityIri;
    private String locationIri;
    private String organizationIri;
    private String personIri;

    @Override
    public void prepare(GraphPropertyWorkerPrepareData workerPrepareData) throws Exception {
        super.prepare(workerPrepareData);

        this.locationIri = getOntologyRepository().getRequiredConceptIRIByIntent("location");
        this.organizationIri = getOntologyRepository().getRequiredConceptIRIByIntent("organization");
        this.personIri = getOntologyRepository().getRequiredConceptIRIByIntent("person");
        this.artifactHasEntityIri = getOntologyRepository().getRequiredRelationshipIRIByIntent("artifactHasEntity");

        String pathPrefix = (String) workerPrepareData.getConfiguration().get(PATH_PREFIX_CONFIG);
        if (pathPrefix == null) {
            pathPrefix = DEFAULT_PATH_PREFIX;
        }
        FileSystem fs = workerPrepareData.getHdfsFileSystem();
        this.tree = loadDictionaries(fs, pathPrefix);
    }

    @Override
    public void execute(InputStream in, GraphPropertyWorkData data) throws Exception {
        String text = IOUtils.toString(in, "UTF-8"); // TODO convert AhoCorasick to use InputStream
        Iterator<SearchResult<Match>> searchResults = tree.search(text.toCharArray());
        Vertex sourceVertex = (Vertex) data.getElement();
        List<Vertex> termMentions = new ArrayList<>();
        while (searchResults.hasNext()) {
            SearchResult searchResult = searchResults.next();
            VisibilityJson visibilityJson = data.getVisibilitySourceJson();
            List<Vertex> newTermMentions = outputResultToTermMention(sourceVertex, searchResult, data.getProperty().getKey(), visibilityJson, data.getVisibility());
            termMentions.addAll(newTermMentions);
            getGraph().flush();
        }
        applyTermMentionFilters(sourceVertex, termMentions);
        pushTextUpdated(data);
    }

    private List<Vertex> outputResultToTermMention(Vertex sourceVertex, SearchResult<Match> searchResult, String propertyKey, VisibilityJson visibilityJson, Visibility visibility) {
        List<Vertex> termMentions = new ArrayList<>();
        for (Match match : searchResult.getOutputs()) {
            int start = searchResult.getLastIndex() - match.getMatchText().length();
            int end = searchResult.getLastIndex();
            String title = match.getEntityTitle();
            String ontologyClassUri = mapToOntologyIri(match.getConceptTitle());

            Vertex resolvedToVertex = findOrAddEntity(title, ontologyClassUri, visibility);
            Edge resolvedEdge = findOrAddEdge(sourceVertex, resolvedToVertex, visibilityJson, visibility);

            Vertex termMention = new TermMentionBuilder()
                    .sourceVertex(sourceVertex)
                    .propertyKey(propertyKey)
                    .start(start)
                    .end(end)
                    .title(title)
                    .conceptIri(ontologyClassUri)
                    .visibilityJson(visibilityJson)
                    .process(PROCESS)
                    .resolvedTo(resolvedToVertex, resolvedEdge)
                    .save(getGraph(), getVisibilityTranslator(), getAuthorizations());
            termMentions.add(termMention);
        }
        return termMentions;
    }

    protected String mapToOntologyIri(String type) {
        String ontologyClassUri;
        if ("location".equals(type)) {
            ontologyClassUri = this.locationIri;
        } else if ("organization".equals(type)) {
            ontologyClassUri = this.organizationIri;
        } else if ("person".equals(type)) {
            ontologyClassUri = this.personIri;
        } else {
            ontologyClassUri = LumifyProperties.CONCEPT_TYPE_THING;
        }
        return ontologyClassUri;
    }

    private Edge findOrAddEdge(Vertex sourceVertex, Vertex resolvedToVertex, VisibilityJson visibilityJson, Visibility visibility) {
        Edge resolvedEdge = singleOrDefault(sourceVertex.getEdges(resolvedToVertex, Direction.BOTH, getAuthorizations()), null);
        if (resolvedEdge == null) {
            EdgeBuilder resolvedEdgeBuilder = getGraph().prepareEdge(sourceVertex, resolvedToVertex, artifactHasEntityIri, visibility);
            LumifyProperties.VISIBILITY_JSON.setProperty(resolvedEdgeBuilder, visibilityJson, visibility);
            resolvedEdge = resolvedEdgeBuilder.save(getAuthorizations());
            getAuditRepository().auditRelationship(AuditAction.CREATE, sourceVertex, resolvedToVertex, resolvedEdge, PROCESS, "", getUser(), visibility);
        }
        return resolvedEdge;
    }

    private Vertex findOrAddEntity(String title, String ontologyClassUri, Visibility visibility) {
        Vertex vertex = singleOrDefault(getGraph().query(getAuthorizations())
                .has(LumifyProperties.TITLE.getPropertyName(), title)
                .has(LumifyProperties.CONCEPT_TYPE.getPropertyName(), ontologyClassUri)
                .vertices(), null);
        if (vertex != null) {
            return vertex;
        }

        VertexBuilder vertexElementMutation = getGraph().prepareVertex(visibility);
        LumifyProperties.TITLE.setProperty(vertexElementMutation, title, visibility);
        LumifyProperties.CONCEPT_TYPE.setProperty(vertexElementMutation, ontologyClassUri, visibility);
        vertex = vertexElementMutation.save(getAuthorizations());
        getGraph().flush();
        return vertex;
    }

    @Override
    public boolean isHandled(Element element, Property property) {
        if (property == null) {
            return false;
        }

        if (property.getName().equals(LumifyProperties.RAW.getPropertyName())) {
            return false;
        }

        String mimeType = LumifyProperties.MIME_TYPE.getMetadataValue(property.getMetadata(), null);
        return !(mimeType == null || !mimeType.startsWith("text"));
    }

    private static AhoCorasick loadDictionaries(FileSystem fs, String pathPrefix) throws IOException {
        AhoCorasick tree = new AhoCorasick();
        Path hdfsDirectory = new Path(pathPrefix, "dictionaries");
        if (!fs.exists(hdfsDirectory)) {
            fs.mkdirs(hdfsDirectory);
        }
        for (FileStatus dictionaryFileStatus : fs.listStatus(hdfsDirectory)) {
            Path hdfsPath = dictionaryFileStatus.getPath();
            if (hdfsPath.getName().startsWith(".") || !hdfsPath.getName().endsWith(".dict")) {
                continue;
            }
            LOGGER.info("Loading known entity dictionary %s", hdfsPath.toString());
            String conceptName = FilenameUtils.getBaseName(hdfsPath.getName());
            conceptName = URLDecoder.decode(conceptName, "UTF-8");
            try (InputStream dictionaryInputStream = fs.open(hdfsPath)) {
                addDictionaryEntriesToTree(tree, conceptName, dictionaryInputStream);
            }
        }
        tree.prepare();
        return tree;
    }

    private static void addDictionaryEntriesToTree(AhoCorasick tree, String type, InputStream dictionaryInputStream) throws IOException {
        CsvPreference csvPrefs = CsvPreference.EXCEL_PREFERENCE;
        CsvListReader csvReader = new CsvListReader(new InputStreamReader(dictionaryInputStream), csvPrefs);
        List<String> line;
        while ((line = csvReader.read()) != null) {
            if (line.size() != 2) {
                throw new RuntimeException("Invalid number of entries on a line. Expected 2 found " + line.size());
            }
            tree.add(line.get(0), new Match(type, line.get(0), line.get(1)));
        }
    }

    private static class Match {
        private final String conceptTitle;
        private final String entityTitle;
        private final String matchText;

        public Match(String type, String matchText, String entityTitle) {
            conceptTitle = type;
            this.matchText = matchText;
            this.entityTitle = entityTitle;
        }

        private String getConceptTitle() {
            return conceptTitle;
        }

        private String getEntityTitle() {
            return entityTitle;
        }

        private String getMatchText() {
            return matchText;
        }

        @Override
        public String toString() {
            return matchText;
        }
    }
}
