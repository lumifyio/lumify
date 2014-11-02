package io.lumify.palantir.dataImport.model.awstateProto;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AwstateProto {
    private List<AwstateProtoObject> objects = new ArrayList<AwstateProtoObject>();

    public AwstateProto(byte[] awstateProto) throws IOException {
        AwstateProtoInputStream in = new AwstateProtoInputStream(awstateProto);

        AwstateProtoWrapper wrapper1 = in.readWrapper();
        AwstateProtoWrapper wrapper2 = wrapper1.getAwstateProtoInputStream().readWrapper();

        AwstateProtoInputStream vertices = wrapper2.getAwstateProtoInputStream();
        while (true) {
            AwstateProtoWrapper vertex = vertices.readWrapper();
            if (vertex == null) {
                break;
            }
            if (vertex.getData().length == 37) {
                AwstateProtoWrapper vertexData = vertex.getAwstateProtoInputStream().readWrapper();
                AwstateProtoObject v = new AwstateProtoObject(vertexData.getData());
                this.objects.add(v);
            }
        }
    }

    public List<AwstateProtoObject> getObjects() {
        return objects;
    }

    public AwstateProtoObject findObject(long objectId) {
        AwstateProtoObject o = null;
        long minDistance = Long.MAX_VALUE;

        for (AwstateProtoObject z : getObjects()) {
            long distance = Math.abs(z.getObjectId() - objectId);
            if (distance < minDistance) {
                o = z;
                minDistance = distance;
            }
        }

        if (minDistance > 0xffff) {
            return null;
        }

        return o;
    }
}
