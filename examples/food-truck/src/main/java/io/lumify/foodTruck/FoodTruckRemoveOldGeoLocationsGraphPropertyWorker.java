package io.lumify.foodTruck;

import io.lumify.core.ingest.graphProperty.GraphPropertyWorkData;
import io.lumify.core.ingest.graphProperty.GraphPropertyWorker;
import io.lumify.core.model.properties.LumifyProperties;
import org.securegraph.Element;
import org.securegraph.Property;
import org.securegraph.Vertex;

import java.io.InputStream;
import java.util.Calendar;
import java.util.Date;

import static org.securegraph.util.IterableUtils.toList;

public class FoodTruckRemoveOldGeoLocationsGraphPropertyWorker extends GraphPropertyWorker {
    @Override
    public void execute(InputStream in, GraphPropertyWorkData data) throws Exception {
        Date geoLocationDate = FoodTruckOntology.GEO_LOCATION_DATE.getPropertyValue(data.getElement());
        if (geoLocationDate == null) {
            return;
        }
        Calendar geoLocationCalendar = Calendar.getInstance();
        geoLocationCalendar.setTime(geoLocationDate);

        Calendar nowCalendar = Calendar.getInstance();
        nowCalendar.setTime(new Date());

        if (geoLocationCalendar.get(Calendar.DAY_OF_YEAR) == nowCalendar.get(Calendar.DAY_OF_YEAR)) {
            return;
        }

        for (Property property : toList(FoodTruckOntology.GEO_LOCATION.getProperties(data.getElement()))) {
            data.getElement().removeProperty(property.getKey(), property.getName(), getAuthorizations());
        }
        for (Property property : toList(FoodTruckOntology.GEO_LOCATION_DATE.getProperties(data.getElement()))) {
            data.getElement().removeProperty(property.getKey(), property.getName(), getAuthorizations());
        }
        getGraph().flush();
    }

    @Override
    public boolean isHandled(Element element, Property property) {
        if (!(element instanceof Vertex)) {
            return false;
        }

        String conceptType = LumifyProperties.CONCEPT_TYPE.getPropertyValue(element);
        if (conceptType == null || !conceptType.equals(FoodTruckOntology.CONCEPT_TYPE_FOOD_TRUCK)) {
            return false;
        }

        return true;
    }
}
