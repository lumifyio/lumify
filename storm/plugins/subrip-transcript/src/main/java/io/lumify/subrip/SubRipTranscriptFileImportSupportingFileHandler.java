package io.lumify.subrip;

import io.lumify.core.ingest.FileImportSupportingFileHandler;
import io.lumify.core.model.properties.types.StreamingLumifyProperty;
import org.securegraph.VertexBuilder;
import org.securegraph.Visibility;
import org.securegraph.property.StreamingPropertyValue;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class SubRipTranscriptFileImportSupportingFileHandler extends FileImportSupportingFileHandler {
    public static final String SUBRIP_CC_FILE_NAME_SUFFIX = ".srt";
    public static final StreamingLumifyProperty SUBRIP_CC = new StreamingLumifyProperty("http://lumify.io#subrip");

    @Override
    public boolean isSupportingFile(File f) {
        return f.getName().endsWith(SUBRIP_CC_FILE_NAME_SUFFIX);
    }

    @Override
    public AddSupportingFilesResult addSupportingFiles(VertexBuilder vertexBuilder, File f, Visibility visibility) throws Exception {
        File mappingJsonFile = new File(f.getParentFile(), f.getName() + SUBRIP_CC_FILE_NAME_SUFFIX);
        if (mappingJsonFile.exists()) {
            final FileInputStream subripIn = new FileInputStream(mappingJsonFile);
            StreamingPropertyValue subripValue = new StreamingPropertyValue(subripIn, byte[].class);
            subripValue.searchIndex(false);
            SUBRIP_CC.setProperty(vertexBuilder, subripValue, visibility);
            return new AddSupportingFilesResult() {
                @Override
                public void close() throws IOException {
                    subripIn.close();
                }
            };
        }
        return null;
    }
}
