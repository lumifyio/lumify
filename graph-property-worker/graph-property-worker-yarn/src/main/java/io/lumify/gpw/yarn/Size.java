package io.lumify.gpw.yarn;

import org.apache.twill.api.ResourceSpecification;

class Size {
    private final int size;
    private final ResourceSpecification.SizeUnit units;

    Size(int size, ResourceSpecification.SizeUnit units) {
        this.size = size;
        this.units = units;
    }

    public int getSize() {
        return size;
    }

    public ResourceSpecification.SizeUnit getUnits() {
        return units;
    }
}
