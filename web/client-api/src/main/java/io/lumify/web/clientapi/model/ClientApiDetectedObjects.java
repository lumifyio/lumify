package io.lumify.web.clientapi.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ClientApiDetectedObjects implements ClientApiObject {
    private List<ClientApiProperty> detectedObjects = new ArrayList<ClientApiProperty>();

    public List<ClientApiProperty> getDetectedObjects() {
        return detectedObjects;
    }

    public void addDetectedObject(ClientApiProperty detectedObject) {
        this.detectedObjects.add(detectedObject);
    }

    public void addDetectedObjects(Collection<ClientApiProperty> properties) {
        this.detectedObjects.addAll(properties);
    }
}
