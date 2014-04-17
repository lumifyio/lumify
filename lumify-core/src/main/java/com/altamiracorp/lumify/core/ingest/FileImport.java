package com.altamiracorp.lumify.core.ingest;

import com.altamiracorp.lumify.core.bootstrap.InjectHelper;
import com.altamiracorp.lumify.core.model.properties.LumifyProperties;
import com.altamiracorp.lumify.core.model.properties.RawLumifyProperties;
import com.altamiracorp.lumify.core.model.workQueue.WorkQueueRepository;
import com.altamiracorp.lumify.core.security.LumifyVisibility;
import com.altamiracorp.lumify.core.security.LumifyVisibilityProperties;
import com.altamiracorp.lumify.core.security.VisibilityTranslator;
import com.altamiracorp.lumify.core.util.*;
import com.altamiracorp.securegraph.*;
import com.altamiracorp.securegraph.mutation.ElementMutation;
import com.altamiracorp.securegraph.property.StreamingPropertyValue;
import com.google.inject.Inject;
import org.apache.commons.io.FilenameUtils;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

import static com.altamiracorp.securegraph.util.IterableUtils.toList;

public class FileImport {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(FileImport.class);
    private final VisibilityTranslator visibilityTranslator;
    private List<FileImportSupportingFileHandler> fileImportSupportingFileHandlers;
    private Graph graph;
    private WorkQueueRepository workQueueRepository;

    @Inject
    public FileImport(VisibilityTranslator visibilityTranslator) {
        this.visibilityTranslator = visibilityTranslator;
    }

    public List<Vertex> importDirectory(File dataDir, boolean queueDuplicates, String visibilitySource, String workspaceId, Authorizations authorizations) throws IOException {
        ensureInitialized();

        ArrayList<Vertex> results = new ArrayList<Vertex>();

        LOGGER.debug("Importing files from %s", dataDir);
        File[] files = dataDir.listFiles();
        if (files == null || files.length == 0) {
            return results;
        }

        int totalFileCount = files.length;
        int fileCount = 0;
        int importedFileCount = 0;
        try {
            for (File f : files) {
                if (f.getName().startsWith(".") || f.length() == 0) {
                    continue;
                }
                if (isSupportingFile(f)) {
                    continue;
                }

                LOGGER.debug("Importing file (%d/%d): %s", fileCount + 1, totalFileCount, f.getAbsolutePath());
                try {
                    Vertex vertex = importFile(f, queueDuplicates, visibilitySource, workspaceId, authorizations);
                    results.add(vertex);
                    importedFileCount++;
                } catch (Exception ex) {
                    LOGGER.error("Could not import %s", f.getAbsolutePath(), ex);
                }
                fileCount++;
            }
        } finally {
            graph.flush();
        }

        LOGGER.debug(String.format("Imported %d, skipped %d files from %s", importedFileCount, fileCount - importedFileCount, dataDir));
        return results;
    }

    private boolean isSupportingFile(File f) {
        for (FileImportSupportingFileHandler fileImportSupportingFileHandler : this.fileImportSupportingFileHandlers) {
            if (fileImportSupportingFileHandler.isSupportingFile(f)) {
                return true;
            }
        }
        return false;
    }

