package io.lumify.core.cmdline;

import com.google.inject.Inject;
import io.lumify.core.model.ontology.OntologyRepository;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.semanticweb.owlapi.model.IRI;

import java.io.File;

public class OwlImport extends CommandLineBase {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(OwlImport.class);
    private String inFileName;
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
        IRI documentIRI;
        if (this.documentIRIString == null) {
            String guessedIri = getOntologyRepository().guessDocumentIRIFromPackage(inFile);
            documentIRI = IRI.create(guessedIri);
        } else {
            documentIRI = IRI.create(this.documentIRIString);
        }
        getOntologyRepository().importFile(inFile, documentIRI, getAuthorizations());
        getGraph().flush();
        getOntologyRepository().clearCache();
        LOGGER.info("owl import complete");
        return 0;
    }
}
