package io.lumify.palantir.dataImport.model.awstateProto;

import java.io.IOException;

public class AwstateProtoObject {
    private final int index;
    private final long objectId;
    private final int x;
    private final int y;

    public AwstateProtoObject(byte[] data) throws IOException {
        AwstateProtoInputStream in = new AwstateProtoInputStream(data);

        in.skip(1); // unknown1;
        index = in.readPackedInt();
        in.skip(1); // unknown2;
        objectId = in.readUint64();
        in.skip(1); // unknown3;
        x = in.readUint16();
        in.skip(9);
        y = in.readUint16();
    }

    public int getIndex() {
        return index;
    }

    public long getObjectId() {
        return objectId;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }
}
