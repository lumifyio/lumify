package com.altamiracorp.lumify.model;

import static com.google.common.base.Preconditions.checkNotNull;

import java.text.ParseException;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.altamiracorp.lumify.core.model.ontology.Property;
import com.altamiracorp.lumify.core.model.ontology.PropertyType;
import com.google.common.collect.Maps;
import com.thinkaurelius.titan.core.attribute.Geoshape;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.gremlin.Tokens;
import com.tinkerpop.gremlin.java.GremlinPipeline;
import com.tinkerpop.pipes.PipeFunction;

public class TitanQueryFormatter {
    private static final Map<String, Tokens.T> tokenMap = Maps.newHashMap();

    static {
        tokenMap.put("<", Tokens.T.lt);
        tokenMap.put("<=", Tokens.T.lte);
        tokenMap.put(">", Tokens.T.gt);
        tokenMap.put(">=", Tokens.T.gte);
        tokenMap.put("equal", Tokens.T.eq);
        tokenMap.put("=", Tokens.T.eq);
    }

    private static final String RANGE = "range";
    private static final String VALUES = "values";
    private static final String PREDICATE = "predicate";

    private static GremlinPipeline filterDate(GremlinPipeline pipeline, JSONObject filterJson, String propertyName) throws JSONException, ParseException {
        String predicate = filterJson.optString(PREDICATE, null);
        if (predicate == null) {
            throw new RuntimeException("'predicate' is required for data type 'date'");
        }

        JSONArray values = filterJson.optJSONArray(VALUES);
        if (values == null) {
            throw new RuntimeException("'values' is required for data type 'date'");
        }

        if (values.isNull(0)) {
            return pipeline;
        }

        long value = Property.DATE_FORMAT.parse(values.getString(0)).getTime();
        if (predicate.equals(RANGE)) {
            if (values.length() != 2) {
                throw new RuntimeException(String.format("'%s' requires 2 values, found %d", predicate, values.length()));
            }

            long otherValue = Property.DATE_FORMAT.parse(values.getString(1)).getTime();
            return pipeline.interval(propertyName, value, otherValue);
        } else {
            throwIfMissingValue(predicate, values);

            Tokens.T comparison = tokenMap.get(predicate);
            if (comparison != null) {
                return pipeline.has(propertyName, comparison, value);
            }
            throw new RuntimeException("Invalid predicate " + predicate);
        }
    }

    private static GremlinPipeline filterNumber(GremlinPipeline pipeline, JSONObject filterJson, String propertyName) throws JSONException {
        String predicate = filterJson.optString(PREDICATE, null);
        if (predicate == null) {
            throw new RuntimeException("'predicate' is required for data type 'number'");
        }

        JSONArray values = filterJson.optJSONArray(VALUES);
        if (values == null) {
            throw new RuntimeException("'values' is required for data type 'number'");
        }
        if (values.isNull(0)) {
            return pipeline;
        }

        double value = values.getDouble(0);

        if (predicate.equals(RANGE)) {
            if (values.length() != 2) {
                throw new RuntimeException(String.format("'%s' requires 2 values, found %d", predicate, values.length()));
            }

            double otherValue = values.getDouble(1);
            return pipeline.interval(propertyName, value, otherValue);
        } else {
            throwIfMissingValue(predicate, values);

            Tokens.T comparison = tokenMap.get(predicate);
            if (comparison != null) {
                return pipeline.has(propertyName, comparison, value);
            }
            throw new RuntimeException("Invalid predicate " + predicate);
        }
    }

    private static void throwIfMissingValue(String predicate, JSONArray values) {
        if (values.length() != 1) {
            throw new RuntimeException(String.format("'%s' requires 1 value, found %d", predicate, values.length()));
        }
    }

    private static GremlinPipeline filterGeoLocation(GremlinPipeline<Vertex, Vertex> pipeline, JSONObject filterJson, final String propertyName) throws JSONException {
        JSONArray values = filterJson.optJSONArray(VALUES);
        if (values == null) {
            throw new RuntimeException("'values' is required for data type 'string'");
        }

        if (values.length() != 3) {
            throw new RuntimeException("'geo location' requires 3 value, found " + values.length());
        }

        if (values.isNull(0)) {
            return pipeline;
        }

        double latitude = values.getDouble(0);
        double longitude = values.getDouble(1);
        double radius = values.getDouble(2);
        final Geoshape bounds = Geoshape.circle(latitude, longitude, radius);

        return pipeline.filter(new PipeFunction<Vertex, Boolean>() {
            @Override
            public Boolean compute(Vertex argument) {
                Geoshape property = argument.getProperty(propertyName);
                if (property == null) {
                    return false;
                }
                return property.within(bounds);
            }
        });
    }

    private static GremlinPipeline filterString(GremlinPipeline<Vertex, Vertex> pipeline, JSONObject filterJson, final String propertyName) throws JSONException {
        JSONArray values = filterJson.optJSONArray(VALUES);
        if (values == null) {
            throw new RuntimeException("'values' is required for data type 'string'");
        }

        if (values.length() != 1) {
            throw new RuntimeException("'contains' requires 1 value, found " + values.length());
        }

        if (values.isNull(0)) {
            return pipeline;
        }

        final String value = values.getString(0).toLowerCase();

        return pipeline.filter(new PipeFunction<Vertex, Boolean>() {
            @Override
            public Boolean compute(Vertex argument) {
                String property = argument.getProperty(propertyName);
                return StringUtils.containsIgnoreCase(property, value);
            }
        });
    }

    private GremlinPipeline<Vertex, Vertex> addFilter(JSONObject filterJson, GremlinPipeline<Vertex, Vertex> pipeline) {
        String propertyDataType = filterJson.optString("propertyDataType", null);
        if (propertyDataType == null) {
            throw new RuntimeException("Could not find 'propertyDataType' on filter JSON.");
        }

        PropertyType propertyDateType = PropertyType.convert(propertyDataType);
        String propertyName = filterJson.optString("propertyName", null);
        if (propertyName == null) {
            throw new RuntimeException("Could not find 'propertyName' to filter on.");
        }

        try {
            switch (propertyDateType) {
                case DATE:
                    return filterDate(pipeline, filterJson, propertyName);
                case CURRENCY:
                    return filterNumber(pipeline, filterJson, propertyName);
                case GEO_LOCATION:
                    return filterGeoLocation(pipeline, filterJson, propertyName);
                default:
                    return filterString(pipeline, filterJson, propertyName);
            }
        } catch (JSONException e) {
            throw new RuntimeException(e);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Create a Gremlin pipeline from a JSON array of JSON objects.
     *
     * @param vertices    Vertices to start the pipeline on
     * @param filtersJson JSON array of objects to filter
     * @return Gremlin pipeline for filtering
     */
    public GremlinPipeline<Vertex, Vertex> createQueryPipeline(Iterable<Vertex> vertices, JSONArray filtersJson) {
        checkNotNull(vertices, "Vertices cannot be null");
        checkNotNull(filtersJson, "JSON filter cannot be null");
        GremlinPipeline<Vertex, Vertex> pipeline = new GremlinPipeline<Vertex, Vertex>(vertices);
        for (int i = 0; i < filtersJson.length(); i++) {
            JSONObject filterJson;
            try {
                filterJson = filtersJson.getJSONObject(i);
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }

            pipeline = addFilter(filterJson, pipeline);
        }
        return pipeline;
    }
}
