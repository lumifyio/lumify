package io.lumify.tikaMimeType;

import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;

import java.io.BufferedInputStream;
import java.io.InputStream;

public class TikaMimeTypeMapper {
    private final LumifyMimeTypeDetector detector;

    public TikaMimeTypeMapper() {
        detector = new LumifyMimeTypeDetector();
    }

    public String guessMimeType(InputStream in, String fileName) throws Exception {
        Metadata metadata = new Metadata();
        metadata.set(LumifyMimeTypeDetector.METADATA_FILENAME, fileName);
        MediaType mediaType = detector.detect(new BufferedInputStream(in), metadata);
        String mimeType = mediaType.toString();
        if (mimeType != null) {
            return mimeType;
        }

        return "application/octet-stream";
    }
}
