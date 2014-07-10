package io.lumify.gdelt;

import com.altamiracorp.bigtable.model.accumulo.AccumuloSession;
import io.lumify.core.config.Configuration;
import io.lumify.core.model.audit.Audit;
import io.lumify.core.model.audit.AuditAction;
import io.lumify.core.model.ontology.OntologyLumifyProperties;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.user.SystemUser;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import io.lumify.core.version.VersionService;
import io.lumify.securegraph.model.audit.SecureGraphAuditRepository;
import org.apache.accumulo.core.data.Mutation;
import org.apache.hadoop.io.LongWritable;
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
import org.securegraph.util.MapUtils;

import java.io.IOException;
import java.text.ParseException;
import java.util.Map;

public class GDELTMapper extends ElementMapper<LongWritable, Text, Text, Mutation> {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(GDELTMapper.class);

    private AccumuloGraph graph;
    private Visibility visibility;
    private AccumuloAuthorizations authorizations;
    private SystemUser user;
    private SecureGraphAuditRepository auditRepository;
    private UserRepository userRepository;

    @Override
    protected void setup(Context context) throws IOException, InterruptedException {
        super.setup(context);
        Map configurationMap = SecureGraphMRUtils.toMap(context.getConfiguration());
        this.graph = (AccumuloGraph) new GraphFactory().createGraph(MapUtils.getAllWithPrefix(configurationMap, "graph"));
        this.visibility = new Visibility("");
        this.authorizations = new AccumuloAuthorizations();
        this.user = new SystemUser(null);
        VersionService versionService = new VersionService();
        Configuration configuration = new Configuration(configurationMap);
        this.auditRepository = new SecureGraphAuditRepository(null, versionService, configuration, null, userRepository);
    }

    @Override
    protected void map(LongWritable filePosition, Text line, Context context) {
        try {
            safeMap(filePosition, line, context);
        } catch (Exception ex) {
            context.getCounter(GDELTImportCounters.ERRORS).increment(1);
            LOGGER.error("failed mapping " + filePosition, ex);
        }
    }

    private void safeMap(LongWritable filePosition, Text line, Context context) throws IOException, InterruptedException, ParseException {
        GDELTEvent event = GDELTParser.parseLine(line.toString());
        VertexBuilder eventVertexBuilder = prepareVertex(generateEventId(event), visibility);
        GDELTProperties.CONCEPT_TYPE.setProperty(eventVertexBuilder, GDELTConstants.EVENT_CONCEPT_URI, visibility);
        GDELTProperties.GLOBAL_EVENT_ID.setProperty(eventVertexBuilder, event.getGlobalEventId(), visibility);
        GDELTProperties.EVENT_DATE.setProperty(eventVertexBuilder, event.getEventDate(), visibility);

        Vertex eventVertex = eventVertexBuilder.save(authorizations);

        // audit vertex
        Text key = new Text(Audit.TABLE_NAME);
        Audit audit = auditRepository.createAudit(AuditAction.CREATE, eventVertex.getId(), "GDELT MR", "", user, visibility);
        context.write(key, AccumuloSession.createMutationFromRow(audit));

        context.getCounter(GDELTImportCounters.EVENTS_PROCESSED).increment(1);
    }

    private String generateEventId(GDELTEvent event) {
        return "GDELTEvent_" + event.getGlobalEventId();
    }

    @Override
    protected void saveDataMutation(Context context, Text tableName, Mutation mutation) throws IOException, InterruptedException {
        context.write(tableName, mutation);
    }

    @Override
    protected void saveEdgeMutation(Context context, Text tableName, Mutation mutation) throws IOException, InterruptedException {
        context.write(tableName, mutation);
    }

    @Override
    protected void saveVertexMutation(Context context, Text tableName, Mutation mutation) throws IOException, InterruptedException {
        context.write(tableName, mutation);
    }

    @Override
    protected IdGenerator getIdGenerator() {
        return this.graph.getIdGenerator();
    }
}
