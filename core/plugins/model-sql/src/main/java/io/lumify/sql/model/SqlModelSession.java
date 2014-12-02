package io.lumify.sql.model;

import com.altamiracorp.bigtable.model.FlushFlag;
import com.altamiracorp.bigtable.model.ModelSession;
import com.altamiracorp.bigtable.model.Row;
import com.altamiracorp.bigtable.model.RowKey;
import com.altamiracorp.bigtable.model.user.ModelUserContext;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class SqlModelSession extends ModelSession {
    @Override
    public void init(Map<String, Object> stringObjectMap) {
    }

    @Override
    public void save(Row row, FlushFlag flushFlag) {
        throw new RuntimeException("Not Implemented");
    }

    @Override
    public void saveMany(String s, Collection<Row> rows) {
        throw new RuntimeException("Not Implemented");
    }

    @Override
    public Iterable<Row> findByRowKeyRange(String s, String s2, String s3, ModelUserContext modelUserContext) {
        throw new RuntimeException("Not Implemented");
    }

    @Override
    public Iterable<Row> findByRowStartsWith(String s, String s2, ModelUserContext modelUserContext) {
        throw new RuntimeException("Not Implemented");
    }

    @Override
    public Iterable<Row> findByRowKeyRegex(String s, String s2, ModelUserContext modelUserContext) {
        throw new RuntimeException("Not Implemented");
    }

    @Override
    public Iterable<Row> findAll(String s, ModelUserContext modelUserContext) {
        throw new RuntimeException("Not Implemented");
    }

    @Override
    public long rowCount(String s, ModelUserContext modelUserContext) {
        throw new RuntimeException("Not Implemented");
    }

    @Override
    public Row findByRowKey(String s, String s2, ModelUserContext modelUserContext) {
        throw new RuntimeException("Not Implemented");
    }

    @Override
    public Row findByRowKey(String s, String s2, Map<String, String> stringStringMap, ModelUserContext modelUserContext) {
        throw new RuntimeException("Not Implemented");
    }

    @Override
    public void initializeTable(String s, ModelUserContext modelUserContext) {
    }

    @Override
    public void deleteTable(String s, ModelUserContext modelUserContext) {
    }

    @Override
    public void deleteRow(String s, RowKey rowKey) {
        throw new RuntimeException("Not Implemented");
    }

    @Override
    public void deleteColumn(Row row, String s, String s2, String s3, String s4) {
        throw new RuntimeException("Not Implemented");
    }

    @Override
    public List<String> getTableList(ModelUserContext modelUserContext) {
        throw new RuntimeException("Not Implemented");
    }

    @Override
    public void close() {
        // NOP was throw new RuntimeException("Not Implemented");
    }

    @Override
    public void flush() {
        throw new RuntimeException("Not Implemented");
    }

    @Override
    public ModelUserContext createModelUserContext(String... strings) {
        throw new RuntimeException("Not Implemented");
    }

    @Override
    public void alterColumnsVisibility(Row row, String s, String s2, FlushFlag flushFlag) {
        throw new RuntimeException("Not Implemented");
    }
}
