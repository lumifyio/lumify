package io.lumify.mapping.csv;

import static com.google.common.base.Preconditions.checkArgument;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import io.lumify.mapping.column.AbstractColumnDocumentMapping;
import io.lumify.mapping.column.ColumnEntityMapping;
import io.lumify.mapping.column.ColumnRelationshipMapping;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

/**
 * DocumentMapping for CSV files.
 */
@JsonTypeName("csv")
public class CsvDocumentMapping extends AbstractColumnDocumentMapping {
    /**
     * The class logger.
     */
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(CsvDocumentMapping.class);

    /**
     * The CSV document mapping type.
     */
    public static final String CSV_DOCUMENT_TYPE = "csv";

    /**
     * The default number of rows to skip.
     */
    public static final int DEFAULT_SKIP_ROWS = 0;

    /**
     * The number of rows to skip.
     */
    private final int skipRows;

    /**
     * Create a new CsvDocumentMapping.
     * @param skipRows the number of rows to skip
     * @param entities the entity mappings
     * @param relationships the relationship mappings
     */
    @JsonCreator
    public CsvDocumentMapping(@JsonProperty("entities") final Map<String, ColumnEntityMapping> entities,
                              @JsonProperty(value="relationships", required=false) final List<ColumnRelationshipMapping> relationships,
                              @JsonProperty(value="skipRows",required=false) final Integer skipRows) {
        super(entities, relationships);
        checkArgument(skipRows == null || skipRows >= 0, "skipRows must be >= 0 if provided.");
        this.skipRows = skipRows != null && skipRows >= 0 ? skipRows : DEFAULT_SKIP_ROWS;
    }

    /**
     * Get the number of rows to skip.
     * @return the number of rows to skip
     */
    @JsonProperty("skipRows")
    public int getSkipRows() {
        return skipRows;
    }

    @Override
    protected Iterable<Row> getRows(final InputStream input) throws IOException {
        return new RowIterable(new CsvRowIterator(new InputStreamReader(input)));
    }

    private class CsvRowIterator implements Iterator<Row> {
        /**
         * The wrapped CSVRecord iterator.
         */
        private final Iterator<CSVRecord> records;

        /**
         * Create a new CsvRowIterator.
         * @param reader the input document
         */
        public CsvRowIterator(final Reader reader) throws IOException {
            records = CSVFormat.EXCEL.parse(reader).iterator();
            // skip configured lines
            for (int ln=0; ln < skipRows && records.hasNext(); records.next());
        }

        @Override
        public boolean hasNext() {
            return records.hasNext();
        }

        @Override
        public Row next() {
            return new CSVRecordRow(records.next());
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("Rows cannot be removed.");
        }
    }

    private class CSVRecordRow implements Row {
        /**
         * The wrapped CSV record.
         */
        private final CSVRecord record;

        /**
         * The cached record size.
         */
        private final int size;

        public CSVRecordRow(final CSVRecord rec) {
            record = rec;
            size = record.size();
        }

        @Override
        public String get(int col) {
            return record.get(col);
        }

        @Override
        public int getColumnCount() {
            return size;
        }

        @Override
        public boolean isEmpty() {
            return size > 0;
        }

        @Override
        public long getRowNumber() {
            return record.getRecordNumber();
        }
    }
}
