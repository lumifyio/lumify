package io.lumify.gdelt;

import com.altamiracorp.bigtable.model.accumulo.AccumuloSession;
import io.lumify.core.config.Configuration;
import io.lumify.core.model.audit.Audit;
import io.lumify.core.model.audit.AuditAction;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.user.SystemUser;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import io.lumify.core.version.VersionService;
import io.lumify.securegraph.model.audit.SecureGraphAuditRepository;
import org.apache.accumulo.core.data.Mutation;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.securegraph.*;
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

    private GDELTParser parser;
    private AccumuloGraph graph;
    private Visibility visibility;
    private AccumuloAuthorizations authorizations;
    private SystemUser user;
    private SecureGraphAuditRepository auditRepository;
    private UserRepository userRepository;

    @Override
    protected void setup(Context context) throws IOException, InterruptedException {
        super.setup(context);
        this.parser = new GDELTParser();
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
        GDELTEvent event = parser.parseLine(line.toString());
        Vertex eventVertex = importEvent(context, event);
        importActor1(context, event, eventVertex);
        importActor2(context, event, eventVertex);
    }

    private Vertex importEvent(Context context, GDELTEvent event) throws IOException, InterruptedException {
        // event vertex
        VertexBuilder eventVertexBuilder = prepareVertex(generateEventId(event), visibility);
        GDELTProperties.CONCEPT_TYPE.setProperty(eventVertexBuilder, GDELTConstants.EVENT_CONCEPT_URI, visibility);
        GDELTProperties.GLOBAL_EVENT_ID.setProperty(eventVertexBuilder, event.getGlobalEventId(), visibility);
        GDELTProperties.EVENT_DATE_OF_OCCURRENCE.setProperty(eventVertexBuilder, event.getDateOfOccurrence(), visibility);
        GDELTProperties.EVENT_IS_ROOT_EVENT.setProperty(eventVertexBuilder, event.isRootEvent(), visibility);
        GDELTProperties.EVENT_CODE.setProperty(eventVertexBuilder, event.getEventCode(), visibility);
        GDELTProperties.EVENT_BASE_CODE.setProperty(eventVertexBuilder, event.getEventBaseCode(), visibility);
        GDELTProperties.EVENT_ROOT_CODE.setProperty(eventVertexBuilder, event.getEventRootCode(), visibility);
        GDELTProperties.EVENT_QUAD_CLASS.setProperty(eventVertexBuilder, event.getQuadClass(), visibility);
        GDELTProperties.EVENT_GOLDSTEIN_SCALE.setProperty(eventVertexBuilder, event.getGoldsteinScale(), visibility);
        GDELTProperties.EVENT_NUM_MENTIONS.setProperty(eventVertexBuilder, event.getNumMentions(), visibility);
        GDELTProperties.EVENT_NUM_SOURCES.setProperty(eventVertexBuilder, event.getNumSources(), visibility);
        GDELTProperties.EVENT_NUM_ARTICLES.setProperty(eventVertexBuilder, event.getNumArticles(), visibility);
        Vertex eventVertex = eventVertexBuilder.save(authorizations);

        // audit event
        Text eventKey = new Text(Audit.TABLE_NAME);
        Audit auditEvent = auditRepository.createAudit(AuditAction.CREATE, eventVertex.getId(), "GDELT MR", "", user, visibility);
        context.write(eventKey, AccumuloSession.createMutationFromRow(auditEvent));

        context.getCounter(GDELTImportCounters.EVENTS_PROCESSED).increment(1);
        return eventVertex;
    }

    private void importActor1(Context context, GDELTEvent event, Vertex eventVertex) throws IOException, InterruptedException {
        // actor 1 vertex
        GDELTActor actor1 = event.getActor1();
        VertexBuilder actor1VertexBuilder = prepareVertex(generateActorId(actor1), visibility);
        GDELTProperties.ACTOR_CODE.setProperty(actor1VertexBuilder, actor1.getCode(), visibility);
        GDELTProperties.ACTOR_NAME.setProperty(actor1VertexBuilder, actor1.getName(), visibility);
        GDELTProperties.ACTOR_COUNTRY_CODE.setProperty(actor1VertexBuilder, actor1.getCountryCode(), visibility);
        GDELTProperties.ACTOR_ETHNIC_CODE.setProperty(actor1VertexBuilder, actor1.getEthnicCode(), visibility);
        GDELTProperties.ACTOR_RELIGION_CODE.addPropertyValue(actor1VertexBuilder, "1", actor1.getReligion1Code(), visibility);
        GDELTProperties.ACTOR_RELIGION_CODE.addPropertyValue(actor1VertexBuilder, "2", actor1.getReligion2Code(), visibility);
        GDELTProperties.ACTOR_TYPE_CODE.addPropertyValue(actor1VertexBuilder, "1", actor1.getType1Code(), visibility);
        GDELTProperties.ACTOR_TYPE_CODE.addPropertyValue(actor1VertexBuilder, "2", actor1.getType2Code(), visibility);
        GDELTProperties.ACTOR_TYPE_CODE.addPropertyValue(actor1VertexBuilder, "3", actor1.getType3Code(), visibility);
        Vertex actor1Vertex = actor1VertexBuilder.save(authorizations);

        // audit actor 1
        Text actor1Key = new Text(Audit.TABLE_NAME);
        Audit auditActor1 = auditRepository.createAudit(AuditAction.CREATE, actor1Vertex.getId(), "GDELT MR", "", user, visibility);
        context.write(actor1Key, AccumuloSession.createMutationFromRow(auditActor1));

        context.getCounter(GDELTImportCounters.ACTORS_PROCESSED).increment(1);

        // actor 1 to event edge
        EdgeBuilder actor1EdgeBuilder = prepareEdge(generateActor1ToEventEdgeId(actor1Vertex, eventVertex), actor1Vertex, eventVertex, GDELTProperties.ACTOR1_TO_EVENT_EDGE, visibility);
        Edge actor1ToEventEdge = actor1EdgeBuilder.save(authorizations);

        // audit actor 1 to event edge
        Text actor1ToEventEdgeKey = new Text(Audit.TABLE_NAME);
        Audit auditActor1ToEventEdge = auditRepository.createAudit(AuditAction.CREATE, actor1ToEventEdge.getId(), "GDELT MR", "", user, visibility);
        context.write(actor1ToEventEdgeKey, AccumuloSession.createMutationFromRow(auditActor1ToEventEdge));

        context.getCounter(GDELTImportCounters.EDGES_PROCESSED).increment(1);
    }

    private void importActor2(Context context, GDELTEvent event, Vertex eventVertex) throws IOException, InterruptedException {
        // actor 2 vertex
        GDELTActor actor2 = event.getActor1();
        VertexBuilder actor2VertexBuilder = prepareVertex(generateActorId(actor2), visibility);
        GDELTProperties.ACTOR_CODE.setProperty(actor2VertexBuilder, actor2.getCode(), visibility);
        GDELTProperties.ACTOR_NAME.setProperty(actor2VertexBuilder, actor2.getName(), visibility);
        GDELTProperties.ACTOR_COUNTRY_CODE.setProperty(actor2VertexBuilder, actor2.getCountryCode(), visibility);
        GDELTProperties.ACTOR_ETHNIC_CODE.setProperty(actor2VertexBuilder, actor2.getEthnicCode(), visibility);
        GDELTProperties.ACTOR_RELIGION_CODE.addPropertyValue(actor2VertexBuilder, "1", actor2.getReligion1Code(), visibility);
        GDELTProperties.ACTOR_RELIGION_CODE.addPropertyValue(actor2VertexBuilder, "2", actor2.getReligion2Code(), visibility);
        GDELTProperties.ACTOR_TYPE_CODE.addPropertyValue(actor2VertexBuilder, "1", actor2.getType1Code(), visibility);
        GDELTProperties.ACTOR_TYPE_CODE.addPropertyValue(actor2VertexBuilder, "2", actor2.getType2Code(), visibility);
        GDELTProperties.ACTOR_TYPE_CODE.addPropertyValue(actor2VertexBuilder, "3", actor2.getType3Code(), visibility);
        Vertex actor2Vertex = actor2VertexBuilder.save(authorizations);

        // audit actor 2
        Text actor2Key = new Text(Audit.TABLE_NAME);
        Audit auditActor2 = auditRepository.createAudit(AuditAction.CREATE, actor2Vertex.getId(), "GDELT MR", "", user, visibility);
        context.write(actor2Key, AccumuloSession.createMutationFromRow(auditActor2));

        context.getCounter(GDELTImportCounters.ACTORS_PROCESSED).increment(1);

        // event to actor 2 edge
        EdgeBuilder eventToActor2EdgeBuilder = prepareEdge(generateEventToActor2EdgeId(actor2Vertex, eventVertex), eventVertex, actor2Vertex, GDELTProperties.EVENT_TO_ACTOR2_EDGE, visibility);
        Edge eventToActor2Edge = eventToActor2EdgeBuilder.save(authorizations);

        // audit event to actor 2 edge
        Text eventToActor2EdgeKey = new Text(Audit.TABLE_NAME);
        Audit auditEventToActor2Edge = auditRepository.createAudit(AuditAction.CREATE, eventToActor2Edge.getId(), "GDELT MR", "", user, visibility);
        context.write(eventToActor2EdgeKey, AccumuloSession.createMutationFromRow(auditEventToActor2Edge));

        context.getCounter(GDELTImportCounters.EDGES_PROCESSED).increment(1);
    }

    private String generateEventId(GDELTEvent event) {
        return "GDELTEvent_" + event.getGlobalEventId();
    }

    private String generateActorId(GDELTActor actor) {
        return "GDELTActor_" + actor.getId();
    }


    private String generateActor1ToEventEdgeId(Vertex actor1Vertex, Vertex eventVertex) {
        return "GDELTActor1ToEvent_" + actor1Vertex.getId() + eventVertex.getId();
    }

    private String generateEventToActor2EdgeId(Vertex actor2Vertex, Vertex eventVertex) {
        return "GDELTEventToActor2_" + eventVertex.getId() + actor2Vertex.getId();
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
