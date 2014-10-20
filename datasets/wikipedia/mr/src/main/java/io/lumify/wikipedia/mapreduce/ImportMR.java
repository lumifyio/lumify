package io.lumify.wikipedia.mapreduce;

import com.google.inject.Inject;
import io.lumify.core.mapreduce.LumifyMRBase;
import io.lumify.core.model.ontology.Concept;
import io.lumify.core.model.ontology.OntologyRepository;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import io.lumify.wikipedia.WikipediaConstants;
import org.apache.accumulo.core.data.Mutation;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.util.ToolRunner;
import org.securegraph.accumulo.mapreduce.AccumuloElementOutputFormat;

import java.io.File;

public class ImportMR extends LumifyMRBase {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(ImportMR.class);
    public static final String WIKIPEDIA_MIME_TYPE = "text/plain";
    public static final String MULTI_VALUE_KEY = ImportMR.class.getName();

    private OntologyRepository ontologyRepository;

    @Override
    protected void setupJob(Job job) throws Exception {
        verifyWikipediaPageConcept(ontologyRepository);
        verifyWikipediaPageInternalLinkWikipediaPageRelationship(ontologyRepository);

        job.setJarByClass(ImportMR.class);
        job.setMapperClass(ImportMRMapper.class);
        job.setNumReduceTasks(0);
        job.setMapOutputValueClass(Mutation.class);
        job.setInputFormatClass(TextInputFormat.class);
        job.setOutputFormatClass(AccumuloElementOutputFormat.class);
        FileInputFormat.addInputPath(job, new Path(getConf().get("in")));
    }

    @Override
    protected String getJobName() {
        return "wikipediaImport";
    }

    private void verifyWikipediaPageInternalLinkWikipediaPageRelationship(OntologyRepository ontologyRepository) {
        if (!ontologyRepository.hasRelationshipByIRI(WikipediaConstants.WIKIPEDIA_PAGE_INTERNAL_LINK_WIKIPEDIA_PAGE_CONCEPT_URI)) {
            throw new RuntimeException(WikipediaConstants.WIKIPEDIA_PAGE_INTERNAL_LINK_WIKIPEDIA_PAGE_CONCEPT_URI + " relationship not found");
        }
    }

    private void verifyWikipediaPageConcept(OntologyRepository ontologyRepository) {
        Concept wikipediaPageConcept = ontologyRepository.getConceptByIRI(WikipediaConstants.WIKIPEDIA_PAGE_CONCEPT_URI);
        if (wikipediaPageConcept == null) {
            throw new RuntimeException(WikipediaConstants.WIKIPEDIA_PAGE_CONCEPT_URI + " concept not found");
        }
    }

    @Override
    protected void parseArgs(JobConf conf, String[] args) {
        if (args.length != 1) {
            throw new RuntimeException("Required arguments <inputFileName>");
        }
        String inFileName = args[0];
        LOGGER.info("inFileName: %s", inFileName);
        conf.set("in", inFileName);
        conf.set(ImportMRMapper.CONFIG_SOURCE_FILE_NAME, new File(inFileName).getName());
    }

    public static void main(String[] args) throws Exception {
        int res = ToolRunner.run(new Configuration(), new ImportMR(), args);
        System.exit(res);
    }

    @Inject
    public void setOntologyRepository(OntologyRepository ontologyRepository) {
        this.ontologyRepository = ontologyRepository;
    }
}
