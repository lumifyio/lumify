package io.lumify.web.clientapi.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class DetectedObjects {
    private List<Property> detectedObjects = new ArrayList<Property>();

    public List<Property> getDetectedObjects() {
        return detectedObjects;
    }

    public void addDetectedObject(Property detectedObject) {
        this.detectedObjects.add(detectedObject);
    }

    public void addDetectedObjects(Collection<Property> properties) {
        this.detectedObjects.addAll(properties);
    }
}
