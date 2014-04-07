package com.altamiracorp.lumify.core.cmdline;

import com.altamiracorp.lumify.core.model.ontology.OntologyRepository;
import com.altamiracorp.securegraph.Graph;
import com.google.inject.Inject;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.semanticweb.owlapi.model.IRI;

import java.io.File;

public class OwlImport extends CommandLineBase {
    public static final String NS_XML_URI = "http://www.w3.org/XML/1998/namespace";

    private OntologyRepository ontologyRepository;
    private Graph graph;
    private String inFileName;
    private File inDir;
    private String documentIRIString;

    public static void main(String[] args) throws Exception {
        int res = new OwlImport().run(args);
        if (res != 0) {
            System.exit(res);
        }
    }

    @Override
    protected Options getOptions() {
        Options options = super.getOptions();

        options.addOption(
                OptionBuilder
                        .withLongOpt("in")
                        .withDescription("The input OWL file")
                        .isRequired()
                        .hasArg(true)
                        .withArgName("fileName")
                        .create("i")
        );

        options.addOption(
                OptionBuilder
                        .withLongOpt("iri")
                        .withDescription("The document IRI (URI used for prefixing concepts)")
                        .isRequired()
                        .hasArg(true)
                        .withArgName("uri")
                        .create()
        );

        return options;
    }

    @Override
    protected void processOptions(CommandLine cmd) throws Exception {
        super.processOptions(cmd);
        this.inFileName = cmd.getOptionValue("in");
        this.documentIRIString = cmd.getOptionValue("iri");
    }

    @Override
    protected int run(CommandLine cmd) throws Exception {
        File inFile = new File(this.inFileName);
        IRI documentIRI = IRI.create(this.documentIRIString);
        ontologyRepository.importFile(inFile, documentIRI);
        graph.flush();
        ontologyRepository.clearCache();
        return 0;
    }

    @Inject
    public void setOntologyRepository(OntologyRepository ontologyRepository) {
        this.ontologyRepository = ontologyRepository;
    }

    @Inject
    public void setGraph(Graph graph) {
        this.graph = graph;
    }
}
