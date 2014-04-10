package com.altamiracorp.lumify.core.model.ontology;

import com.altamiracorp.lumify.core.exception.LumifyException;
import com.altamiracorp.lumify.core.model.properties.LumifyProperties;
import com.altamiracorp.lumify.core.util.LumifyLogger;
import com.altamiracorp.lumify.core.util.LumifyLoggerFactory;
import com.altamiracorp.securegraph.property.StreamingPropertyValue;
import org.apache.commons.io.IOUtils;
import org.coode.owlapi.rdf.rdfxml.RDFXMLRenderer;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.OWLOntologyDocumentSource;
import org.semanticweb.owlapi.io.ReaderDocumentSource;
import org.semanticweb.owlapi.model.*;

import java.io.*;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class OntologyRepositoryBase implements OntologyRepository {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(OntologyRepositoryBase.class);

    @Override
    public void importFile(File inFile, IRI documentIRI) throws Exception {
        if (!inFile.exists()) {
            throw new LumifyException("File " + inFile + " does not exist");
        }
        File inDir = inFile.getParentFile();

        FileInputStream inFileIn = new FileInputStream(inFile);
        try {
            importFile(inFileIn, documentIRI, inDir);
        } finally {
            inFileIn.close();
        }
    }

    @Override
    public void importFile(InputStream in, IRI documentIRI, File inDir) throws Exception {
        byte[] inFileData = IOUtils.toByteArray(in);

        Reader inFileReader = new InputStreamReader(new ByteArrayInputStream(inFileData));

        OWLOntologyManager m = OWLManager.createOWLOntologyManager();
        OWLOntologyLoaderConfiguration config = new OWLOntologyLoaderConfiguration();
        config.setMissingImportHandlingStrategy(MissingImportHandlingStrategy.SILENT);

        loadOntologyFiles(m, config, documentIRI);

        OWLOntologyDocumentSource documentSource = new ReaderDocumentSource(inFileReader, documentIRI);
        OWLOntology o = m.loadOntologyFromOntologyDocument(documentSource, config);

        for (OWLClass ontologyClass : o.getClassesInSignature()) {
            if (!o.isDeclared(ontologyClass, false)) {
                continue;
            }
            importOntologyClass(o, ontologyClass, inDir);
        }

        for (OWLDataProperty dataTypeProperty : o.getDataPropertiesInSignature()) {
            if (!o.isDeclared(dataTypeProperty, false)) {
                continue;
            }
            importDataProperty(o, dataTypeProperty);
        }

        for (OWLObjectProperty objectProperty : o.getObjectPropertiesInSignature()) {
            if (!o.isDeclared(objectProperty, false)) {
                continue;
            }
            importObjectProperty(o, objectProperty);
        }

        storeOntologyFile(new ByteArrayInputStream(inFileData), documentIRI);
    }

    protected abstract void storeOntologyFile(InputStream inputStream, IRI documentIRI);

    public void exportOntology(OutputStream out, IRI documentIRI) throws Exception {
        OWLOntologyManager m = OWLManager.createOWLOntologyManager();
        OWLOntologyLoaderConfiguration config = new OWLOntologyLoaderConfiguration();
        config.setMissingImportHandlingStrategy(MissingImportHandlingStrategy.SILENT);

        List<OWLOntology> loadedOntologies = loadOntologyFiles(m, config, null);
        OWLOntology o = findOntology(loadedOntologies, documentIRI);
        if (o == null) {
            throw new LumifyException("Could not find ontology with iri " + documentIRI);
        }

        Writer fileWriter = new OutputStreamWriter(out);

        new RDFXMLRenderer(o, fileWriter).render();
    }

    protected abstract List<OWLOntology> loadOntologyFiles(OWLOntologyManager m, OWLOntologyLoaderConfiguration config, IRI excludedIRI) throws Exception;

    private OWLOntology findOntology(List<OWLOntology> loadedOntologies, IRI documentIRI) {
        for (OWLOntology o : loadedOntologies) {
            if (documentIRI.equals(o.getOntologyID().getOntologyIRI())) {
                return o;
            }
        }
        return null;
    }

    private Concept importOntologyClass(OWLOntology o, OWLClass ontologyClass, File inDir) throws IOException {
        String uri = ontologyClass.getIRI().toString();
        if ("http://www.w3.org/2002/07/owl#Thing".equals(uri)) {
            return getEntityConcept();
        }

        String label = getLabel(o, ontologyClass);
        checkNotNull(label, "label cannot be null or empty: " + uri);
        LOGGER.info("Importing ontology class " + uri + " (label: " + label + ")");

        Concept parent = getParentConcept(o, ontologyClass, inDir);
        Concept result = getOrCreateConcept(parent, uri, label);

        String color = getColor(o, ontologyClass);
        if (color != null) {
            result.setProperty(OntologyLumifyProperties.COLOR.getKey(), color, OntologyRepository.VISIBILITY.getVisibility());
        }

        String displayType = getDisplayType(o, ontologyClass);
        if (displayType != null) {
            result.setProperty(OntologyLumifyProperties.DISPLAY_TYPE.getKey(), displayType, OntologyRepository.VISIBILITY.getVisibility());
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
                result.setProperty(LumifyProperties.GLYPH_ICON.getKey(), value, OntologyRepository.VISIBILITY.getVisibility());
            } finally {
                iconFileIn.close();
            }
        }

        return result;
    }

    private Concept getParentConcept(OWLOntology o, OWLClass ontologyClass, File inDir) throws IOException {
        Set<OWLClassExpression> superClasses = ontologyClass.getSuperClasses(o);
        if (superClasses.size() == 0) {
            return getEntityConcept();
        } else if (superClasses.size() == 1) {
            OWLClassExpression superClassExpr = superClasses.iterator().next();
            OWLClass superClass = superClassExpr.asOWLClass();
            String superClassUri = superClass.getIRI().toString();
            Concept parent = getConceptByIRI(superClassUri);
            if (parent != null) {
                return parent;
            }

            parent = importOntologyClass(o, superClass, inDir);
            if (parent == null) {
                throw new LumifyException("Could not find or create parent: " + superClass);
            }
            return parent;
        } else {
            throw new LumifyException("Unhandled multiple super classes. Found " + superClasses.size() + ", expected 0 or 1.");
        }
    }

    private void importDataProperty(OWLOntology o, OWLDataProperty dataTypeProperty) {
        String propertyIRI = dataTypeProperty.getIRI().toString();
        String propertyDisplayName = getLabel(o, dataTypeProperty);
        PropertyType propertyType = getPropertyType(o, dataTypeProperty);
        boolean userVisible = getUserVisible(o, dataTypeProperty);
        if (propertyType == null) {
            throw new LumifyException("Could not get property type on data property " + propertyIRI);
        }

        for (OWLClassExpression domainClassExpr : dataTypeProperty.getDomains(o)) {
            OWLClass domainClass = domainClassExpr.asOWLClass();
            String domainClassUri = domainClass.getIRI().toString();
            Concept domainConcept = getConceptByIRI(domainClassUri);
            checkNotNull(domainConcept, "Could not find class with uri: " + domainClassUri);

            LOGGER.info("Adding data property " + propertyIRI + " to class " + domainConcept.getTitle());
            addPropertyTo(domainConcept, propertyIRI, propertyDisplayName, propertyType, userVisible);
        }
    }

    protected abstract OntologyProperty addPropertyTo(Concept concept, String propertyIRI, String displayName, PropertyType dataType, boolean userVisible);

    private Relationship importObjectProperty(OWLOntology o, OWLObjectProperty objectProperty) {
        String uri = objectProperty.getIRI().toString();
        String label = getLabel(o, objectProperty);
        checkNotNull(label, "label cannot be null or empty for " + uri);
        LOGGER.info("Importing ontology object property " + uri + " (label: " + label + ")");

        Concept domain = getDomainConcept(o, objectProperty);
        Concept range = getRangeConcept(o, objectProperty);

        return getOrCreateRelationshipType(domain, range, uri, label);
    }

    private Concept getRangeConcept(OWLOntology o, OWLObjectProperty objectProperty) {
        String uri = objectProperty.getIRI().toString();
        if (objectProperty.getRanges(o).size() != 1) {
            throw new LumifyException("Invalid number of range properties on " + uri);
        }

        for (OWLClassExpression rangeClassExpr : objectProperty.getRanges(o)) {
            OWLClass rangeClass = rangeClassExpr.asOWLClass();
            String rangeClassUri = rangeClass.getIRI().toString();
            Concept ontologyClass = getConceptByIRI(rangeClassUri);
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
            Concept ontologyClass = getConceptByIRI(rangeClassUri);
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
        if ("http://www.w3.org/2001/XMLSchema#int".equals(iri)) {
            return PropertyType.DOUBLE;
        }
        if ("http://www.w3.org/2001/XMLSchema#double".equals(iri)) {
            return PropertyType.DOUBLE;
        }
        if ("http://lumify.io#geolocation".equals(iri)) {
            return PropertyType.GEO_LOCATION;
        }
        if ("http://lumify.io#currency".equals(iri)) {
            return PropertyType.CURRENCY;
        }
        if ("http://lumify.io#image".equals(iri)) {
            return PropertyType.IMAGE;
        }
        if ("http://www.w3.org/2001/XMLSchema#hexBinary".equals(iri)) {
            return PropertyType.BINARY;
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

    private String getDisplayType(OWLOntology o, OWLEntity owlEntity) {
        return getAnnotationValueByUri(o, owlEntity, "http://lumify.io#displayType");
    }

    private boolean getUserVisible(OWLOntology o, OWLEntity owlEntity) {
        String val = getAnnotationValueByUri(o, owlEntity, "http://lumify.io#userVisible");
        return val == null || Boolean.parseBoolean(val);
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
}
