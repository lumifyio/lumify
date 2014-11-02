package io.lumify.palantir.dataImport.model.awstateProto;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

public class AwstateProtoInputStream {
    private final DataInputStream in;

    public AwstateProtoInputStream(byte[] awstateProto) {
        in = new DataInputStream(new ByteArrayInputStream(awstateProto));
    }

    public AwstateProtoWrapper readWrapper() throws IOException {
        int type = in.read();
        int len = readPackedInt();
        if (len == -1) {
            return null;
        }
        byte[] data = new byte[len];
        in.read(data);
        return new AwstateProtoWrapper(type, data);
    }

    public int readPackedInt() throws IOException {
        int size;
        int b = in.read();

        size = b & 0x7f;

        int shift = 7;
        while ((b & 0x80) == 0x80) {
            b = in.read();
            if (b == -1) {
                return -1;
            }
            size |= (b & 0x7f) << shift;
            shift += 7;
        }

        return size;
    }

    public int read() throws IOException {
        return this.in.read();
    }

    public long readUint64() throws IOException {
        byte[] readBuffer = new byte[8];
        in.readFully(readBuffer, 0, 8);
        return (((long) readBuffer[7] << 56) +
                ((long) (readBuffer[6] & 255) << 48) +
                ((long) (readBuffer[5] & 255) << 40) +
                ((long) (readBuffer[4] & 255) << 32) +
                ((long) (readBuffer[3] & 255) << 24) +
                ((readBuffer[2] & 255) << 16) +
                ((readBuffer[1] & 255) << 8) +
                ((readBuffer[0] & 255) << 0));
    }

    public int readUint16() throws IOException {
        byte[] readBuffer = new byte[2];
        in.readFully(readBuffer, 0, 2);
        return ((readBuffer[1] & 255) << 8) + ((readBuffer[0] & 255) << 0);
    }

    public void skip(int i) throws IOException {
        this.in.skip(i);
    }
}
