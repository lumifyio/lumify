package io.lumify.tikaMimeType;

import io.lumify.core.exception.LumifyException;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import org.apache.commons.io.FilenameUtils;
import org.apache.tika.detect.DefaultDetector;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;

import java.io.*;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TikaMimeTypeMapper {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(TikaMimeTypeMapper.class);
    public static final String EXT_TO_MIME_TYPE_MAPPING_FILE = "extToMimeTypeMapping.txt";
    private Map<String, String> extToMimeTypeMapping = new HashMap<String, String>();

    public TikaMimeTypeMapper() {
        loadExtToMimeTypeMappingFile();
    }

    public String guessMimeType(InputStream in, String fileName) throws Exception {
        String mimeType;
        if (fileName != null) {
            mimeType = URLConnection.guessContentTypeFromName(fileName);
            if (mimeType != null) {
                return mimeType;
            }

            mimeType = setContentTypeUsingFileExt(FilenameUtils.getExtension(fileName).toLowerCase());
            if (mimeType != null) {
                return mimeType;
            }
        }

        DefaultDetector detector = new DefaultDetector();
        Metadata metadata = new Metadata();
        MediaType mediaType = detector.detect(new BufferedInputStream(in), metadata);
        mimeType = mediaType.toString();
        if (mimeType != null) {
            return mimeType;
        }

        return "application/octet-stream";
    }

    private void loadExtToMimeTypeMappingFile() {
        try {
            InputStream in = getClass().getResourceAsStream(EXT_TO_MIME_TYPE_MAPPING_FILE);
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
                if (!extToMimeTypeMapping.containsKey(ext)) {
                    extToMimeTypeMapping.put(ext, mimeType);
                }
            }
            in.close();
        } catch (IOException ex) {
            throw new LumifyException("Could not load " + EXT_TO_MIME_TYPE_MAPPING_FILE);
        }
    }

    private String setContentTypeUsingFileExt(String fileExt) {
        if (extToMimeTypeMapping.containsKey(fileExt)) {
            return extToMimeTypeMapping.get(fileExt);
        }
        return null;
    }
}
