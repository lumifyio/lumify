package io.lumify.themoviedb;

import io.lumify.core.mapreduce.LumifyMRBase;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import org.apache.accumulo.core.data.Mutation;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat;
import org.apache.hadoop.util.ToolRunner;
import org.securegraph.accumulo.mapreduce.AccumuloElementOutputFormat;

import java.io.File;
import java.io.IOException;

public class ImportJsonMR extends LumifyMRBase {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(ImportJsonMR.class);
    private static final String JOB_NAME = "theMovieDbJsonImport";

    @Override
    protected String getJobName() {
        return JOB_NAME;
    }

    @Override
    protected void setupJob(Job job) throws InterruptedException, IOException, ClassNotFoundException {
        job.setMapperClass(ImportJsonMRMapper.class);
        job.setMapOutputValueClass(Mutation.class);
        job.setNumReduceTasks(0);
        job.setInputFormatClass(SequenceFileInputFormat.class);
        job.setOutputFormatClass(AccumuloElementOutputFormat.class);

        SequenceFileInputFormat.addInputPath(job, new Path(getConf().get("in")));
    }

    @Override
    protected void parseArgs(JobConf conf, String[] args) {
        if (args.length != 1) {
            throw new RuntimeException("Required arguments <inputFileName>");
        }
        String inFileName = args[0];
        conf.set("in", inFileName);
        conf.set(CONFIG_SOURCE_FILE_NAME, new File(inFileName).getName());
        LOGGER.info("inFileName: %s", inFileName);
    }

    public static void main(String[] args) throws Exception {
        int res = ToolRunner.run(new ImportJsonMR(), args);
        System.exit(res);
    }
}
