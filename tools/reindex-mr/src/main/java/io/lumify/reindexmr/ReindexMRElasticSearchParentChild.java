package io.lumify.reindexmr;

import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.util.ToolRunner;
import org.elasticsearch.hadoop.mr.EsOutputFormat;

public class ReindexMRElasticSearchParentChild extends ReindexMRBase {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(ReindexMRElasticSearchParentChild.class);
    public static final String ES_ELEMENT_TYPE_PROPERTY = "_estype";
    public static final String ES_ID_PROPERTY = "_id";
    public static final String ES_PARENT_PROPERTY = "_parent";

    public static void main(String[] args) throws Exception {
        int res = ToolRunner.run(new Configuration(), new ReindexMRElasticSearchParentChild(), args);
        System.exit(res);
    }

    @Override
    protected void setupJobMapper(Job job) {
        job.setMapperClass(ReindexMRMapperElasticSearchParentChild.class);
        job.setOutputFormatClass(EsOutputFormat.class);
    }

    @Override
    protected JobConf getConfiguration(String[] args, io.lumify.core.config.Configuration lumifyConfig) {
        JobConf conf = super.getConfiguration(args, lumifyConfig);

        conf.set("es.input.json", "yes");
        conf.setBoolean("mapred.map.tasks.speculative.execution", false);
        conf.setBoolean("mapred.reduce.tasks.speculative.execution", false);
        conf.set("es.nodes", conf.get("graph.search.locations"));
        conf.set("es.mapping.id", ES_ID_PROPERTY);
        conf.set("es.resource.write", conf.get("graph.search.indexName") + "/{" + ES_ELEMENT_TYPE_PROPERTY + "}");
        conf.set("es.mapping.parent", ES_PARENT_PROPERTY);

        return conf;
    }
}
