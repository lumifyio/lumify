package io.lumify.core.util;

import org.apache.commons.codec.binary.Hex;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

public class HdfsLimitOutputStream extends OutputStream {
    private static Random random = new Random();
    private final FileSystem fs;
    private final int maxSizeToStore;
    private final ByteArrayOutputStream smallOutputStream;
    private final MessageDigest digest;
    private OutputStream largeOutputStream;
    private Path hdfsPath;
    private long length;

    public HdfsLimitOutputStream(FileSystem fs, long maxSizeToStore) throws NoSuchAlgorithmException {
        this.digest = MessageDigest.getInstance("SHA-256");
        this.fs = fs;
        this.maxSizeToStore = (int) maxSizeToStore;
        this.smallOutputStream = new ByteArrayOutputStream((int) maxSizeToStore);
        this.length = 0;
    }

    private synchronized OutputStream getLargeOutputStream() throws IOException {
        if (largeOutputStream == null) {
            hdfsPath = createTempPath();
            largeOutputStream = fs.create(hdfsPath);
            largeOutputStream.write(smallOutputStream.toByteArray());
        }
        return largeOutputStream;
    }

    private Path createTempPath() {
        return new Path("/tmp/hdfsLimitOutputStream-" + random.nextLong());
    }

    @Override
    public synchronized void write(int b) throws IOException {
        this.digest.update((byte) b);
        if (this.smallOutputStream.size() <= maxSizeToStore - 1) {
            this.smallOutputStream.write(b);
        } else {
            getLargeOutputStream().write(b);
        }
        length++;
    }

    @Override
    public synchronized void write(byte[] b) throws IOException {
        this.digest.update(b);
        if (this.smallOutputStream.size() <= maxSizeToStore - b.length) {
            this.smallOutputStream.write(b);
        } else {
            getLargeOutputStream().write(b);
        }
        length += b.length;
    }

    @Override
    public synchronized void write(byte[] b, int off, int len) throws IOException {
        this.digest.update(b, off, len);
        if (this.smallOutputStream.size() <= maxSizeToStore - len) {
            this.smallOutputStream.write(b, off, len);
        } else {
            getLargeOutputStream().write(b, off, len);
        }
        length += len;
    }

    public synchronized boolean hasExceededSizeLimit() {
        return this.largeOutputStream != null;
    }

    public Path getHdfsPath() {
        return hdfsPath;
    }

    public byte[] getSmall() {
        if (hasExceededSizeLimit()) {
            return null;
        }
        return this.smallOutputStream.toByteArray();
    }

    public String getRowKey() {
        byte[] sha = this.digest.digest();
        return "urn" + RowKeyHelper.MINOR_FIELD_SEPARATOR + "sha256" + RowKeyHelper.MINOR_FIELD_SEPARATOR + Hex.encodeHexString(sha);
    }

    @Override
    public synchronized void flush() throws IOException {
        if (this.largeOutputStream != null) {
            this.largeOutputStream.flush();
        }
        super.close();
    }

    @Override
    public synchronized void close() throws IOException {
        if (this.largeOutputStream != null) {
            this.largeOutputStream.close();
        }
        this.smallOutputStream.close();
        super.close();
    }

    public long getLength() {
        return this.length;
    }
}
