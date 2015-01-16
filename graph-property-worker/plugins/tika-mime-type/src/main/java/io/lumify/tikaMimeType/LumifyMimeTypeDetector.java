package io.lumify.tikaMimeType;

import io.lumify.core.exception.LumifyException;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import org.apache.commons.io.FilenameUtils;
import org.apache.tika.detect.DefaultDetector;
import org.apache.tika.detect.Detector;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LumifyMimeTypeDetector implements Detector {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(LumifyMimeTypeDetector.class);
    public static final String EXT_TO_MIME_TYPE_MAPPING_FILE = "extToMimeTypeMapping.txt";
    public static final String METADATA_FILENAME = "fileName";
    private final DefaultDetector defaultDetector;
    private static final Map<String, String> extToMimeTypeMapping = loadExtToMimeTypeMappingFile();

    public LumifyMimeTypeDetector() {
        defaultDetector = new DefaultDetector();
    }

    @Override
    public MediaType detect(InputStream input, Metadata metadata) throws IOException {
        String fileName = metadata.get(METADATA_FILENAME);
        if (fileName != null) {
            String mimeType = URLConnection.guessContentTypeFromName(fileName);
            if (mimeType != null) {
                return toMediaType(mimeType);
            }

            MediaType mediaType = setContentTypeUsingFileExt(FilenameUtils.getExtension(fileName).toLowerCase());
            if (mediaType != null) {
                return mediaType;
            }
        }

        return defaultDetector.detect(input, metadata);
    }

    private static Map<String, String> loadExtToMimeTypeMappingFile() {
        Map<String, String> results = new HashMap<>();
        try {
            InputStream in = LumifyMimeTypeDetector.class.getResourceAsStream(EXT_TO_MIME_TYPE_MAPPING_FILE);
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            Pattern linePattern = Pattern.compile("(.+)\\s+(.+)");
            String line;
            while ((line = reader.readLine()) != null) {
                Matcher m = linePattern.matcher(line);
                if (!m.matches()) {
                    LOGGER.warn("Invalid line in mime type mapping file: %s", line);
                    continue;
                }
                String ext = m.group(1).trim().toLowerCase();
                String mimeType = m.group(2).trim();
                if (ext.startsWith(".")) {
                    ext = ext.substring(1);
                }

                // take the first entry because the second entry is the alternative mime type
                if (!results.containsKey(ext)) {
                    results.put(ext, mimeType);
                }
            }
            in.close();
        } catch (IOException ex) {
            throw new LumifyException("Could not load " + EXT_TO_MIME_TYPE_MAPPING_FILE);
        }
        return results;
    }

    private MediaType setContentTypeUsingFileExt(String fileExt) {
        if (extToMimeTypeMapping.containsKey(fileExt)) {
            return toMediaType(extToMimeTypeMapping.get(fileExt));
        }
        return null;
    }

    private MediaType toMediaType(String str) {
        String[] parts = str.split("/");
        return new MediaType(parts[0], parts[1]);
    }
}
