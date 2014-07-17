package io.lumify.mapping;

import io.lumify.core.ingest.FileImportSupportingFileHandler;
import io.lumify.core.model.properties.LumifyProperties;
import org.securegraph.VertexBuilder;
import org.securegraph.Visibility;
import org.securegraph.property.StreamingPropertyValue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class MappingFileImportSupportingFileHandler extends FileImportSupportingFileHandler {
    private static final String MAPPING_JSON_FILE_NAME_SUFFIX = ".mapping.json";

    @Override
    public boolean isSupportingFile(File f) {
        return f.getName().endsWith(MAPPING_JSON_FILE_NAME_SUFFIX);
    }

    @Override
    public AddSupportingFilesResult addSupportingFiles(VertexBuilder vertexBuilder, File f, Visibility visibility) throws FileNotFoundException {
        File mappingJsonFile = new File(f.getParentFile(), f.getName() + MAPPING_JSON_FILE_NAME_SUFFIX);
        if (mappingJsonFile.exists()) {
            final FileInputStream mappingJsonInputStream = new FileInputStream(mappingJsonFile);
            StreamingPropertyValue mappingJsonValue = new StreamingPropertyValue(mappingJsonInputStream, byte[].class);
            mappingJsonValue.searchIndex(false);
            LumifyProperties.MAPPING_JSON.setProperty(vertexBuilder, mappingJsonValue, visibility);
            return new AddSupportingFilesResult() {
                @Override
                public void close() throws IOException {
                    mappingJsonInputStream.close();
                }
            };
        }
        return null;
    }
}
