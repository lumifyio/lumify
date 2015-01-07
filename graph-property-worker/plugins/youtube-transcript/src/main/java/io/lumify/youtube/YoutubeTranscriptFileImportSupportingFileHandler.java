package io.lumify.youtube;

import io.lumify.core.ingest.FileImportSupportingFileHandler;
import io.lumify.core.model.properties.types.StreamingLumifyProperty;
import org.securegraph.VertexBuilder;
import org.securegraph.Visibility;
import org.securegraph.property.StreamingPropertyValue;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class YoutubeTranscriptFileImportSupportingFileHandler extends FileImportSupportingFileHandler {
    public static final String YOUTUBE_CC_FILE_NAME_SUFFIX = ".youtubecc";
    public static final StreamingLumifyProperty YOUTUBE_CC = new StreamingLumifyProperty("http://lumify.io#youtubecc");

    @Override
    public boolean isSupportingFile(File f) {
        return f.getName().endsWith(YOUTUBE_CC_FILE_NAME_SUFFIX);
    }

    @Override
    public AddSupportingFilesResult addSupportingFiles(VertexBuilder vertexBuilder, File f, Visibility visibility) throws Exception {
        File mappingJsonFile = new File(f.getParentFile(), f.getName() + YOUTUBE_CC_FILE_NAME_SUFFIX);
        if (mappingJsonFile.exists()) {
            final FileInputStream youtubeccIn = new FileInputStream(mappingJsonFile);
            StreamingPropertyValue youtubeValue = new StreamingPropertyValue(youtubeccIn, byte[].class);
            youtubeValue.searchIndex(false);
            YOUTUBE_CC.setProperty(vertexBuilder, youtubeValue, visibility);
            return new AddSupportingFilesResult() {
                @Override
                public void close() throws IOException {
                    youtubeccIn.close();
                }
            };
        }
        return null;
    }
}
