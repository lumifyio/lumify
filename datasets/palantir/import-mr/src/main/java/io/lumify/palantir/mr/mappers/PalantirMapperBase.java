package io.lumify.palantir.mr.mappers;

import io.lumify.core.mapreduce.LumifyElementMapperBase;
import io.lumify.palantir.mr.ImportMR;
import org.apache.hadoop.io.LongWritable;
import org.securegraph.accumulo.AccumuloAuthorizations;

import java.io.IOException;

public abstract class PalantirMapperBase<VALUEIN> extends LumifyElementMapperBase<LongWritable, VALUEIN> {
    private String baseIri;
    private AccumuloAuthorizations authorizations;

    @Override
    protected void setup(Context context) throws IOException, InterruptedException {
        super.setup(context);
        baseIri = context.getConfiguration().get(ImportMR.CONF_BASE_IRI);
        authorizations = new AccumuloAuthorizations();
    }

    public String getBaseIri() {
        return baseIri;
    }

    public AccumuloAuthorizations getAuthorizations() {
        return authorizations;
    }
}
