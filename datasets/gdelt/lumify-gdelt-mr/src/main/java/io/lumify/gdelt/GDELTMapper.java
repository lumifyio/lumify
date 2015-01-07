package io.lumify.gdelt;

import com.altamiracorp.bigtable.model.accumulo.AccumuloSession;
import io.lumify.core.config.Configuration;
import io.lumify.core.config.HashMapConfigurationLoader;
import io.lumify.core.model.audit.Audit;
import io.lumify.core.model.audit.AuditAction;
import io.lumify.core.model.properties.types.LumifyProperty;
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
import org.securegraph.type.GeoPoint;
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
        Map configurationMap = SecureGraphMRUtils.toMap(context.getConfiguration());
        this.parser = new GDELTParser();
        this.graph = (AccumuloGraph) new GraphFactory().createGraph(MapUtils.getAllWithPrefix(configurationMap, "graph"));
        this.visibility = new Visibility("");
        this.authorizations = new AccumuloAuthorizations();
        this.user = new SystemUser(null);
        VersionService versionService = new VersionService();
        Configuration configuration = new HashMapConfigurationLoader(configurationMap).createConfiguration();
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
        context.getCounter(GDELTImportCounters.EVENTS_ATTEMPTED).increment(1);

        // event vertex
        VertexBuilder eventVertexBuilder = prepareVertex(generateEventId(event), visibility);
        GDELTProperties.CONCEPT_TYPE.setProperty(eventVertexBuilder, GDELTConstants.EVENT_CONCEPT_URI, visibility);
        GDELTProperties.GLOBAL_EVENT_ID.setProperty(eventVertexBuilder, event.getGlobalEventId(), visibility);
        GDELTProperties.EVENT_DATE_OF_OCCURRENCE.setProperty(eventVertexBuilder, event.getDateOfOccurrence(), visibility);
        GDELTProperties.EVENT_CODE.setProperty(eventVertexBuilder, event.getEventCode(), visibility);
        setOptionalProperty(GDELTProperties.EVENT_IS_ROOT_EVENT, eventVertexBuilder, event.isRootEvent());
        setOptionalProperty(GDELTProperties.EVENT_BASE_CODE, eventVertexBuilder, event.getEventBaseCode());
        setOptionalProperty(GDELTProperties.EVENT_ROOT_CODE, eventVertexBuilder, event.getEventRootCode());
        setOptionalProperty(GDELTProperties.EVENT_QUAD_CLASS, eventVertexBuilder, event.getQuadClass());
        setOptionalProperty(GDELTProperties.EVENT_GOLDSTEIN_SCALE, eventVertexBuilder, event.getGoldsteinScale());
        setOptionalProperty(GDELTProperties.EVENT_NUM_MENTIONS, eventVertexBuilder, event.getNumMentions());
        setOptionalProperty(GDELTProperties.EVENT_NUM_SOURCES, eventVertexBuilder, event.getNumSources());
        setOptionalProperty(GDELTProperties.EVENT_NUM_ARTICLES, eventVertexBuilder, event.getNumArticles());
        setOptionalProperty(GDELTProperties.EVENT_AVG_TONE, eventVertexBuilder, event.getAverageTone());
        setOptionalProperty(GDELTProperties.EVENT_DATE_ADDED, eventVertexBuilder, event.getDateAdded());
        setOptionalProperty(GDELTProperties.EVENT_SOURCE_URL, eventVertexBuilder, event.getSourceUrl());

        setGeolocationProperty(event, eventVertexBuilder);
        Vertex eventVertex = eventVertexBuilder.save(authorizations);

        // audit event
        Text eventKey = new Text(Audit.TABLE_NAME);
        Audit auditEvent = auditRepository.createAudit(AuditAction.CREATE, eventVertex.getId(), "GDELT MR", "", user, visibility);
        context.write(eventKey, AccumuloSession.createMutationFromRow(auditEvent));

        context.getCounter(GDELTImportCounters.EVENTS_PROCESSED).increment(1);
        return eventVertex;
    }

    private void setGeolocationProperty(GDELTEvent event, VertexBuilder eventVertexBuilder) {
        if (event.getActionGeoFullName() != null) {
            GeoPoint point = new GeoPoint(event.getActionGeoLatitude(), event.getActionGeoLongitude(), event.getActionGeoFullName());
            GDELTProperties.EVENT_GEOLOCATION.setProperty(eventVertexBuilder, point, visibility);
        }
    }

    private void importActor1(Context context, GDELTEvent event, Vertex eventVertex) throws IOException, InterruptedException {
        GDELTActor actor = event.getActor1();

        if (actor.getCode() == null) {
            context.getCounter(GDELTImportCounters.ACTORS_SKIPPED).increment(1);
            return;
        }

        // actor 1 vertex
        Vertex actorVertex = importActor(actor, context);

        // audit actor 1
        Text key = new Text(Audit.TABLE_NAME);
        Audit auditActor = auditRepository.createAudit(AuditAction.CREATE, actorVertex.getId(), "GDELT MR", "", user, visibility);
        context.write(key, AccumuloSession.createMutationFromRow(auditActor));

        context.getCounter(GDELTImportCounters.EDGES_ATTEMPTED).increment(1);

        // actor 1 to event edge
        EdgeBuilder actor1EdgeBuilder = prepareEdge(generateActor1ToEventEdgeId(actorVertex, eventVertex), actorVertex, eventVertex, GDELTProperties.ACTOR1_TO_EVENT_EDGE, visibility);
        Edge actor1ToEventEdge = actor1EdgeBuilder.save(authorizations);

        // audit actor 1 to event edge
        Text actor1ToEventEdgeKey = new Text(Audit.TABLE_NAME);
        Audit auditActor1ToEventEdge = auditRepository.createAudit(AuditAction.CREATE, actor1ToEventEdge.getId(), "GDELT MR", "", user, visibility);
        context.write(actor1ToEventEdgeKey, AccumuloSession.createMutationFromRow(auditActor1ToEventEdge));

        context.getCounter(GDELTImportCounters.EDGES_PROCESSED).increment(1);
    }

    private void importActor2(Context context, GDELTEvent event, Vertex eventVertex) throws IOException, InterruptedException {
        GDELTActor actor = event.getActor2();

        if (actor.getCode() == null) {
            context.getCounter(GDELTImportCounters.ACTORS_SKIPPED).increment(1);
            return;
        }

        // actor 2
        Vertex actorVertex = importActor(actor, context);

        // audit actor 2
        Text key = new Text(Audit.TABLE_NAME);
        Audit auditActor = auditRepository.createAudit(AuditAction.CREATE, actorVertex.getId(), "GDELT MR", "", user, visibility);
        context.write(key, AccumuloSession.createMutationFromRow(auditActor));

        context.getCounter(GDELTImportCounters.EDGES_ATTEMPTED).increment(1);

        // event to actor 2 edge
        EdgeBuilder eventToActor2EdgeBuilder = prepareEdge(generateEventToActor2EdgeId(actorVertex, eventVertex), eventVertex, actorVertex, GDELTProperties.EVENT_TO_ACTOR2_EDGE, visibility);
        Edge eventToActor2Edge = eventToActor2EdgeBuilder.save(authorizations);

        // audit event to actor 2 edge
        Text eventToActor2EdgeKey = new Text(Audit.TABLE_NAME);
        Audit auditEventToActor2Edge = auditRepository.createAudit(AuditAction.CREATE, eventToActor2Edge.getId(), "GDELT MR", "", user, visibility);
        context.write(eventToActor2EdgeKey, AccumuloSession.createMutationFromRow(auditEventToActor2Edge));

        context.getCounter(GDELTImportCounters.EDGES_PROCESSED).increment(1);
    }

    private Vertex importActor(GDELTActor actor, Context context) {
        context.getCounter(GDELTImportCounters.ACTORS_ATTEMPTED).increment(1);

        VertexBuilder vertexBuilder = prepareVertex(generateActorId(actor), visibility);
        GDELTProperties.CONCEPT_TYPE.setProperty(vertexBuilder, GDELTConstants.ACTOR_CONCEPT_URI, visibility);
        GDELTProperties.ACTOR_CODE.setProperty(vertexBuilder, actor.getCode(), visibility);
        setOptionalProperty(GDELTProperties.ACTOR_NAME, vertexBuilder, actor.getName());
        setOptionalProperty(GDELTProperties.ACTOR_COUNTRY_CODE, vertexBuilder, actor.getCountryCode());
        setOptionalProperty(GDELTProperties.ACTOR_KNOWN_GROUP_CODE, vertexBuilder, actor.getKnownGroupCode());
        setOptionalProperty(GDELTProperties.ACTOR_ETHNIC_CODE, vertexBuilder, actor.getEthnicCode());
        addOptionPropertyValue(GDELTProperties.ACTOR_RELIGION_CODE, vertexBuilder, "1", actor.getReligion1Code());
        addOptionPropertyValue(GDELTProperties.ACTOR_RELIGION_CODE, vertexBuilder, "2", actor.getReligion2Code());
        addOptionPropertyValue(GDELTProperties.ACTOR_TYPE_CODE, vertexBuilder, "1", actor.getType1Code());
        addOptionPropertyValue(GDELTProperties.ACTOR_TYPE_CODE, vertexBuilder, "2", actor.getType2Code());
        addOptionPropertyValue(GDELTProperties.ACTOR_TYPE_CODE, vertexBuilder, "3", actor.getType3Code());

        context.getCounter(GDELTImportCounters.ACTORS_PROCESSED).increment(1);

        return vertexBuilder.save(authorizations);
    }

    private void setOptionalProperty(LumifyProperty property, VertexBuilder vertexBuilder, Object propertyValue) {
        if (propertyValue != null) {
            property.setProperty(vertexBuilder, propertyValue, visibility);
        }
    }

    private void addOptionPropertyValue(LumifyProperty property, VertexBuilder vertexBuilder, String propertyKey, Object propertyValue) {
        if (propertyValue != null) {
            property.addPropertyValue(vertexBuilder, propertyKey, propertyValue, visibility);
        }
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
    public IdGenerator getIdGenerator() {
        return this.graph.getIdGenerator();
    }
}
