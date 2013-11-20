package com.altamiracorp.lumify.contentTypeExtraction;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.assertEquals;

@RunWith(JUnit4.class)
public class TikaContentTypeExtractorTest {

    @Test
    public void textExtract() throws Exception {
        TikaContentTypeExtractor contentTypeExtractor = new TikaContentTypeExtractor();
        assertEquals("video/x-m4v", contentTypeExtractor.extract(TikaContentTypeExtractor.class.getResourceAsStream("/H_264.m4v"), "m4v"));
        assertEquals("video/mpeg", contentTypeExtractor.extract(TikaContentTypeExtractor.class.getResourceAsStream("/MPEG-2.m2v"), "m2v"));
        assertEquals("video/mp4", contentTypeExtractor.extract(TikaContentTypeExtractor.class.getResourceAsStream("/MPEG-4.mp4"), "mp4"));
        assertEquals("text/plain", contentTypeExtractor.extract(TikaContentTypeExtractor.class.getResourceAsStream("/hello.txt"), "txt"));
        assertEquals("application/pdf", contentTypeExtractor.extract(TikaContentTypeExtractor.class.getResourceAsStream("/hello.pdf"), "pdf"));
        assertEquals("image/jpeg", contentTypeExtractor.extract(TikaContentTypeExtractor.class.getResourceAsStream("/cat.jpg"), "jpg"));
        assertEquals("", contentTypeExtractor.extract(TikaContentTypeExtractor.class.getResourceAsStream("/test.abc"), "abc"));
    }
}
