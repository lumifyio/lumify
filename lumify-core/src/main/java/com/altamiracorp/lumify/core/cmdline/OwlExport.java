package com.altamiracorp.lumify.core.cmdline;

import com.altamiracorp.lumify.core.exception.LumifyException;
import com.altamiracorp.lumify.core.model.ontology.OntologyRepository;
import com.google.inject.Inject;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.coode.owlapi.rdf.rdfxml.RDFXMLRenderer;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.List;

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
        OWLOntologyManager m = OWLManager.createOWLOntologyManager();
        OWLOntologyLoaderConfiguration config = new OWLOntologyLoaderConfiguration();
        config.setMissingImportHandlingStrategy(MissingImportHandlingStrategy.SILENT);

        List<OWLOntology> loadedOntologies = this.ontologyRepository.loadOntologyFiles(m, config, null);
        OWLOntology o = findOntology(loadedOntologies, documentIRI);
        if (o == null) {
            throw new LumifyException("Could not find ontology with iri " + documentIRI);
        }

        OutputStream out;
        if (outFileName != null) {
            out = new FileOutputStream(outFileName);
        } else {
            out = System.out;
        }
        Writer fileWriter = new OutputStreamWriter(out);

        try {
            new RDFXMLRenderer(o, fileWriter).render();

            return 0;
        } finally {
            fileWriter.close();
        }
    }

    private OWLOntology findOntology(List<OWLOntology> loadedOntologies, IRI documentIRI) {
        for (OWLOntology o : loadedOntologies) {
            if (documentIRI.equals(o.getOntologyID().getOntologyIRI())) {
                return o;
            }
        }
        return null;
    }

    @Inject
    public void setOntologyRepository(OntologyRepository ontologyRepository) {
        this.ontologyRepository = ontologyRepository;
    }
}
