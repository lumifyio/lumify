package io.lumify.wikipedia.mapreduce;

import com.google.inject.Inject;
import io.lumify.core.mapreduce.LumifyMRBase;
import io.lumify.core.model.ontology.Concept;
import io.lumify.core.model.ontology.OntologyRepository;
import io.lumify.core.model.ontology.Relationship;
import io.lumify.core.model.termMention.TermMentionModel;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import io.lumify.wikipedia.WikipediaConstants;
import org.apache.accumulo.core.client.AccumuloException;
import org.apache.accumulo.core.client.AccumuloSecurityException;
import org.apache.accumulo.core.client.TableNotFoundException;
import org.apache.accumulo.core.client.mapreduce.lib.partition.RangePartitioner;
import org.apache.accumulo.core.data.Mutation;
import org.apache.accumulo.core.util.TextUtil;
import org.apache.commons.codec.binary.Base64;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.util.ToolRunner;
import org.securegraph.Graph;
import org.securegraph.Vertex;
import org.securegraph.accumulo.AccumuloGraph;
import org.securegraph.accumulo.mapreduce.AccumuloElementOutputFormat;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ImportMR extends LumifyMRBase {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(ImportMR.class);
    public static final String WIKIPEDIA_MIME_TYPE = "text/plain";
    public static final String WIKIPEDIA_SOURCE = "Wikipedia";
    public static final String WIKIPEDIA_ID_PREFIX = "WIKIPEDIA_";
    public static final String WIKIPEDIA_LINK_ID_PREFIX = "WIKIPEDIA_LINK_";
    public static final String MULTI_VALUE_KEY = ImportMR.class.getName();

    private OntologyRepository ontologyRepository;

    static String getWikipediaPageVertexId(String pageTitle) {
        return WIKIPEDIA_ID_PREFIX + pageTitle.trim().toLowerCase();
    }

    static String getWikipediaPageToPageEdgeId(Vertex pageVertex, Vertex linkedPageVertex) {
        return WIKIPEDIA_LINK_ID_PREFIX + getWikipediaPageTitleFromId(pageVertex.getId()) + "_" + getWikipediaPageTitleFromId(linkedPageVertex.getId());
    }

    static String getWikipediaPageTitleFromId(Object id) {
        return id.toString().substring(WIKIPEDIA_ID_PREFIX.length());
    }

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
        Relationship wikipediaPageInternalLinkWikipediaPageRelationship = ontologyRepository.getRelationshipByIRI(WikipediaConstants.WIKIPEDIA_PAGE_INTERNAL_LINK_WIKIPEDIA_PAGE_CONCEPT_URI);
        if (wikipediaPageInternalLinkWikipediaPageRelationship == null) {
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
