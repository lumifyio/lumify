package io.lumify.palantir.mr;

import io.lumify.core.mapreduce.LumifyMRBase;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import io.lumify.palantir.mr.mappers.PtGraphMapper;
import io.lumify.palantir.mr.mappers.PtGraphObjectMapper;
import io.lumify.palantir.mr.mappers.PtObjectMapper;
import io.lumify.palantir.mr.mappers.PtUserMapper;
import org.apache.accumulo.core.data.Mutation;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat;
import org.apache.hadoop.util.ToolRunner;
import org.securegraph.accumulo.mapreduce.AccumuloElementOutputFormat;

public class ImportMR extends LumifyMRBase {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(ImportMR.class);
    public static final String CONF_BASE_IRI = "baseIri";
    public static final String CONF_TYPE = "type";
    public static final String CONF_IN_DIR = "inDir";

    @Override
    protected void setupJob(Job job) throws Exception {
        String type = job.getConfiguration().get(CONF_TYPE);
        Path inPath = new Path(job.getConfiguration().get(CONF_IN_DIR));
        Path inFilePath;

        job.setJarByClass(ImportMR.class);
        if (type.equalsIgnoreCase("ptuser")) {
            inFilePath = new Path(inPath, "PtUser.seq");
            job.setMapperClass(PtUserMapper.class);
        } else if (type.equalsIgnoreCase("ptgraph")) {
            inFilePath = new Path(inPath, "PtGraph.seq");
            job.setMapperClass(PtGraphMapper.class);
        } else if (type.equalsIgnoreCase("ptobject")) {
            inFilePath = new Path(inPath, "PtObject.seq");
            job.setMapperClass(PtObjectMapper.class);
        } else if (type.equalsIgnoreCase("ptgraphobject")) {
            inFilePath = new Path(inPath, "PtGraphObject.seq");
            job.setMapperClass(PtGraphObjectMapper.class);
        } else {
            throw new RuntimeException("Invalid import type");
        }
        job.setNumReduceTasks(0);
        job.setMapOutputValueClass(Mutation.class);
        job.setInputFormatClass(SequenceFileInputFormat.class);
        job.setOutputFormatClass(AccumuloElementOutputFormat.class);
        SequenceFileInputFormat.setInputPaths(job, inFilePath);
    }

    @Override
    protected String getJobName() {
        return "palantirImport";
    }

    @Override
    protected void parseArgs(JobConf conf, String[] args) {
        if (args.length != 3) {
            throw new RuntimeException("Required arguments <inputDir> <type> <baseIri>");
        }
        String inDir = args[0];
        LOGGER.info("inDir: %s", inDir);
        conf.set(CONF_IN_DIR, inDir);

        String type = args[1];
        LOGGER.info("type: %s", type);
        conf.set(CONF_TYPE, type);

        String baseIri = args[2];
        LOGGER.info("baseIri: %s", baseIri);
        conf.set(CONF_BASE_IRI, baseIri);
    }

    public static void main(String[] args) throws Exception {
        int res = ToolRunner.run(new Configuration(), new ImportMR(), args);
        System.exit(res);
    }
}
