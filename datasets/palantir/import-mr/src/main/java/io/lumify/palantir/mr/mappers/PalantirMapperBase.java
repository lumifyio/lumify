package io.lumify.palantir.mr.mappers;

import com.google.inject.Inject;
import io.lumify.core.bootstrap.InjectHelper;
import io.lumify.core.mapreduce.LumifyElementMapperBase;
import io.lumify.core.model.ontology.OntologyRepository;
import io.lumify.core.model.user.AuthorizationRepository;
import io.lumify.core.security.VisibilityTranslator;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import io.lumify.palantir.model.PtLinkType;
import io.lumify.palantir.model.PtObjectType;
import io.lumify.palantir.mr.ImportMR;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.SequenceFile;
import org.securegraph.accumulo.AccumuloAuthorizations;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public abstract class PalantirMapperBase<KEYIN, VALUEIN> extends LumifyElementMapperBase<KEYIN, VALUEIN> {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(PalantirMapperBase.class);
    public static final String ID_PREFIX = "PALANTIR_";
    private String baseIri;
    private AccumuloAuthorizations authorizations;
    private VisibilityTranslator visibilityTranslator;
    private AuthorizationRepository authorizationRepository;
    private OntologyRepository ontologyRepository;
    private Map<Long, PtObjectType> objectTypes;
    private Map<Long, PtLinkType> linkTypes;

    @Override
    protected void setup(Context context) throws IOException, InterruptedException {
        super.setup(context);
        InjectHelper.inject(this);
        baseIri = context.getConfiguration().get(ImportMR.CONF_BASE_IRI);
        authorizations = new AccumuloAuthorizations();
    }

    protected PtObjectType getObjectType(long type) {
        return objectTypes.get(type);
    }

    protected void loadObjectTypes(Context context) throws IOException {
        Path inDir = new Path(context.getConfiguration().get(ImportMR.CONF_IN_DIR));
        Path inFilePath = new Path(inDir, "PtObjectType.seq");
        LOGGER.info("reading: %s", inFilePath.toString());
        try (SequenceFile.Reader reader = new SequenceFile.Reader(
                context.getConfiguration(),
                SequenceFile.Reader.file(inFilePath)
        )) {
            Class<?> keyClass = reader.getKeyClass();
            Class<?> valueClass = reader.getValueClass();
            LOGGER.debug("Found key class: %s", keyClass.getName());
            LOGGER.debug("Found value class: %s", valueClass.getName());
            objectTypes = new HashMap<>();

            LongWritable key = new LongWritable();
            PtObjectType ptObjectType = new PtObjectType();
            while (reader.next(key, ptObjectType)) {
                objectTypes.put(key.get(), ptObjectType);
                ptObjectType = new PtObjectType();
            }
        }
    }

    protected PtLinkType getLinkType(long type) {
        return linkTypes.get(type);
    }

    protected void loadLinkTypes(Context context) throws IOException {
        Path inDir = new Path(context.getConfiguration().get(ImportMR.CONF_IN_DIR));
        Path inFilePath = new Path(inDir, "PtLinkType.seq");
        LOGGER.info("reading: %s", inFilePath.toString());
        try (SequenceFile.Reader reader = new SequenceFile.Reader(
                context.getConfiguration(),
                SequenceFile.Reader.file(inFilePath)
        )) {
            Class<?> keyClass = reader.getKeyClass();
            Class<?> valueClass = reader.getValueClass();
            LOGGER.debug("Found key class: %s", keyClass.getName());
            LOGGER.debug("Found value class: %s", valueClass.getName());
            linkTypes = new HashMap<>();

            LongWritable key = new LongWritable();
            PtLinkType ptLinkType = new PtLinkType();
            while (reader.next(key, ptLinkType)) {
                linkTypes.put(key.get(), ptLinkType);
                ptLinkType = new PtLinkType();
            }
        }
    }

    public String getBaseIri() {
        return baseIri;
    }

    public AccumuloAuthorizations getAuthorizations() {
        return authorizations;
    }

    @Inject
    public final void setVisibilityTranslator(VisibilityTranslator visibilityTranslator) {
        this.visibilityTranslator = visibilityTranslator;
    }

    @Inject
    public final void setAuthorizationRepository(AuthorizationRepository authorizationRepository) {
        this.authorizationRepository = authorizationRepository;
    }

    @Inject
    public final void setOntologyRepository(OntologyRepository ontologyRepository) {
        this.ontologyRepository = ontologyRepository;
    }

    protected OntologyRepository getOntologyRepository() {
        return ontologyRepository;
    }

    protected VisibilityTranslator getVisibilityTranslator() {
        return visibilityTranslator;
    }

    protected AuthorizationRepository getAuthorizationRepository() {
        return authorizationRepository;
    }
}
