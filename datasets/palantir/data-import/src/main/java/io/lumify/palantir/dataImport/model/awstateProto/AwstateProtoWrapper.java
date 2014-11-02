package io.lumify.palantir.dataImport.model.awstateProto;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;

public class AwstateProtoWrapper {
    private final int type;
    private final byte[] data;

    public AwstateProtoWrapper(int type, byte[] data) {
        this.type = type;
        this.data = data;
    }

    public AwstateProtoInputStream getAwstateProtoInputStream() {
        return new AwstateProtoInputStream(this.data);
    }

    public int getType() {
        return type;
    }

    public byte[] getData() {
        return data;
    }

    public DataInputStream getDataInputStream() {
        return new DataInputStream(new ByteArrayInputStream(getData()));
    }
}
