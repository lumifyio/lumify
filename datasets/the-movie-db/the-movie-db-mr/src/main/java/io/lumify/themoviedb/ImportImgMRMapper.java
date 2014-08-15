package io.lumify.themoviedb;

import com.google.inject.Inject;
import io.lumify.core.config.HashMapConfigurationLoader;
import io.lumify.core.exception.LumifyException;
import io.lumify.core.model.properties.LumifyProperties;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.user.SystemUser;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import io.lumify.core.version.VersionService;
import io.lumify.securegraph.model.audit.SecureGraphAuditRepository;
import org.apache.accumulo.core.data.Mutation;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.Text;
import org.securegraph.GraphFactory;
import org.securegraph.Vertex;
import org.securegraph.VertexBuilder;
import org.securegraph.Visibility;
import org.securegraph.accumulo.AccumuloAuthorizations;
import org.securegraph.accumulo.AccumuloGraph;
import org.securegraph.accumulo.mapreduce.ElementMapper;
import org.securegraph.accumulo.mapreduce.SecureGraphMRUtils;
import org.securegraph.id.IdGenerator;
import org.securegraph.property.StreamingPropertyValue;
import org.securegraph.util.MapUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Map;

public class ImportImgMRMapper extends ElementMapper<SequenceFileKey, BytesWritable, Text, Mutation> {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(ImportImgMRMapper.class);
    public static final String MULTI_VALUE_KEY = ImportJsonMR.class.getName();
    public static final String SOURCE = "TheMovieDb.org";
    public static final String CONFIG_SOURCE_FILE_NAME = "sourceFileName";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    private static final SimpleDateFormat DATE_YEAR_FORMAT = new SimpleDateFormat("yyyy");
    private AccumuloGraph graph;
    private Visibility visibility;
    private AccumuloAuthorizations authorizations;
    private SystemUser user;
    private String sourceFileName;
    private UserRepository userRepository;
    private SecureGraphAuditRepository auditRepository;

    @Override
    protected void setup(Context context) throws IOException, InterruptedException {
        super.setup(context);
        Map configurationMap = SecureGraphMRUtils.toMap(context.getConfiguration());
        this.graph = (AccumuloGraph) new GraphFactory().createGraph(MapUtils.getAllWithPrefix(configurationMap, "graph"));
        this.visibility = new Visibility("");
        this.authorizations = new AccumuloAuthorizations();
        this.user = new SystemUser(null);
        VersionService versionService = new VersionService();
        io.lumify.core.config.Configuration configuration = new HashMapConfigurationLoader(configurationMap).createConfiguration();
        this.auditRepository = new SecureGraphAuditRepository(null, versionService, configuration, null, userRepository);
        this.sourceFileName = context.getConfiguration().get(CONFIG_SOURCE_FILE_NAME);
    }

    @Override
    protected void map(SequenceFileKey key, BytesWritable imageData, Context context) throws IOException, InterruptedException {
        try {
            safeMap(key, imageData.getBytes(), context);
        } catch (Exception ex) {
            LOGGER.error("failed mapping " + key, ex);
        }
    }

    private void safeMap(SequenceFileKey key, byte[] imageData, Context context) throws IOException, InterruptedException, ParseException {
        String conceptType;
        String sourceVertexId;
        String edgeLabel;

        switch (key.getRecordType()) {
            case PERSON:
                conceptType = TheMovieDbOntology.CONCEPT_TYPE_PROFILE_IMAGE;
                edgeLabel = TheMovieDbOntology.EDGE_LABEL_HAS_PROFILE_IMAGE;
                sourceVertexId = TheMovieDbOntology.getPersonVertexId(key.getId());
                break;
            case MOVIE:
                conceptType = TheMovieDbOntology.CONCEPT_TYPE_POSTER_IMAGE;
                edgeLabel = TheMovieDbOntology.EDGE_LABEL_HAS_POSTER_IMAGE;
                sourceVertexId = TheMovieDbOntology.getMovieVertexId(key.getId());
                break;
            case PRODUCTION_COMPANY:
                conceptType = TheMovieDbOntology.CONCEPT_TYPE_LOGO;
                edgeLabel = TheMovieDbOntology.EDGE_LABEL_HAS_LOGO;
                sourceVertexId = TheMovieDbOntology.getProductionCompanyVertexId(key.getId());
                break;
            default:
                throw new LumifyException("Invalid record type: " + key.getRecordType());
        }

        String edgeId = TheMovieDbOntology.getHasImageEdgeId(key.getId(), key.getImagePath());
        String title = key.getTitle();
        String vertexId = TheMovieDbOntology.getImageVertexId(key.getImagePath());
        VertexBuilder m = graph.prepareVertex(vertexId, visibility);
        LumifyProperties.CONCEPT_TYPE.addPropertyValue(m, MULTI_VALUE_KEY, conceptType, visibility);
        LumifyProperties.SOURCE.addPropertyValue(m, MULTI_VALUE_KEY, SOURCE, visibility);
        StreamingPropertyValue rawValue = new StreamingPropertyValue(new ByteArrayInputStream(imageData), byte[].class);
        rawValue.store(true);
        rawValue.searchIndex(false);
        LumifyProperties.RAW.addPropertyValue(m, MULTI_VALUE_KEY, rawValue, visibility);
        LumifyProperties.TITLE.addPropertyValue(m, MULTI_VALUE_KEY, "Image of " + title, visibility);
        Vertex profileImageVertex = m.save(authorizations);

        Vertex sourceVertex = graph.addVertex(sourceVertexId, visibility, authorizations);

        graph.addEdge(edgeId, sourceVertex, profileImageVertex, edgeLabel, visibility, authorizations);
        LumifyProperties.ENTITY_HAS_IMAGE_VERTEX_ID.addPropertyValue(sourceVertex, MULTI_VALUE_KEY, profileImageVertex.getId(), visibility, authorizations);

        context.getCounter(TheMovieDbImportCounters.IMAGES_PROCESSED).increment(1);
    }

    @Override
    protected void saveDataMutation(Context context, Text dataTableName, Mutation m) throws IOException, InterruptedException {
        context.write(ImportJsonMR.getKey(dataTableName.toString(), m.getRow()), m);
    }

    @Override
    protected void saveEdgeMutation(Context context, Text edgesTableName, Mutation m) throws IOException, InterruptedException {
        context.write(ImportJsonMR.getKey(edgesTableName.toString(), m.getRow()), m);
    }

    @Override
    protected void saveVertexMutation(Context context, Text verticesTableName, Mutation m) throws IOException, InterruptedException {
        context.write(ImportJsonMR.getKey(verticesTableName.toString(), m.getRow()), m);
    }

    @Override
    protected IdGenerator getIdGenerator() {
        return this.graph.getIdGenerator();
    }

    @Inject
    public void setUserRepository(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
}
