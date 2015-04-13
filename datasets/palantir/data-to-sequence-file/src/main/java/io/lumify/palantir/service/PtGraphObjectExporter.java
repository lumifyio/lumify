package io.lumify.palantir.service;

import io.lumify.palantir.model.LongLongWritable;
import io.lumify.palantir.model.PtGraphObject;

public class PtGraphObjectExporter extends ExporterBase<PtGraphObject> {
    public PtGraphObjectExporter() {
        super(PtGraphObject.class);
    }

    @Override
    protected String getSql() {
        return "SELECT * FROM {namespace}.PT_GRAPH_OBJECT ORDER BY GRAPH_ID, OBJECT_ID";
    }

    @Override
    protected Class<?> getKeyClass() {
        return LongLongWritable.class;
    }
}