    public Vertex importFile(File f, boolean queueDuplicates, String visibilitySource, String workspaceId, Authorizations authorizations) throws Exception {
        ensureInitialized();

        String hash = calculateFileHash(f);

        Vertex vertex = findExistingVertexWithHash(hash, authorizations);
        if (vertex != null) {
            LOGGER.warn("vertex already exists with hash %s", hash);
            if (queueDuplicates) {
                pushOnQueue(vertex);
            }
            return vertex;
        }

        List<FileImportSupportingFileHandler.AddSupportingFilesResult> addSupportingFilesResults = new ArrayList<FileImportSupportingFileHandler.AddSupportingFilesResult>();

        FileInputStream fileInputStream = new FileInputStream(f);
        try {
            StreamingPropertyValue rawValue = new StreamingPropertyValue(fileInputStream, byte[].class);
            rawValue.searchIndex(false);

            JSONObject visibilityJson = GraphUtil.updateVisibilitySourceAndAddWorkspaceId(null, visibilitySource, workspaceId);
            LumifyVisibility lumifyVisibility = this.visibilityTranslator.toVisibility(visibilityJson);
            Visibility visibility = lumifyVisibility.getVisibility();
            Map<String, Object> propertyMetadata = new HashMap<String, Object>();
            LumifyVisibilityProperties.VISIBILITY_JSON_PROPERTY.setMetadata(propertyMetadata, visibilityJson);

            VertexBuilder vertexBuilder = this.graph.prepareVertex(visibility, authorizations);
            LumifyVisibilityProperties.VISIBILITY_JSON_PROPERTY.setProperty(vertexBuilder, visibilityJson, visibility);
            RawLumifyProperties.RAW.setProperty(vertexBuilder, rawValue, propertyMetadata, visibility);
            LumifyProperties.TITLE.setProperty(vertexBuilder, f.getName(), propertyMetadata, visibility);
            LumifyProperties.ROW_KEY.setProperty(vertexBuilder, hash, propertyMetadata, visibility);
            RawLumifyProperties.FILE_NAME.setProperty(vertexBuilder, f.getName(), propertyMetadata, visibility);
            RawLumifyProperties.FILE_NAME_EXTENSION.setProperty(vertexBuilder, FilenameUtils.getExtension(f.getName()), propertyMetadata, visibility);
            RawLumifyProperties.CREATE_DATE.setProperty(vertexBuilder, new Date(f.lastModified()), propertyMetadata, visibility);

            for (FileImportSupportingFileHandler fileImportSupportingFileHandler : this.fileImportSupportingFileHandlers) {
                FileImportSupportingFileHandler.AddSupportingFilesResult addSupportingFilesResult = fileImportSupportingFileHandler.addSupportingFiles(vertexBuilder, f, visibility);
                if (addSupportingFilesResult != null) {
                    addSupportingFilesResults.add(addSupportingFilesResult);
                }
            }

            vertex = vertexBuilder.save();
            graph.flush();
            LOGGER.debug("File %s imported. vertex id: %s", f.getAbsolutePath(), vertex.getId().toString());
            pushOnQueue(vertex);
            return vertex;
        } finally {
            fileInputStream.close();
            for (FileImportSupportingFileHandler.AddSupportingFilesResult addSupportingFilesResult : addSupportingFilesResults) {
                addSupportingFilesResult.close();
            }
        }
    }

    private void ensureInitialized() {
        if (fileImportSupportingFileHandlers == null) {
            fileImportSupportingFileHandlers = toList(ServiceLoaderUtil.load(FileImportSupportingFileHandler.class));
            for (FileImportSupportingFileHandler fileImportSupportingFileHandler : fileImportSupportingFileHandlers) {
                InjectHelper.inject(fileImportSupportingFileHandler);
            }
        }
    }

    private void pushOnQueue(Vertex vertex) {
        LOGGER.debug("pushing %s on to %s queue", vertex.getId().toString(), WorkQueueRepository.GRAPH_PROPERTY_QUEUE_NAME);
        this.workQueueRepository.pushGraphPropertyQueue(vertex.getId(), ElementMutation.DEFAULT_KEY, RawLumifyProperties.RAW.getKey());
    }

    private Vertex findExistingVertexWithHash(String hash, Authorizations authorizations) {
        Iterator<Vertex> existingVertices = this.graph.query(authorizations)
                .has(LumifyProperties.ROW_KEY.getKey(), hash)
                .vertices()
                .iterator();
        if (existingVertices.hasNext()) {
            return existingVertices.next();
        }
        return null;
    }

    private String calculateFileHash(File f) throws IOException {
        FileInputStream fileInputStream = new FileInputStream(f);
        try {
            return RowKeyHelper.buildSHA256KeyString(fileInputStream);
        } finally {
            fileInputStream.close();
        }
    }

    @Inject
    public void setGraph(Graph graph) {
        this.graph = graph;
    }

    @Inject
    public void setWorkQueueRepository(WorkQueueRepository workQueueRepository) {
        this.workQueueRepository = workQueueRepository;
    }
}
