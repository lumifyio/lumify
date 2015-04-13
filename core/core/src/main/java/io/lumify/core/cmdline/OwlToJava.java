package io.lumify.core.cmdline;

import com.google.inject.Inject;
import io.lumify.core.exception.LumifyException;
import io.lumify.core.model.ontology.OntologyRepository;
import io.lumify.core.model.ontology.OntologyRepositoryBase;
import io.lumify.core.model.properties.types.*;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.semanticweb.owlapi.model.*;

import static org.securegraph.util.IterableUtils.toList;

public class OwlToJava extends CommandLineBase {
    public static void main(String[] args) throws Exception {
        int res = new OwlToJava().run(args);
        if (res != 0) {
            System.exit(res);
        }
    }

    @Override
    protected Options getOptions() {
        Options options = super.getOptions();

        options.addOption(
                OptionBuilder
                        .withLongOpt("iri")
                        .withDescription("The IRI of the ontology you would like exported")
                        .isRequired()
                        .hasArg(true)
                        .withArgName("iri")
                        .create("i")
        );

        return options;
    }

    @Override
    protected int run(CommandLine cmd) throws Exception {
        IRI iri = IRI.create(cmd.getOptionValue("iri"));

        OWLOntologyLoaderConfiguration config = new OWLOntologyLoaderConfiguration();
        OWLOntologyManager m = getOntologyRepository().createOwlOntologyManager(config, null);

        OWLOntology o = m.getOntology(iri);
        if (o == null) {
            System.err.println("Could not find ontology " + iri);
            return 1;
        }

        System.out.println("public class Ontology {");

        for (OWLObjectProperty objectProperty : o.getObjectPropertiesInSignature()) {
            if (!o.isDeclared(objectProperty, false)) {
                continue;
            }
            exportObjectProperty(o, objectProperty);
        }
        System.out.println();

        for (OWLClass owlClass : o.getClassesInSignature()) {
            if (!o.isDeclared(owlClass, false)) {
                continue;
            }
            exportClass(o, owlClass);
        }
        System.out.println();

        for (OWLDataProperty dataProperty : o.getDataPropertiesInSignature()) {
            if (!o.isDeclared(dataProperty, false)) {
                continue;
            }
            exportDataProperty(o, dataProperty);
        }
        System.out.println();

        System.out.println("}");

        return 0;
    }

    private void exportObjectProperty(OWLOntology o, OWLObjectProperty objectProperty) {
        String iri = objectProperty.getIRI().toString();
        String label = OntologyRepositoryBase.getLabel(o, objectProperty);

        System.out.println(String.format("    public static final String EDGE_LABEL_%s = \"%s\";", toJavaConst(label), iri));
    }

    private void exportClass(OWLOntology o, OWLClass owlClass) {
        String iri = owlClass.getIRI().toString();
        String label = OntologyRepositoryBase.getLabel(o, owlClass);

        System.out.println(String.format("    public static final String CONCEPT_TYPE_%s = \"%s\";", toJavaConst(label), iri));
    }

    private void exportDataProperty(OWLOntology o, OWLDataProperty dataProperty) {
        String iri = dataProperty.getIRI().toString();
        String label = OntologyRepositoryBase.getLabel(o, dataProperty);
        OWLDatatype range = (OWLDatatype) toList(dataProperty.getRanges(o)).get(0);
        String rangeIri = range.getIRI().toString();

        String type;
        if ("http://www.w3.org/2001/XMLSchema#double".equals(rangeIri)) {
            type = DoubleLumifyProperty.class.getSimpleName();
        } else if ("http://www.w3.org/2001/XMLSchema#int".equals(rangeIri)) {
            type = IntegerLumifyProperty.class.getSimpleName();
        } else if ("http://lumify.io#geolocation".equals(rangeIri)) {
            type = GeoPointLumifyProperty.class.getSimpleName();
        } else if ("http://www.w3.org/2001/XMLSchema#string".equals(rangeIri)) {
            type = StringLumifyProperty.class.getSimpleName();
        } else if ("http://www.w3.org/2001/XMLSchema#dateTime".equals(rangeIri)) {
            type = DateLumifyProperty.class.getSimpleName();
        } else {
            throw new LumifyException("Could not map range type " + rangeIri);
        }

        System.out.println(String.format("    public static final %s %s = new %s(\"%s\");", type, toJavaConst(label), type, iri));
    }

    private String toJavaConst(String label) {
        return label.toUpperCase().replaceAll(" ", "_");
    }
}
