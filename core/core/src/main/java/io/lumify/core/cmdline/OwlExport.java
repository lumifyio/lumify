package io.lumify.core.cmdline;

import com.google.inject.Inject;
import io.lumify.core.model.ontology.OntologyRepository;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.semanticweb.owlapi.model.IRI;

import java.io.FileOutputStream;
import java.io.OutputStream;

public class OwlExport extends CommandLineBase {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(OwlExport.class);
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
            getOntologyRepository().exportOntology(out, this.documentIRI);
            LOGGER.info("owl export complete");
            return 0;
        } finally {
            out.close();
        }
    }
}
