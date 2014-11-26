package io.lumify.csv;

import io.lumify.core.exception.LumifyException;
import io.lumify.csv.model.Mapping;

public class LumifyCsvException extends LumifyException {
    public LumifyCsvException(State state, Mapping.Vertex vertex, Throwable cause) {
        super(createMessage(state, vertex, null, null), cause);
    }

    public LumifyCsvException(State state, Mapping.Vertex vertex, Mapping.Property property, Throwable cause) {
        super(createMessage(state, vertex, null, property), cause);
    }

    public LumifyCsvException(State state, Mapping.Edge edge, Throwable cause) {
        super(createMessage(state, null, edge, null), cause);
    }

    private static String createMessage(State state, Mapping.Vertex vertex, Mapping.Edge edge, Mapping.Property property) {
        StringBuilder sb = new StringBuilder();
        sb.append("Error processing record: ");
        sb.append(state.getRecord());
        sb.append(".");
        if (vertex != null) {
            sb.append(" Vertex: ");
            sb.append(vertex);
            sb.append(".");
        }
        if (edge != null) {
            sb.append(" Edge: ");
            sb.append(edge);
            sb.append(".");
        }
        if (property != null) {
            sb.append(" Property: ");
            sb.append(property);
            sb.append(".");
        }
        return sb.toString();
    }
}
