package io.lumify.themoviedb;

import io.lumify.core.mapreduce.LumifyMRBase;
import org.apache.accumulo.core.data.Mutation;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapreduce.Counter;
import org.apache.hadoop.mapreduce.CounterGroup;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat;
import org.apache.hadoop.util.ToolRunner;
import org.securegraph.accumulo.mapreduce.AccumuloElementOutputFormat;

import java.io.IOException;

public class ImportJsonMR extends LumifyMRBase {
    private static final String JOB_NAME = "theMovieDbJsonImport";

    @Override
    protected String getJobName() {
        return JOB_NAME;
    }

    @Override
    protected int run(Job job) throws InterruptedException, IOException, ClassNotFoundException {
        job.setMapperClass(ImportJsonMRMapper.class);
        job.setMapOutputValueClass(Mutation.class);
        job.setNumReduceTasks(0);
        job.setInputFormatClass(SequenceFileInputFormat.class);
        job.setOutputFormatClass(AccumuloElementOutputFormat.class);

        SequenceFileInputFormat.addInputPath(job, new Path(getConf().get("in")));

        int returnCode = job.waitForCompletion(true) ? 0 : 1;

        CounterGroup groupCounters = job.getCounters().getGroup(TheMovieDbImportCounters.class.getName());
        for (Counter counter : groupCounters) {
            System.out.println(counter.getDisplayName() + ": " + counter.getValue());
        }

        return returnCode;
    }

    public static void main(String[] args) throws Exception {
        int res = ToolRunner.run(new ImportJsonMR(), args);
        System.exit(res);
    }
}
