package io.lumify.core.util;

import io.lumify.core.model.properties.LumifyProperties;
import org.securegraph.Property;

import java.util.Comparator;
import java.util.Map;

public class ConfidencePropertyComparator implements Comparator<Property> {
    @Override
    public int compare(Property p1, Property p2) {
        Map<String, Object> p1meta = p1.getMetadata();
        Map<String, Object> p2meta = p2.getMetadata();

        if (p1meta == null && p2meta == null) {
            return 0;
        }
        if (p1meta == null) {
            return 1;
        }
        if (p2meta == null) {
            return -1;
        }

        double p1confidence = LumifyProperties.CONFIDENCE.getMetadataValue(p1meta, 0.5);
        double p2confidence = LumifyProperties.CONFIDENCE.getMetadataValue(p2meta, 0.5);

        return -Double.compare(p1confidence, p2confidence);
    }
}
