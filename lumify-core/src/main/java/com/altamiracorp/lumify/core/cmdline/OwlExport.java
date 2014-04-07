package com.altamiracorp.lumify.core.cmdline;

import com.altamiracorp.lumify.core.model.ontology.OntologyRepository;
import com.google.inject.Inject;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.semanticweb.owlapi.model.IRI;

import java.io.FileOutputStream;
import java.io.OutputStream;

public class OwlExport extends CommandLineBase {
    private OntologyRepository ontologyRepository;
    private String outFileName;
    private IRI documentIRI;

    public static void main(String[] args) throws Exception {
        int res = new OwlExport().run(args);
        if (res != 0) {
            System.exit(res);
        }
    }

    @Override
    protected Options getOptions() {
        Options options = super.getOptions();

        options.addOption(
                OptionBuilder
                        .withLongOpt("out")
                        .withDescription("The output OWL file")
                        .hasArg(true)
                        .withArgName("fileName")
                        .create("o")
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
        this.outFileName = cmd.getOptionValue("out");
        this.documentIRI = IRI.create(cmd.getOptionValue("iri"));
    }

    @Override
    protected int run(CommandLine cmd) throws Exception {
        OutputStream out;
        if (outFileName != null) {
            out = new FileOutputStream(outFileName);
        } else {
            out = System.out;
        }
        try {
            ontologyRepository.exportOntology(out, this.documentIRI);
            return 0;
        } finally {
            out.close();
        }
    }

    @Inject
    public void setOntologyRepository(OntologyRepository ontologyRepository) {
        this.ontologyRepository = ontologyRepository;
    }
}
