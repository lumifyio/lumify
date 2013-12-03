package com.altamiracorp.lumify.core.model.geoNames;

import java.util.Comparator;

public class GeoNamePopulationComparator implements Comparator<GeoName> {
    @Override
    public int compare(GeoName geoName1, GeoName geoName2) {
        Long population1 = geoName1.getMetadata().getPopulation();
        Long population2 = geoName2.getMetadata().getPopulation();

        // nulls get put at the top
        if (population1 == null && population2 == null) {
            return 0;
        }
        if (population1 == null) {
            return -1;
        }
        if (population2 == null) {
            return 1;
        }
        return population1.compareTo(population2);
    }
}
