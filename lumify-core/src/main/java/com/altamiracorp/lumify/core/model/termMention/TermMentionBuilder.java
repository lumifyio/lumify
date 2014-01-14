package com.altamiracorp.lumify.core.model.termMention;

import com.altamiracorp.bigtable.model.BaseBuilder;
import com.altamiracorp.bigtable.model.Column;
import com.altamiracorp.bigtable.model.ColumnFamily;
import com.altamiracorp.bigtable.model.Row;

import java.util.Collection;

public class TermMentionBuilder extends BaseBuilder<TermMentionModel> {
    @Override
    public TermMentionModel fromRow(Row row) {
        TermMentionModel termMention = new TermMentionModel(row.getRowKey());
        Collection<ColumnFamily> families = row.getColumnFamilies();
        for (ColumnFamily columnFamily : families) {
            if (columnFamily.getColumnFamilyName().equals(TermMentionMetadata.NAME)) {
                Collection<Column> columns = columnFamily.getColumns();
                termMention.addColumnFamily(new TermMentionMetadata().addColumns(columns));
            } else {
                termMention.addColumnFamily(columnFamily);
            }
        }
        return termMention;
    }

    @Override
    public String getTableName() {
        return TermMentionModel.TABLE_NAME;
    }
}
