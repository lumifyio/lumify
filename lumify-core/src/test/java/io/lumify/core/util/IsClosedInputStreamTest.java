package io.lumify.core.util;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertEquals;

public class IsClosedInputStreamTest {
    @Test
    public void testIsClosed() throws IOException {
        byte[] data = new byte[10];
        InputStream origIn = new ByteArrayInputStream(data);
        IsClosedInputStream in = new IsClosedInputStream(origIn);
        assertEquals(false, in.isClosed());
        in.close();
        assertEquals(true, in.isClosed());
    }
}
