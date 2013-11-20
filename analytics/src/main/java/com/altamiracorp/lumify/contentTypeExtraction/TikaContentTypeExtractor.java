package com.altamiracorp.lumify.contentTypeExtraction;

import org.apache.hadoop.mapreduce.Mapper;
import org.apache.tika.detect.DefaultDetector;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class TikaContentTypeExtractor implements ContentTypeExtractor {
    private static final String PROPS_FILE = "tika-extractor.properties";

    @Override
    public void setup(Mapper.Context context) {
        Properties tikaProperties = new Properties();
        try {
            InputStream propsIn = Thread.currentThread().getContextClassLoader().getResourceAsStream(PROPS_FILE);
            if (propsIn != null) {
                tikaProperties.load(propsIn);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String extract(InputStream in, String fileExt) throws Exception {
        DefaultDetector detector = new DefaultDetector();
        Metadata metadata = new Metadata();
        MediaType mediaType = detector.detect(new BufferedInputStream(in), metadata);

        String contentType = mediaType.toString();
        if (contentType == null || contentType.equals("application/octet-stream")) {
            contentType = setContentTypeUsingFileExt(fileExt.toLowerCase());
        }
        return contentType;
    }

    private String setContentTypeUsingFileExt(String fileExt) {
        if (fileExt.equals("jpeg") || fileExt.equals("tiff") || fileExt.equals("raw") || fileExt.equals("gif") ||
                fileExt.equals("bmp") || fileExt.equals("png")) {
            return "image";
        }
        if (fileExt.equals("flv") || fileExt.equals("avi") || fileExt.equals("m2v") || fileExt.equals("mov") ||
                fileExt.equals("mpg") || fileExt.equals("wmv")) {
            return "video";
        }
        return "";
    }
}
