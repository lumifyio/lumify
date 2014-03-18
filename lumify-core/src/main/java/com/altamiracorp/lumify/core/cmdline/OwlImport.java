package com.altamiracorp.lumify.core.cmdline;

import com.altamiracorp.lumify.core.exception.LumifyException;
import com.altamiracorp.lumify.core.model.ontology.*;
import com.altamiracorp.lumify.core.model.properties.LumifyProperties;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.securegraph.Graph;
import com.altamiracorp.securegraph.property.StreamingPropertyValue;
import com.google.inject.Inject;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.OWLOntologyDocumentSource;
import org.semanticweb.owlapi.io.ReaderDocumentSource;
import org.semanticweb.owlapi.model.*;

import java.io.*;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

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
        if (cmd.hasOption("iri")) {
            this.documentIRIString = cmd.getOptionValue("iri");
        } else {
            this.documentIRIString = new File(this.inFileName).toURI().toString();
        }
    }

    @Override
    protected int run(CommandLine cmd) throws Exception {
        File inFile = new File(this.inFileName);
        IRI documentIRI = IRI.create(this.documentIRIString);
        importFile(inFile, documentIRI, getUser());
        return 0;
    }

    public void importFile(File inFile, IRI documentIRI, User user) throws OWLOntologyCreationException, IOException {
        inDir = inFile.getParentFile();

        OWLOntologyManager m = OWLManager.createOWLOntologyManager();
        Reader inFileReader = new FileReader(inFile);
        OWLOntologyLoaderConfiguration config = new OWLOntologyLoaderConfiguration();
        config.setMissingImportHandlingStrategy(MissingImportHandlingStrategy.SILENT);

        loadBaseOntology(m, config);

        OWLOntologyDocumentSource documentSource = new ReaderDocumentSource(inFileReader, documentIRI);
        OWLOntology o = m.loadOntologyFromOntologyDocument(documentSource, config);

        for (OWLClass ontologyClass : o.getClassesInSignature()) {
            importOntologyClass(o, ontologyClass);
        }

        for (OWLDataProperty dataTypeProperty : o.getDataPropertiesInSignature()) {
            importDataProperty(o, dataTypeProperty);
        }

        for (OWLObjectProperty objectProperty : o.getObjectPropertiesInSignature()) {
            importObjectProperty(o, objectProperty);
        }

        graph.flush();
        ontologyRepository.clearCache();
    }

    private void loadBaseOntology(OWLOntologyManager m, OWLOntologyLoaderConfiguration config) throws OWLOntologyCreationException, IOException {
        InputStream lumifyBaseOntologyIn = getClass().getResourceAsStream("/com/altamiracorp/lumify/core/ontology/base.owl");
        checkNotNull(lumifyBaseOntologyIn, "Could not load base ontology file");
        try {
            Reader lumifyBaseOntologyReader = new InputStreamReader(lumifyBaseOntologyIn);
            IRI lumifyBaseOntologyIRI = IRI.create("http://lumify.io");
            OWLOntologyDocumentSource lumifyBaseOntologySource = new ReaderDocumentSource(lumifyBaseOntologyReader, lumifyBaseOntologyIRI);
            m.loadOntologyFromOntologyDocument(lumifyBaseOntologySource, config);
        } finally {
            lumifyBaseOntologyIn.close();
        }
    }

    private Concept importOntologyClass(OWLOntology o, OWLClass ontologyClass) throws IOException {
        String uri = ontologyClass.getIRI().toString();

        String label = getLabel(o, ontologyClass);
        LOGGER.info("Importing ontology class " + uri + " (label: " + label + ")");

        Concept parent = getParentConcept(o, ontologyClass);
        Concept result = ontologyRepository.getOrCreateConcept(parent, uri, label);

        String color = getColor(o, ontologyClass);
        if (color != null) {
            OntologyLumifyProperties.COLOR.setProperty(result.getVertex(), color, OntologyRepository.VISIBILITY.getVisibility());
        }

        String glyphIconFileName = getGlyphIconFileName(o, ontologyClass);
        if (glyphIconFileName != null) {
            File iconFile = new File(inDir, glyphIconFileName);
            if (!iconFile.exists()) {
                throw new RuntimeException("Could not find icon file: " + iconFile.toString());
            }
            InputStream iconFileIn = new FileInputStream(iconFile);
            try {
                StreamingPropertyValue value = new StreamingPropertyValue(iconFileIn, byte[].class);
                value.searchIndex(false);
                value.store(true);
                LumifyProperties.GLYPH_ICON.setProperty(result.getVertex(), value, OntologyRepository.VISIBILITY.getVisibility());
            } finally {
                iconFileIn.close();
            }
        }

        return result;
    }

    private Concept getParentConcept(OWLOntology o, OWLClass ontologyClass) throws IOException {
        Set<OWLClassExpression> superClasses = ontologyClass.getSuperClasses(o);
        if (superClasses.size() == 0) {
            return ontologyRepository.getEntityConcept();
        } else if (superClasses.size() == 1) {
            OWLClassExpression superClassExpr = superClasses.iterator().next();
            OWLClass superClass = superClassExpr.asOWLClass();
            String superClassUri = superClass.getIRI().toString();
            Concept parent = ontologyRepository.getConceptById(superClassUri);
            if (parent != null) {
                return parent;
            }

            parent = importOntologyClass(o, superClass);
            if (parent == null) {
                throw new LumifyException("Could not find or create parent: " + superClass);
            }
            return parent;
        } else {
            throw new LumifyException("Unhandled multiple super classes. Found " + superClasses.size() + ", expected 0 or 1.");
        }
    }

    private void importDataProperty(OWLOntology o, OWLDataProperty dataTypeProperty) {
        String propertyId = dataTypeProperty.getIRI().toString();
        String propertyDisplayName = getLabel(o, dataTypeProperty);
        PropertyType propertyType = getPropertyType(o, dataTypeProperty);
        if (propertyType == null) {
            throw new LumifyException("Could not get property type on data property " + propertyId);
        }

        for (OWLClassExpression domainClassExpr : dataTypeProperty.getDomains(o)) {
            OWLClass domainClass = domainClassExpr.asOWLClass();
            String domainClassUri = domainClass.getIRI().toString();
            Concept domainConcept = ontologyRepository.getConceptById(domainClassUri);
            checkNotNull(domainConcept, "Could not find class with uri: " + domainClassUri);

            LOGGER.info("Adding data property " + propertyId + " to class " + domainConcept.getId());
            ontologyRepository.addPropertyTo(domainConcept.getVertex(), propertyId, propertyDisplayName, propertyType);
        }
    }

    private Relationship importObjectProperty(OWLOntology o, OWLObjectProperty objectProperty) {
        String uri = objectProperty.getIRI().toString();
        String label = getLabel(o, objectProperty);
        LOGGER.info("Importing ontology object property " + uri + " (label: " + label + ")");

        Concept domain = getDomainConcept(o, objectProperty);
        Concept range = getRangeConcept(o, objectProperty);

        return ontologyRepository.getOrCreateRelationshipType(domain, range, uri, label);
    }

    private Concept getRangeConcept(OWLOntology o, OWLObjectProperty objectProperty) {
        String uri = objectProperty.getIRI().toString();
        if (objectProperty.getRanges(o).size() != 1) {
            throw new LumifyException("Invalid number of range properties on " + uri);
        }

        for (OWLClassExpression rangeClassExpr : objectProperty.getRanges(o)) {
            OWLClass rangeClass = rangeClassExpr.asOWLClass();
            String rangeClassUri = rangeClass.getIRI().toString();
            Concept ontologyClass = ontologyRepository.getConceptById(rangeClassUri);
            checkNotNull(ontologyClass, "Could not find class with uri: " + rangeClassUri);
            return ontologyClass;
        }
        throw new LumifyException("Invalid number of range properties on " + uri);
    }

    private Concept getDomainConcept(OWLOntology o, OWLObjectProperty objectProperty) {
        String uri = objectProperty.getIRI().toString();
        if (objectProperty.getRanges(o).size() != 1) {
            throw new LumifyException("Invalid number of domain properties on " + uri);
        }

        for (OWLClassExpression rangeClassExpr : objectProperty.getDomains(o)) {
            OWLClass rangeClass = rangeClassExpr.asOWLClass();
            String rangeClassUri = rangeClass.getIRI().toString();
            Concept ontologyClass = ontologyRepository.getConceptById(rangeClassUri);
            checkNotNull(ontologyClass, "Could not find class with uri: " + rangeClassUri);
            return ontologyClass;
        }
        throw new LumifyException("Invalid number of domain properties on " + uri);
    }

    private PropertyType getPropertyType(OWLOntology o, OWLDataProperty dataTypeProperty) {
        Set<OWLDataRange> ranges = dataTypeProperty.getRanges(o);
        if (ranges.size() == 0) {
            return null;
        }
        if (ranges.size() > 1) {
            throw new LumifyException("Unexpected number of ranges on data property " + dataTypeProperty.getIRI().toString());
        }
        for (OWLDataRange range : ranges) {
            if (range instanceof OWLDatatype) {
                OWLDatatype datatype = (OWLDatatype) range;
                return getPropertyType(datatype.getIRI().toString());
            }
        }
        throw new LumifyException("Could not find range on data property " + dataTypeProperty.getIRI().toString());
    }

    private PropertyType getPropertyType(String iri) {
        if ("http://www.w3.org/2001/XMLSchema#string".equals(iri)) {
            return PropertyType.STRING;
        }
        if ("http://www.w3.org/2001/XMLSchema#dateTime".equals(iri)) {
            return PropertyType.DATE;
        }
        if ("http://lumify.io#geolocation".equals(iri)) {
            return PropertyType.GEO_LOCATION;
        }
        if ("http://lumify.io#currency".equals(iri)) {
            return PropertyType.CURRENCY;
        }
        throw new LumifyException("Unhandled property type " + iri);
    }

    private String getLabel(OWLOntology o, OWLEntity owlEntity) {
        for (OWLAnnotation annotation : owlEntity.getAnnotations(o)) {
            if (annotation.getProperty().isLabel()) {
                OWLLiteral value = (OWLLiteral) annotation.getValue();
                return value.getLiteral();
            }
        }
        return null;
    }

    private String getColor(OWLOntology o, OWLEntity owlEntity) {
        return getAnnotationValueByUri(o, owlEntity, "http://lumify.io#color");
    }

    private String getGlyphIconFileName(OWLOntology o, OWLEntity owlEntity) {
        return getAnnotationValueByUri(o, owlEntity, "http://lumify.io#glyphIconFileName");
    }

    private String getAnnotationValueByUri(OWLOntology o, OWLEntity owlEntity, String uri) {
        for (OWLAnnotation annotation : owlEntity.getAnnotations(o)) {
            if (annotation.getProperty().getIRI().toString().equals(uri)) {
                OWLLiteral value = (OWLLiteral) annotation.getValue();
                return value.getLiteral();
            }
        }
        return null;
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
