package io.lumify.core.model.ontology;

import com.google.common.io.Files;
import io.lumify.core.config.Configuration;
import io.lumify.core.exception.LumifyException;
import io.lumify.core.exception.LumifyResourceNotFoundException;
import io.lumify.core.model.properties.LumifyProperties;
import io.lumify.core.util.ExecutorServiceUtil;
import io.lumify.core.util.JSONUtil;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import io.lumify.web.clientapi.model.ClientApiOntology;
import io.lumify.web.clientapi.model.PropertyType;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.coode.owlapi.rdf.rdfxml.RDFXMLRenderer;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.securegraph.Authorizations;
import org.securegraph.TextIndexHint;
import org.securegraph.property.StreamingPropertyValue;
import org.securegraph.util.CloseableUtils;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.OWLOntologyDocumentSource;
import org.semanticweb.owlapi.io.ReaderDocumentSource;
import org.semanticweb.owlapi.model.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class OntologyRepositoryBase implements OntologyRepository {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(OntologyRepositoryBase.class);

    public void defineOntology(Configuration config, Authorizations authorizations) throws Exception {
        Concept rootConcept = getOrCreateConcept(null, OntologyRepository.ROOT_CONCEPT_IRI, "root", null);
        Concept entityConcept = getOrCreateConcept(rootConcept, OntologyRepository.ENTITY_CONCEPT_IRI, "thing", null);
        clearCache();
        addEntityGlyphIcon(entityConcept);
        importBaseOwlFile(authorizations);

        for (String key : config.getKeys(Configuration.ONTOLOGY_REPOSITORY_OWL)) {
            if (key.endsWith(".iri")) {
                String iri = config.get(key, null);
                String dir = config.get(key.replace(".iri", ".dir"), null);
                String file = config.get(key.replace(".iri", ".file"), null);

                if (iri != null) {
                    if (dir != null) {
                        File owlFile = findOwlFile(new File(dir));
                        if (owlFile == null) {
                            throw new LumifyResourceNotFoundException("could not find owl file in directory " + new File(dir).getAbsolutePath());
                        }
                        importFile(owlFile, IRI.create(iri), authorizations);
                    } else if (file != null) {
                        writePackage(new File(file), IRI.create(iri), authorizations);
                    } else {
                        throw new LumifyResourceNotFoundException("iri " + iri + " without matching dir or file");
                    }
                }
            }
        }
    }

    private void importBaseOwlFile(Authorizations authorizations) {
        importResourceOwl("base.owl", "http://lumify.io", authorizations);
        importResourceOwl("user.owl", "http://lumify.io/user", authorizations);
        importResourceOwl("workspace.owl", "http://lumify.io/workspace", authorizations);
        importResourceOwl("comment.owl", "http://lumify.io/comment", authorizations);
    }

    private void importResourceOwl(String fileName, String iri, Authorizations authorizations) {
        LOGGER.debug("importResourceOwl %s (iri: %s)", fileName, iri);
        InputStream baseOwlFile = OntologyRepositoryBase.class.getResourceAsStream(fileName);
        checkNotNull(baseOwlFile, "Could not load resource " + OntologyRepositoryBase.class.getResource(fileName));

        try {
            importFile(baseOwlFile, IRI.create(iri), null, authorizations);
        } catch (Exception e) {
            throw new LumifyException("Could not import ontology file: " + fileName + " (iri: " + iri + ")", e);
        } finally {
            CloseableUtils.closeQuietly(baseOwlFile);
        }
    }

    private void addEntityGlyphIcon(Concept entityConcept) {
        InputStream entityGlyphIconInputStream = OntologyRepositoryBase.class.getResourceAsStream("entity.png");
        checkNotNull(entityGlyphIconInputStream, "Could not load resource " + OntologyRepositoryBase.class.getResource("entity.png"));

        try {
            ByteArrayOutputStream imgOut = new ByteArrayOutputStream();
            IOUtils.copy(entityGlyphIconInputStream, imgOut);

            byte[] rawImg = imgOut.toByteArray();

            addEntityGlyphIconToEntityConcept(entityConcept, rawImg);
        } catch (IOException e) {
            throw new LumifyException("invalid stream for glyph icon");
        }
    }

    protected abstract void addEntityGlyphIconToEntityConcept(Concept entityConcept, byte[] rawImg);

    public boolean isOntologyDefined() {
        try {
            Concept concept = getConceptByIRI(OntologyRepository.ROOT_CONCEPT_IRI);
            return concept != null; // todo should check for more
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains(LumifyProperties.ONTOLOGY_TITLE.getPropertyName())) {
                return false;
            }
            throw new RuntimeException(e);
        }
    }

    @Override
    public String guessDocumentIRIFromPackage(File file) throws IOException, ZipException {
        ZipFile zipped = new ZipFile(file);
        if (zipped.isValidZipFile()) {
            File tempDir = Files.createTempDir();
            try {
                LOGGER.info("Extracting: %s to %s", file.getAbsoluteFile(), tempDir.getAbsolutePath());
                zipped.extractAll(tempDir.getAbsolutePath());

                File owlFile = findOwlFile(tempDir);
                return guessDocumentIRIFromFile(owlFile);
            } finally {
                FileUtils.deleteDirectory(tempDir);
            }
        } else {
            if (file.isDirectory()) {
                file = findOwlFile(file);
            }
            return guessDocumentIRIFromFile(file);
        }
    }

    public String guessDocumentIRIFromFile(File owlFile) throws IOException {
        FileInputStream owlFileIn = new FileInputStream(owlFile);
        try {
            String owlContents = IOUtils.toString(owlFileIn);

            Pattern iriRegex = Pattern.compile("<owl:Ontology rdf:about=\"(.*?)\">");
            Matcher m = iriRegex.matcher(owlContents);
            if (m.find()) {
                return m.group(1);
            }
            return null;
        } finally {
            owlFileIn.close();
        }
    }

    @Override
    public void importFile(File inFile, IRI documentIRI, Authorizations authorizations) throws Exception {
        checkNotNull(inFile, "inFile cannot be null");
        if (!inFile.exists()) {
            throw new LumifyException("File " + inFile + " does not exist");
        }
        File inDir = inFile.getParentFile();

        FileInputStream inFileIn = new FileInputStream(inFile);
        try {
            LOGGER.debug("importing %s", inFile.getAbsolutePath());
            importFile(inFileIn, documentIRI, inDir, authorizations);
        } finally {
            inFileIn.close();
        }
    }

    private void importFile(InputStream in, IRI documentIRI, File inDir, Authorizations authorizations) throws Exception {
        byte[] inFileData = IOUtils.toByteArray(in);

        Reader inFileReader = new InputStreamReader(new ByteArrayInputStream(inFileData));

        OWLOntologyLoaderConfiguration config = new OWLOntologyLoaderConfiguration();
        OWLOntologyManager m = createOwlOntologyManager(config, documentIRI);

        OWLOntologyDocumentSource documentSource = new ReaderDocumentSource(inFileReader, documentIRI);
        OWLOntology o = m.loadOntologyFromOntologyDocument(documentSource, config);

        storeOntologyFile(new ByteArrayInputStream(inFileData), documentIRI);

        long totalStartTime = System.currentTimeMillis();
        long startTime = System.currentTimeMillis();
        importOntologyClasses(o, inDir, authorizations);
        clearCache(); // this is required to cause a new lookup of classes for data and object properties.
        long endTime = System.currentTimeMillis();
        long importConceptsTime = endTime - startTime;

        startTime = System.currentTimeMillis();
        importDataProperties(o);
        endTime = System.currentTimeMillis();
        long importDataPropertiesTime = endTime - startTime;

        startTime = System.currentTimeMillis();
        importObjectProperties(o);
        clearCache(); // needed to find the relationship for inverse of
        endTime = System.currentTimeMillis();
        long importObjectPropertiesTime = endTime - startTime;

        startTime = System.currentTimeMillis();
        importInverseOfObjectProperties(o);
        endTime = System.currentTimeMillis();
        long importInverseOfObjectPropertiesTime = endTime - startTime;
        long totalEndTime = System.currentTimeMillis();

        LOGGER.debug("import concepts time: %dms", importConceptsTime);
        LOGGER.debug("import data properties time: %dms", importDataPropertiesTime);
        LOGGER.debug("import object properties time: %dms", importObjectPropertiesTime);
        LOGGER.debug("import inverse of object properties time: %dms", importInverseOfObjectPropertiesTime);
        LOGGER.debug("import total time: %dms", totalEndTime - totalStartTime);

        clearCache();
    }

    private void importInverseOfObjectProperties(OWLOntology o) {
        for (OWLObjectProperty objectProperty : o.getObjectPropertiesInSignature()) {
            if (!o.isDeclared(objectProperty, false)) {
                continue;
            }
            importInverseOf(o, objectProperty);
        }
    }

    private void importObjectProperties(OWLOntology o) {
        for (OWLObjectProperty objectProperty : o.getObjectPropertiesInSignature()) {
            if (!o.isDeclared(objectProperty, false)) {
                continue;
            }
            importObjectProperty(o, objectProperty);
        }
    }

    private void importDataProperties(OWLOntology o) {
        for (OWLDataProperty dataTypeProperty : o.getDataPropertiesInSignature()) {
            if (!o.isDeclared(dataTypeProperty, false)) {
                continue;
            }
            importDataProperty(o, dataTypeProperty);
        }
    }

    private void importOntologyClasses(OWLOntology o, File inDir, Authorizations authorizations) throws IOException {
        for (OWLClass ontologyClass : o.getClassesInSignature()) {
            if (!o.isDeclared(ontologyClass, false)) {
                continue;
            }
            importOntologyClass(o, ontologyClass, inDir, authorizations);
        }
    }

    public OWLOntologyManager createOwlOntologyManager(OWLOntologyLoaderConfiguration config, IRI excludeDocumentIRI) throws Exception {
        OWLOntologyManager m = OWLManager.createOWLOntologyManager();
        config.setMissingImportHandlingStrategy(MissingImportHandlingStrategy.SILENT);
        loadOntologyFiles(m, config, excludeDocumentIRI);
        return m;
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

    protected Concept importOntologyClass(OWLOntology o, OWLClass ontologyClass, File inDir, Authorizations authorizations) throws IOException {
        String uri = ontologyClass.getIRI().toString();
        if ("http://www.w3.org/2002/07/owl#Thing".equals(uri)) {
            return getEntityConcept();
        }

        String label = getLabel(o, ontologyClass);
        checkNotNull(label, "label cannot be null or empty: " + uri);
        LOGGER.info("Importing ontology class " + uri + " (label: " + label + ")");

        Concept parent = getParentConcept(o, ontologyClass, inDir, authorizations);
        Concept result = getOrCreateConcept(parent, uri, label, inDir);

        for (OWLAnnotation annotation : ontologyClass.getAnnotations(o)) {
            String annotationIri = annotation.getProperty().getIRI().toString();
            OWLLiteral valueLiteral = (OWLLiteral) annotation.getValue();
            String valueString = valueLiteral.getLiteral();

            if (annotationIri.equals("http://lumify.io#searchable")) {
                boolean searchable = valueString == null || Boolean.parseBoolean(valueString);
                result.setProperty(LumifyProperties.SEARCHABLE.getPropertyName(), searchable, authorizations);
            } else if (annotationIri.equals(LumifyProperties.USER_VISIBLE.getPropertyName())) {
                boolean userVisible = valueString == null || Boolean.parseBoolean(valueString);
                result.setProperty(LumifyProperties.USER_VISIBLE.getPropertyName(), userVisible, authorizations);
            } else if (annotationIri.equals(LumifyProperties.GLYPH_ICON_FILE_NAME.getPropertyName())) {
                setIconProperty(result, inDir, valueString, LumifyProperties.GLYPH_ICON.getPropertyName(), authorizations);
            } else if (annotationIri.equals(LumifyProperties.MAP_GLYPH_ICON_FILE_NAME.getPropertyName())) {
                setIconProperty(result, inDir, valueString, LumifyProperties.MAP_GLYPH_ICON.getPropertyName(), authorizations);
            } else if (annotationIri.equals(LumifyProperties.ADD_RELATED_CONCEPT_WHITE_LIST.getPropertyName())) {
                if (valueString == null || valueString.trim().length() == 0) {
                    continue;
                }
                result.setProperty(LumifyProperties.ADD_RELATED_CONCEPT_WHITE_LIST.getPropertyName(), valueString.trim(), authorizations);
            } else if (annotationIri.equals("http://www.w3.org/2000/01/rdf-schema#label")) {
                continue;
            } else {
                result.setProperty(annotationIri, valueString, authorizations);
            }
        }

        return result;
    }

    protected void setIconProperty(Concept concept, File inDir, String glyphIconFileName, String propertyKey, Authorizations authorizations) throws IOException {
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
                concept.setProperty(propertyKey, value, authorizations);
            } finally {
                iconFileIn.close();
            }
        }
    }

    protected Concept getParentConcept(OWLOntology o, OWLClass ontologyClass, File inDir, Authorizations authorizations) throws IOException {
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

            parent = importOntologyClass(o, superClass, inDir, authorizations);
            if (parent == null) {
                throw new LumifyException("Could not find or create parent: " + superClass);
            }
            return parent;
        } else {
            throw new LumifyException("Unhandled multiple super classes. Found " + superClasses.size() + ", expected 0 or 1.");
        }
    }

    protected void importDataProperty(OWLOntology o, OWLDataProperty dataTypeProperty) {
        String propertyIRI = dataTypeProperty.getIRI().toString();
        String propertyDisplayName = getLabel(o, dataTypeProperty);
        PropertyType propertyType = getPropertyType(o, dataTypeProperty);
        boolean userVisible = getUserVisible(o, dataTypeProperty);
        boolean searchable = getSearchable(o, dataTypeProperty);
        String displayType = getDisplayType(o, dataTypeProperty);
        String propertyGroup = getPropertyGroup(o, dataTypeProperty);
        Double boost = getBoost(o, dataTypeProperty);
        if (propertyType == null) {
            throw new LumifyException("Could not get property type on data property " + propertyIRI);
        }

        List<Concept> domainConcepts = new ArrayList<Concept>();
        for (OWLClassExpression domainClassExpr : dataTypeProperty.getDomains(o)) {
            OWLClass domainClass = domainClassExpr.asOWLClass();
            String domainClassUri = domainClass.getIRI().toString();
            Concept domainConcept = getConceptByIRI(domainClassUri);
            if (domainConcept == null) {
                LOGGER.error("Could not find class with uri: %s", domainClassUri);
            } else {
                LOGGER.info("Adding data property " + propertyIRI + " to class " + domainConcept.getTitle());
                domainConcepts.add(domainConcept);
            }
        }

        Map<String, String> possibleValues = getPossibleValues(o, dataTypeProperty);
        Collection<TextIndexHint> textIndexHints = getTextIndexHints(o, dataTypeProperty);
        addPropertyTo(domainConcepts, propertyIRI, propertyDisplayName, propertyType, possibleValues, textIndexHints, userVisible, searchable, displayType, propertyGroup, boost);
    }

    protected abstract OntologyProperty addPropertyTo(
            List<Concept> concepts,
            String propertyIri,
            String displayName,
            PropertyType dataType,
            Map<String, String> possibleValues,
            Collection<TextIndexHint> textIndexHints,
            boolean userVisible,
            boolean searchable,
            String displayType,
            String propertyGroup,
            Double boost);

    protected void importObjectProperty(OWLOntology o, OWLObjectProperty objectProperty) {
        String iri = objectProperty.getIRI().toString();
        String label = getLabel(o, objectProperty);
        checkNotNull(label, "label cannot be null or empty for " + iri);
        LOGGER.info("Importing ontology object property " + iri + " (label: " + label + ")");

        getOrCreateRelationshipType(getDomainsConcepts(o, objectProperty), getRangesConcepts(o, objectProperty), iri, label);
    }

    protected void importInverseOf(OWLOntology o, OWLObjectProperty objectProperty) {
        String iri = objectProperty.getIRI().toString();
        Relationship fromRelationship = null;

        for (OWLObjectPropertyExpression inverseOf : objectProperty.getInverses(o)) {
            if (inverseOf instanceof OWLObjectProperty) {
                if (fromRelationship == null) {
                    fromRelationship = getRelationshipByIRI(iri);
                    checkNotNull(fromRelationship, "could not find from relationship: " + iri);
                }

                OWLObjectProperty inverseOfOWLObjectProperty = (OWLObjectProperty) inverseOf;
                String inverseOfIri = inverseOfOWLObjectProperty.getIRI().toString();
                Relationship inverseOfRelationship = getRelationshipByIRI(inverseOfIri);
                getOrCreateInverseOfRelationship(fromRelationship, inverseOfRelationship);
            }
        }
    }

    protected abstract void getOrCreateInverseOfRelationship(Relationship fromRelationship, Relationship inverseOfRelationship);

    private Iterable<Concept> getRangesConcepts(OWLOntology o, OWLObjectProperty objectProperty) {
        List<Concept> ranges = new ArrayList<Concept>();
        for (OWLClassExpression rangeClassExpr : objectProperty.getRanges(o)) {
            OWLClass rangeClass = rangeClassExpr.asOWLClass();
            String rangeClassUri = rangeClass.getIRI().toString();
            Concept ontologyClass = getConceptByIRI(rangeClassUri);
            if (ontologyClass == null) {
                LOGGER.error("Could not find class with uri: %s", rangeClassUri);
            } else {
                ranges.add(ontologyClass);
            }
        }
        return ranges;
    }

    private Iterable<Concept> getDomainsConcepts(OWLOntology o, OWLObjectProperty objectProperty) {
        List<Concept> domains = new ArrayList<Concept>();
        for (OWLClassExpression domainClassExpr : objectProperty.getDomains(o)) {
            OWLClass rangeClass = domainClassExpr.asOWLClass();
            String rangeClassUri = rangeClass.getIRI().toString();
            Concept ontologyClass = getConceptByIRI(rangeClassUri);
            if (ontologyClass == null) {
                LOGGER.error("Could not find class with uri: %s", rangeClassUri);
            } else {
                domains.add(ontologyClass);
            }
        }
        return domains;
    }

    protected PropertyType getPropertyType(OWLOntology o, OWLDataProperty dataTypeProperty) {
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
        if ("http://www.w3.org/2001/XMLSchema#date".equals(iri)) {
            return PropertyType.DATE;
        }
        if ("http://www.w3.org/2001/XMLSchema#gYear".equals(iri)) {
            return PropertyType.DATE;
        }
        if ("http://www.w3.org/2001/XMLSchema#gYearMonth".equals(iri)) {
            return PropertyType.DATE;
        }
        if ("http://www.w3.org/2001/XMLSchema#int".equals(iri)) {
            return PropertyType.DOUBLE;
        }
        if ("http://www.w3.org/2001/XMLSchema#double".equals(iri)) {
            return PropertyType.DOUBLE;
        }
        if ("http://www.w3.org/2001/XMLSchema#float".equals(iri)) {
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
        if ("http://www.w3.org/2001/XMLSchema#boolean".equals(iri)) {
            return PropertyType.BOOLEAN;
        }
        if ("http://www.w3.org/2001/XMLSchema#integer".equals(iri)) {
            return PropertyType.INTEGER;
        }
        if ("http://www.w3.org/2001/XMLSchema#nonNegativeInteger".equals(iri)) {
            return PropertyType.INTEGER;
        }
        if ("http://www.w3.org/2001/XMLSchema#positiveInteger".equals(iri)) {
            return PropertyType.INTEGER;
        }
        throw new LumifyException("Unhandled property type " + iri);
    }

    public static String getLabel(OWLOntology o, OWLEntity owlEntity) {
        String bestLabel = owlEntity.getIRI().toString();
        for (OWLAnnotation annotation : owlEntity.getAnnotations(o)) {
            if (annotation.getProperty().isLabel()) {
                OWLLiteral value = (OWLLiteral) annotation.getValue();
                bestLabel = value.getLiteral();
                if (value.getLang() != null && value.getLang().equals("en")) {
                    return bestLabel;
                }
            }
        }
        return bestLabel;
    }

    protected String getColor(OWLOntology o, OWLEntity owlEntity) {
        return getAnnotationValueByUri(o, owlEntity, LumifyProperties.COLOR.getPropertyName());
    }

    protected String getDisplayType(OWLOntology o, OWLEntity owlEntity) {
        return getAnnotationValueByUri(o, owlEntity, LumifyProperties.DISPLAY_TYPE.getPropertyName());
    }

    protected String getPropertyGroup(OWLOntology o, OWLEntity owlEntity) {
        return getAnnotationValueByUri(o, owlEntity, LumifyProperties.PROPERTY_GROUP.getPropertyName());
    }

    protected String getTitleFormula(OWLOntology o, OWLEntity owlEntity) {
        return getAnnotationValueByUri(o, owlEntity, LumifyProperties.TITLE_FORMULA.getPropertyName());
    }

    protected String getSubtitleFormula(OWLOntology o, OWLEntity owlEntity) {
        return getAnnotationValueByUri(o, owlEntity, LumifyProperties.SUBTITLE_FORMULA.getPropertyName());
    }

    protected String getTimeFormula(OWLOntology o, OWLEntity owlEntity) {
        return getAnnotationValueByUri(o, owlEntity, LumifyProperties.TIME_FORMULA.getPropertyName());
    }

    protected Double getBoost(OWLOntology o, OWLEntity owlEntity) {
        String val = getAnnotationValueByUri(o, owlEntity, LumifyProperties.BOOST.getPropertyName());
        if (val == null) {
            return null;
        }
        return Double.parseDouble(val);
    }

    protected boolean getUserVisible(OWLOntology o, OWLEntity owlEntity) {
        String val = getAnnotationValueByUri(o, owlEntity, LumifyProperties.USER_VISIBLE.getPropertyName());
        return val == null || Boolean.parseBoolean(val);
    }

    protected boolean getSearchable(OWLOntology o, OWLEntity owlEntity) {
        String val = getAnnotationValueByUri(o, owlEntity, "http://lumify.io#searchable");
        return val == null || Boolean.parseBoolean(val);
    }

    protected String getGlyphIconFileName(OWLOntology o, OWLEntity owlEntity) {
        return getAnnotationValueByUri(o, owlEntity, LumifyProperties.GLYPH_ICON_FILE_NAME.getPropertyName());
    }

    protected String getMapGlyphIconFileName(OWLOntology o, OWLEntity owlEntity) {
        return getAnnotationValueByUri(o, owlEntity, LumifyProperties.MAP_GLYPH_ICON_FILE_NAME.getPropertyName());
    }

    protected Map<String, String> getPossibleValues(OWLOntology o, OWLEntity owlEntity) {
        String val = getAnnotationValueByUri(o, owlEntity, LumifyProperties.POSSIBLE_VALUES.getPropertyName());
        if (val == null || val.trim().length() == 0) {
            return null;
        }
        return JSONUtil.toMap(new JSONObject(val));
    }

    protected String getAddRelatedConceptWhiteList(OWLOntology o, OWLEntity owlEntity) {
        String val = getAnnotationValueByUri(o, owlEntity, LumifyProperties.ADD_RELATED_CONCEPT_WHITE_LIST.getPropertyName());
        if (val == null || val.trim().length() == 0) {
            return null;
        }
        return val;
    }

    protected Collection<TextIndexHint> getTextIndexHints(OWLOntology o, OWLDataProperty owlEntity) {
        return TextIndexHint.parse(getAnnotationValueByUri(o, owlEntity, LumifyProperties.TEXT_INDEX_HINTS.getPropertyName()));
    }

    protected String getAnnotationValueByUri(OWLOntology o, OWLEntity owlEntity, String uri) {
        for (OWLAnnotation annotation : owlEntity.getAnnotations(o)) {
            if (annotation.getProperty().getIRI().toString().equals(uri)) {
                OWLLiteral value = (OWLLiteral) annotation.getValue();
                return value.getLiteral();
            }
        }
        return null;
    }

    @Override
    public void writePackage(File file, IRI documentIRI, Authorizations authorizations) throws Exception {
        ZipFile zipped = new ZipFile(file);
        if (zipped.isValidZipFile()) {
            File tempDir = Files.createTempDir();
            try {
                LOGGER.info("Extracting: %s to %s", file.getAbsoluteFile(), tempDir.getAbsolutePath());
                zipped.extractAll(tempDir.getAbsolutePath());

                File owlFile = findOwlFile(tempDir);
                importFile(owlFile, documentIRI, authorizations);
            } finally {
                FileUtils.deleteDirectory(tempDir);
            }
        } else {
            importFile(file, documentIRI, authorizations);
        }
    }

    protected File findOwlFile(File fileOrDir) {
        if (fileOrDir.isFile()) {
            return fileOrDir;
        }
        File[] files = fileOrDir.listFiles();
        if (files == null) {
            return null;
        }
        for (File child : files) {
            if (child.isDirectory()) {
                File found = findOwlFile(child);
                if (found != null) {
                    return found;
                }
            } else if (child.getName().toLowerCase().endsWith(".owl")) {
                return child;
            }
        }
        return null;
    }

    @Override
    public void resolvePropertyIds(JSONArray filterJson) throws JSONException {
        for (int i = 0; i < filterJson.length(); i++) {
            JSONObject filter = filterJson.getJSONObject(i);
            if (filter.has("propertyId") && !filter.has("propertyName")) {
                String propertyVertexId = filter.getString("propertyId");
                OntologyProperty property = getPropertyByIRI(propertyVertexId);
                if (property == null) {
                    throw new RuntimeException("Could not find property with id: " + propertyVertexId);
                }
                filter.put("propertyName", property.getTitle());
                filter.put("propertyDataType", property.getDataType());
            }
        }
    }

    @Override
    public Concept getConceptByIRI(String conceptIRI) {
        for (Concept concept : getConceptsWithProperties()) {
            if (concept.getIRI().equals(conceptIRI)) {
                return concept;
            }
        }
        return null;
    }

    @Override
    public OntologyProperty getPropertyByIRI(String propertyIRI) {
        for (OntologyProperty prop : getProperties()) {
            if (prop.getTitle().equals(propertyIRI)) {
                return prop;
            }
        }
        return null;
    }

    public Relationship getRelationshipByIRI(String relationshipIRI) {
        for (Relationship rel : getRelationships()) {
            if (rel.getIRI().equals(relationshipIRI)) {
                return rel;
            }
        }
        return null;
    }

    @Override
    public ClientApiOntology getClientApiObject() {
        Object[] results = ExecutorServiceUtil.runAllAndWait(
                new Callable<Object>() {
                    @Override
                    public Object call() {
                        Iterable<Concept> concepts = getConceptsWithProperties();
                        return Concept.toClientApiConcepts(concepts);
                    }
                },
                new Callable<Object>() {
                    @Override
                    public Object call() {
                        Iterable<OntologyProperty> properties = getProperties();
                        return OntologyProperty.toClientApiProperties(properties);
                    }
                },
                new Callable<Object>() {
                    @Override
                    public Object call() {
                        Iterable<Relationship> relationships = getRelationships();
                        return Relationship.toClientApiRelationships(relationships);
                    }
                }
        );

        ClientApiOntology ontology = new ClientApiOntology();
        ontology.addAllConcepts((Collection<ClientApiOntology.Concept>) results[0]);
        ontology.addAllProperties((Collection<ClientApiOntology.Property>) results[1]);
        ontology.addAllRelationships((Collection<ClientApiOntology.Relationship>) results[2]);

        return ontology;
    }
}
