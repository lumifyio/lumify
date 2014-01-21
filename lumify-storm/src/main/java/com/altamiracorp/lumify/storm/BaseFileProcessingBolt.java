/*
 * Copyright 2013 Altamira Corporation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.altamiracorp.lumify.storm;

import backtype.storm.tuple.Tuple;
import com.altamiracorp.lumify.core.contentTypeExtraction.ContentTypeExtractor;
import com.altamiracorp.lumify.core.ingest.ArtifactExtractedInfo;
import com.altamiracorp.lumify.core.model.artifact.Artifact;
import com.altamiracorp.lumify.core.model.ontology.PropertyName;
import com.altamiracorp.lumify.core.util.LumifyLogger;
import com.altamiracorp.lumify.core.util.LumifyLoggerFactory;
import com.altamiracorp.lumify.storm.file.FileMetadata;
import com.altamiracorp.securegraph.Vertex;
import com.google.common.io.Files;
import com.google.inject.Inject;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;

import java.io.*;

/**
 * Base class for bolts that process files from HDFS.
 */
public abstract class BaseFileProcessingBolt extends BaseLumifyBolt {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(BaseFileProcessingBolt.class);

    private ContentTypeExtractor contentTypeExtractor;

    protected FileMetadata getFileMetadata(Tuple input) throws Exception {
        String fileName = input.getString(0);
        if (fileName == null || fileName.length() == 0) {
            throw new RuntimeException("Invalid item on the queue.");
        }
        String mimeType = null;
        InputStream raw = null;
        String source = null;
        String title = null;

        if (fileName.startsWith("{")) {
            JSONObject json = getJsonFromTuple(input);
            fileName = json.optString("fileName");
            mimeType = json.optString("mimeType");
            source = json.optString("source");
            title = json.optString("title");
            String rawString = json.optString("raw");

            String vertexId = json.optString("graphVertexId");
            if (vertexId != null && vertexId.length() > 0) {
                Vertex vertex = graph.getVertex(vertexId, getUser().getAuthorizations());
                if (vertex == null) {
                    throw new RuntimeException("Could not find vertex with id: " + vertexId);
                }
                String rowKey = (String) vertex.getPropertyValue(PropertyName.ROW_KEY.toString(), 0);
                Artifact artifact = artifactRepository.findByRowKey(rowKey, getUser().getModelUserContext());
                fileName = artifact.getMetadata().getFileName();
                mimeType = artifact.getMetadata().getMimeType();
                source = (String) vertex.getPropertyValue(PropertyName.SOURCE.toString(), 0);
                title = (String) vertex.getPropertyValue(PropertyName.TITLE.toString(), 0);
                raw = artifactRepository.getRaw(artifact, vertex, getUser());
            } else if (rawString != null) {
                raw = new ByteArrayInputStream(rawString.getBytes());
            }
        }
        if (mimeType == null) {
            mimeType = getMimeType(fileName);
        }

        FileMetadata fileMetadata = new FileMetadata()
                .setFileName(fileName)
                .setMimeType(mimeType)
                .setRaw(raw)
                .setSource(source);
        if (title != null) {
            fileMetadata.setTitle(title);
        } else {
            fileMetadata.setTitle(fileMetadata.getFileNameWithoutDateSuffix());
        }
        return fileMetadata;
    }

    protected String getMimeType(String fileName) throws Exception {
        String mimeType = null;
        if (contentTypeExtractor != null) {
            InputStream in = getInputStream(fileName, null);
            mimeType = contentTypeExtractor.extract(in, FilenameUtils.getExtension(fileName));
        }
        return mimeType;
    }

    protected InputStream getInputStream(final String fileName, final ArtifactExtractedInfo artifactExtractedInfo) throws Exception {
        InputStream in;
        if (getFileSize(fileName) < Artifact.MAX_SIZE_OF_INLINE_FILE) {
            InputStream rawIn = openFile(fileName);
            byte[] data;
            try {
                data = IOUtils.toByteArray(rawIn);
                if (artifactExtractedInfo != null) {
                    artifactExtractedInfo.setRaw(data);
                }
            } finally {
                rawIn.close();
            }
            in = new ByteArrayInputStream(data);
        } else {
            in = openFile(fileName);
        }
        return in;
    }

    /**
     * Extract an archive file to a local temporary directory.
     *
     * @param fileMetadata the file metadata
     * @return the temporary directory containing the archive contents
     */
    protected File extractArchive(final FileMetadata fileMetadata) throws Exception {
        File tempDir = Files.createTempDir();
        LOGGER.debug("Extracting %s to %s", fileMetadata.getFileName(), tempDir);
        InputStream in = getInputStream(fileMetadata.getFileName(), null);
        try {
            ArchiveInputStream input = new ArchiveStreamFactory().createArchiveInputStream(new BufferedInputStream(in));
            try {
                ArchiveEntry entry;
                while ((entry = input.getNextEntry()) != null) {
                    File outputFile = new File(tempDir, entry.getName());
                    OutputStream out = new FileOutputStream(outputFile);
                    try {
                        long numberOfBytesExtracted = IOUtils.copyLarge(input, out);
                        LOGGER.debug("Extracted (%d bytes) to %s", numberOfBytesExtracted, outputFile.getAbsolutePath());
                    } finally {
                        out.close();
                    }
                }
            } finally {
                input.close();
            }
        } finally {
            in.close();
        }
        return tempDir;
    }

    protected ContentTypeExtractor getContentTypeExtractor() {
        return contentTypeExtractor;
    }

    @Inject(optional = true)
    public void setContentTypeExtractor(@Nullable ContentTypeExtractor contentTypeExtractor) {
        this.contentTypeExtractor = contentTypeExtractor;
    }
}
