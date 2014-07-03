package io.lumify.wikipedia;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

public class RandomAccessFileInputStream extends InputStream {
    private final RandomAccessFile randomAccessFile;

    public RandomAccessFileInputStream(RandomAccessFile randomAccessFile) {
        this.randomAccessFile = randomAccessFile;
    }

    @Override
    public int read() throws IOException {
        return this.randomAccessFile.read();
    }

    @Override
    public int read(byte[] b) throws IOException {
        return this.randomAccessFile.read(b);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return this.randomAccessFile.read(b, off, len);
    }
}
