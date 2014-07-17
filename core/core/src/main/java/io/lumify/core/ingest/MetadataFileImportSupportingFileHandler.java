package io.lumify.core.ingest;

import io.lumify.core.model.properties.LumifyProperties;
import org.securegraph.VertexBuilder;
import org.securegraph.Visibility;
import org.securegraph.property.StreamingPropertyValue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class MetadataFileImportSupportingFileHandler extends FileImportSupportingFileHandler {
    private static final String METADATA_JSON_FILE_NAME_SUFFIX = ".metadata.json";

    @Override
    public boolean isSupportingFile(File f) {
        return f.getName().endsWith(METADATA_JSON_FILE_NAME_SUFFIX);
    }

    @Override
    public AddSupportingFilesResult addSupportingFiles(VertexBuilder vertexBuilder, File f, Visibility visibility) throws FileNotFoundException {
        File mappingJsonFile = getMetadataFile(f);
        if (mappingJsonFile.exists()) {
            final FileInputStream mappingJsonInputStream = new FileInputStream(mappingJsonFile);
            StreamingPropertyValue mappingJsonValue = new StreamingPropertyValue(mappingJsonInputStream, byte[].class);
            mappingJsonValue.searchIndex(false);
            LumifyProperties.METADATA_JSON.setProperty(vertexBuilder, mappingJsonValue, visibility);
            return new AddSupportingFilesResult() {
                @Override
                public void close() throws IOException {
                    mappingJsonInputStream.close();
                }
            };
        }
        return null;
    }

    public static File getMetadataFile(File f) {
        return new File(f.getParentFile(), f.getName() + METADATA_JSON_FILE_NAME_SUFFIX);
    }
}
