package io.lumify.palantir.util;

import org.apache.commons.io.IOUtils;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.InflaterInputStream;

public class TryInflaterInputStream extends InputStream {
    public static final int TEST_COMPRESSED_SIZE = 10 * 1024;
    private InputStream in;

    public TryInflaterInputStream(InputStream in) throws IOException {
        BufferedInputStream bufferedIn = new BufferedInputStream(in, TEST_COMPRESSED_SIZE);
        bufferedIn.mark(TEST_COMPRESSED_SIZE);
        try {
            InflaterInputStream testCompressedIn = new InflaterInputStream(bufferedIn);
            byte[] temp = new byte[TEST_COMPRESSED_SIZE / 10];
            testCompressedIn.read(temp);

            this.in = new InflaterInputStream(bufferedIn);
        } catch (Exception ex) {
            this.in = bufferedIn;
        } finally {
            bufferedIn.reset();
        }
    }

    @Override
    public int read() throws IOException {
        return in.read();
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return this.in.read(b, off, len);
    }

    public static byte[] inflate(byte[] data) throws IOException {
        return IOUtils.toByteArray(new TryInflaterInputStream(new ByteArrayInputStream(data)));
    }
}
