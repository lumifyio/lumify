package com.altamiracorp.lumify.core.util;

import com.altamiracorp.securegraph.Edge;
import com.altamiracorp.securegraph.Element;
import com.altamiracorp.securegraph.Vertex;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Collection;

public class GraphUtil {
    public static JSONArray toJson(Collection<Element> elements) {
        JSONArray result = new JSONArray();
        for (Element element : elements) {
            result.put(toJson(element));
        }
        return result;
    }

    public static JSONObject toJson(Element element) {
        if (element instanceof Vertex) {
            return toJson((Vertex) element);
        }
        if (element instanceof Edge) {
            return toJson((Edge) element);
        }
        throw new RuntimeException("Unexpected element type: " + element.getClass().getName());
    }

    public static JSONObject toJson(Vertex vertex) {

    }

    public static JSONObject toJson(Edge edge) {

    }
}
