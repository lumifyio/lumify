package io.lumify.migrations;

import io.lumify.core.mapreduce.LumifyElementMapperBase;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Counter;
import org.securegraph.Authorizations;
import org.securegraph.Element;
import org.securegraph.accumulo.AccumuloAuthorizations;

import java.io.IOException;

public abstract class ElementMigrationMapperBase extends LumifyElementMapperBase<Text, Element> {
    private Counter elementsMigratedCounter;
    private Counter elementsViewedCounter;
    private boolean dryRun;
    private Authorizations authorizations;

    @Override
    protected void setup(Context context) throws IOException, InterruptedException {
        super.setup(context);
        this.elementsMigratedCounter = context.getCounter(MigrationCounters.ELEMENTS_MIGRATED);
        this.elementsViewedCounter = context.getCounter(MigrationCounters.ELEMENTS_VIEWED);
        this.authorizations = new AccumuloAuthorizations();
        this.dryRun = MigrationBase.isDryRun(context);
    }

    @Override
    protected final void safeMap(Text key, Element element, Context context) throws Exception {
        context.setStatus("migrating " + key.toString());
        if (migrate(element, context)) {
            elementsMigratedCounter.increment(1);
        }
        elementsViewedCounter.increment(1);
    }

    protected abstract boolean migrate(Element element, Context context);

    public boolean isDryRun() {
        return dryRun;
    }

    public Authorizations getAuthorizations() {
        return authorizations;
    }
}
