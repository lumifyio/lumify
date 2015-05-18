package io.lumify.mapping.column;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.lumify.mapping.LumifyDataMappingException;
import io.lumify.mapping.column.AbstractColumnDocumentMapping.Row;
import io.lumify.web.clientapi.model.util.ObjectMapperFactory;

/**
 * Exception generated when mapping a columnar data source.
 */
public class LumifyColumnMappingException extends LumifyDataMappingException {
    private static final ObjectMapper MAPPER = ObjectMapperFactory.getInstance();

    public LumifyColumnMappingException(final Row row, final ColumnEntityMapping vertex, Throwable cause) {
        super(createMessage(row, vertex, null, null), cause);
    }

    public LumifyColumnMappingException(final Row row, final ColumnEntityMapping vertex, final ColumnValue property, Throwable cause) {
        super(createMessage(row, vertex, null, property), cause);
    }

    public LumifyColumnMappingException(final Row row, final ColumnRelationshipMapping edge, Throwable cause) {
        super(createMessage(row, null, edge, null), cause);
    }

    private static String createMessage(final Row row, final ColumnEntityMapping vertex, final ColumnRelationshipMapping edge, final ColumnValue property) {
        StringBuilder msg = new StringBuilder("Error processing row ").append(row.getRowNumber()).append(".");
        if (vertex != null) {
            msg.append(" Vertex: ").append(safeJson(vertex));
        }
        if (edge != null) {
            msg.append(" Edge: ").append(safeJson(edge));
        }
        if (property != null) {
            msg.append(" Property: ").append(safeJson(property));
        }
        return msg.toString();
    }

    private static String safeJson(final Object obj) {
        String json;
        try {
            json = MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException jpe) {
            json = String.format("{ Error: %s }", jpe.getMessage());
        }
        return json;
    }
}
