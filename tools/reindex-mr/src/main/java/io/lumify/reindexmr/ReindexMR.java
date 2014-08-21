package io.lumify.reindexmr;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.output.NullOutputFormat;
import org.apache.hadoop.util.ToolRunner;

public class ReindexMR extends ReindexMRBase {
    public static void main(String[] args) throws Exception {
        int res = ToolRunner.run(new Configuration(), new ReindexMR(), args);
        System.exit(res);
    }

    @Override
    protected void setupJobMapper(Job job) {
        job.setMapperClass(ReindexMRMapper.class);
        job.setOutputFormatClass(NullOutputFormat.class);
    }
}
