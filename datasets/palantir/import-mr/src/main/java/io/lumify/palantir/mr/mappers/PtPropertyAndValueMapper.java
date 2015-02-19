package io.lumify.palantir.mr.mappers;

import io.lumify.core.security.LumifyVisibility;
import io.lumify.palantir.model.PtPropertyAndValue;
import org.apache.hadoop.io.LongWritable;
import org.securegraph.Visibility;

import java.io.IOException;

public class PtPropertyAndValueMapper extends PalantirMapperBase<LongWritable, PtPropertyAndValue> {
    private Visibility visibility;

    @Override
    protected void setup(Context context) throws IOException, InterruptedException {
        super.setup(context);
        visibility = new LumifyVisibility("").getVisibility();
    }

    @Override
    protected void safeMap(LongWritable key, PtPropertyAndValue ptPropertyAndValue, Context context) throws Exception {
        context.setStatus(key.toString());


    }

}
