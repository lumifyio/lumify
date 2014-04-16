package com.altamiracorp.lumify.core.ingest;

import com.altamiracorp.lumify.core.bootstrap.InjectHelper;
import com.altamiracorp.lumify.core.model.properties.LumifyProperties;
import com.altamiracorp.lumify.core.model.properties.RawLumifyProperties;
import com.altamiracorp.lumify.core.model.workQueue.WorkQueueRepository;
import com.altamiracorp.lumify.core.util.LumifyLogger;
import com.altamiracorp.lumify.core.util.LumifyLoggerFactory;
import com.altamiracorp.lumify.core.util.RowKeyHelper;
import com.altamiracorp.lumify.core.util.ServiceLoaderUtil;
import com.altamiracorp.securegraph.*;
import com.altamiracorp.securegraph.mutation.ElementMutation;
import com.altamiracorp.securegraph.property.StreamingPropertyValue;
import com.google.inject.Inject;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import static com.altamiracorp.securegraph.util.IterableUtils.toList;

public class FileImport {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(FileImport.class);
    private List<FileImportSupportingFileHandler> fileImportSupportingFileHandlers;
    private Graph graph;
    private WorkQueueRepository workQueueRepository;

    public void importDirectory(String dataDir, boolean queueDuplicates, Visibility visibility, Authorizations authorizations) throws IOException {
        ensureInitialized();

        LOGGER.debug("Importing files from %s", dataDir);
        File dataDirFile = new File(dataDir);
        File[] files = dataDirFile.listFiles();
        if (files == null || files.length == 0) {
            return;
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
                    if (importFile(f, queueDuplicates, visibility, authorizations)) {
                        importedFileCount++;
                    }
                } catch (Exception ex) {
                    LOGGER.error("Could not import %s", f.getAbsolutePath(), ex);
                }
                fileCount++;
            }
        } finally {
            graph.flush();
        }

        LOGGER.debug(String.format("Imported %d, skipped %d files from %s", importedFileCount, fileCount - importedFileCount, dataDirFile));
    }

    private boolean isSupportingFile(File f) {
        for (FileImportSupportingFileHandler fileImportSupportingFileHandler : this.fileImportSupportingFileHandlers) {
            if (fileImportSupportingFileHandler.isSupportingFile(f)) {
                return true;
            }
        }
        return false;
    }

    public boolean importFile(File f, boolean queueDuplicates, Visibility visibility, Authorizations authorizations) throws Exception {
        ensureInitialized();

        String hash = calculateFileHash(f);

        Vertex vertex = findExistingVertexWithHash(hash, authorizations);
        if (vertex != null) {
            LOGGER.warn("vertex already exists with hash %s", hash);
            if (queueDuplicates) {
                pushOnQueue(vertex);
            }
            return false;
        }

        List<FileImportSupportingFileHandler.AddSupportingFilesResult> addSupportingFilesResults = new ArrayList<FileImportSupportingFileHandler.AddSupportingFilesResult>();

        FileInputStream fileInputStream = new FileInputStream(f);
        try {
            StreamingPropertyValue rawValue = new StreamingPropertyValue(fileInputStream, byte[].class);
            rawValue.searchIndex(false);

            VertexBuilder vertexBuilder = this.graph.prepareVertex(visibility, authorizations);
            RawLumifyProperties.RAW.setProperty(vertexBuilder, rawValue, visibility);
            LumifyProperties.TITLE.setProperty(vertexBuilder, f.getName(), visibility);
            LumifyProperties.ROW_KEY.setProperty(vertexBuilder, hash, visibility);
            RawLumifyProperties.FILE_NAME.setProperty(vertexBuilder, f.getName(), visibility);
            RawLumifyProperties.FILE_NAME_EXTENSION.setProperty(vertexBuilder, FilenameUtils.getExtension(f.getName()), visibility);
            RawLumifyProperties.CREATE_DATE.setProperty(vertexBuilder, new Date(f.lastModified()), visibility);

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
            return true;
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
