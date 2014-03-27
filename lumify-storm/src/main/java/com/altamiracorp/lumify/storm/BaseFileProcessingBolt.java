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
import com.altamiracorp.lumify.core.contentType.MimeTypeMapper;
import com.altamiracorp.lumify.core.util.LumifyLogger;
import com.altamiracorp.lumify.core.util.LumifyLoggerFactory;
import com.altamiracorp.lumify.storm.file.FileMetadata;
import com.altamiracorp.securegraph.Vertex;
import com.altamiracorp.securegraph.property.StreamingPropertyValue;
import com.google.common.io.Files;
import com.google.inject.Inject;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;

import javax.annotation.Nullable;
import java.io.*;

import static com.altamiracorp.lumify.core.model.properties.EntityLumifyProperties.SOURCE;
import static com.altamiracorp.lumify.core.model.properties.LumifyProperties.TITLE;
import static com.altamiracorp.lumify.core.model.properties.RawLumifyProperties.*;

/**
 * Base class for bolts that process files from HDFS.
 */
public abstract class BaseFileProcessingBolt extends BaseLumifyBolt {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(BaseFileProcessingBolt.class);

    private MimeTypeMapper mimeTypeMapper;

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
                Vertex artifactVertex = graph.getVertex(vertexId, getAuthorizations());
                if (artifactVertex == null) {
                    throw new RuntimeException("Could not find vertex with id: " + vertexId);
                }
                fileName = FILE_NAME.getPropertyValue(artifactVertex);
                mimeType = MIME_TYPE.getPropertyValue(artifactVertex);
                source = SOURCE.getPropertyValue(artifactVertex);
                title = TITLE.getPropertyValue(artifactVertex);

                StreamingPropertyValue rawPropertyValue = RAW.getPropertyValue(artifactVertex);
                raw = rawPropertyValue.getInputStream();
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
        if (mimeTypeMapper != null) {
            InputStream in = openFile(fileName);
            mimeType = mimeTypeMapper.guessMimeType(in, FilenameUtils.getExtension(fileName));
        }
        return mimeType;
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
        InputStream in = openFile(fileMetadata.getFileName());
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

    protected MimeTypeMapper getMimeTypeMapper() {
        return mimeTypeMapper;
    }

    @Inject(optional = true)
    public void setMimeTypeMapper(@Nullable MimeTypeMapper mimeTypeMapper) {
        this.mimeTypeMapper = mimeTypeMapper;
    }
}
