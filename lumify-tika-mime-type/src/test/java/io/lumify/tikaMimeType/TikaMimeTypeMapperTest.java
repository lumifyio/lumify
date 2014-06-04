package io.lumify.tikaMimeType;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.assertEquals;

@RunWith(JUnit4.class)
public class TikaMimeTypeMapperTest {

    @Test
    public void testGuessMimeType() throws Exception {
        TikaMimeTypeMapper contentTypeExtractor = new TikaMimeTypeMapper();
        assertEquals("video/mp4", contentTypeExtractor.guessMimeType(TikaMimeTypeMapper.class.getResourceAsStream("/H_264.m4v"), "H_264.m4v"));
        assertEquals("video/mpeg", contentTypeExtractor.guessMimeType(TikaMimeTypeMapper.class.getResourceAsStream("/MPEG-2.m2v"), "2.m2v"));
        assertEquals("video/mp4", contentTypeExtractor.guessMimeType(TikaMimeTypeMapper.class.getResourceAsStream("/MPEG-4.mp4"), "MPEG-4.mp4"));
        assertEquals("text/plain", contentTypeExtractor.guessMimeType(TikaMimeTypeMapper.class.getResourceAsStream("/hello.txt"), "hello.txt"));
        assertEquals("application/pdf", contentTypeExtractor.guessMimeType(TikaMimeTypeMapper.class.getResourceAsStream("/hello.pdf"), "hello.pdf"));
        assertEquals("image/jpeg", contentTypeExtractor.guessMimeType(TikaMimeTypeMapper.class.getResourceAsStream("/cat.jpg"), "cat.jpg"));
        assertEquals("application/octet-stream", contentTypeExtractor.guessMimeType(TikaMimeTypeMapper.class.getResourceAsStream("/test.abc"), "test.abc"));
    }
}
