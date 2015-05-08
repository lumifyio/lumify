package io.lumify.core.ingest;

import io.lumify.core.model.properties.LumifyProperties;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.securegraph.VertexBuilder;
import org.securegraph.Visibility;
import org.securegraph.property.StreamingPropertyValue;

/**
 * This handler looks for files of the format `${filename}.mapping.json` and,
 * if found, attaches the content of that file as the streaming property
 * value `mappingJson`.
 */
public class MappingFileImportSupportingFileHandler extends FileImportSupportingFileHandler {
    private static final List<String> WORK_PROPERTIES = Arrays.asList(LumifyProperties.MAPPING_JSON.getPropertyName());
    private static final String MAPPING_JSON_FILE_NAME_SUFFIX = ".mapping.json";

    @Override
    public boolean isSupportingFile(File f) {
        return f.getName().endsWith(MAPPING_JSON_FILE_NAME_SUFFIX);
    }

    @Override
    public AddSupportingFilesResult addSupportingFiles(VertexBuilder vertexBuilder, File f, Visibility visibility) throws FileNotFoundException {
        File mappingJsonFile = getMappingFile(f);
        if (mappingJsonFile.exists()) {
            final FileInputStream mappingJsonInputStream = new FileInputStream(mappingJsonFile);
            StreamingPropertyValue mappingJsonValue = new StreamingPropertyValue(mappingJsonInputStream, byte[].class);
            mappingJsonValue.searchIndex(false);
            LumifyProperties.MAPPING_JSON.setProperty(vertexBuilder, mappingJsonValue, visibility);
            return new AddSupportingFilesResult() {
                @Override
                public List<String> getPropertiesToQueue() {
                    return WORK_PROPERTIES;
                }

                @Override
                public void close() throws IOException {
                    mappingJsonInputStream.close();
                }
            };
        }
        return null;
    }

    public static File getMappingFile(File f) {
        return new File(f.getParentFile(), f.getName() + MAPPING_JSON_FILE_NAME_SUFFIX);
    }
}
