package io.lumify.friendster;

import com.altamiracorp.bigtable.model.accumulo.AccumuloSession;
import com.google.inject.Inject;
import io.lumify.core.config.HashMapConfigurationLoader;
import io.lumify.core.model.audit.Audit;
import io.lumify.core.model.audit.AuditAction;
import io.lumify.core.model.properties.LumifyProperties;
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
import java.util.Map;

public class ImportMRMapper extends ElementMapper<LongWritable, Text, Text, Mutation> {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(ImportMRMapper.class);
    public static final String CONFIG_SOURCE_FILE_NAME = "sourceFileName";
    private AccumuloGraph graph;
    private Visibility visibility;
    private Authorizations authorizations;
    private SystemUser user;
    private UserRepository userRepository;
    private SecureGraphAuditRepository auditRepository;
    private String sourceFileName;

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
    protected void map(LongWritable filePosition, Text line, Context context) throws IOException, InterruptedException {
        try {
            safeMap(filePosition, line, context);
        } catch (Exception ex) {
            LOGGER.error("failed mapping " + filePosition, ex);
        }
    }

    private void safeMap(LongWritable filePosition, Text line, Context context) throws IOException, InterruptedException {
        String lineString = line.toString();
        int colonOffet = lineString.indexOf(':');
        if (colonOffet < 1) {
            return;
        }
        long userId = Long.parseLong(lineString.substring(0, colonOffet));
        context.setStatus("User: " + userId);

        Vertex userVertex = createUserVertex(userId);

        // audit vertex
        Text key = ImportMR.getKey(Audit.TABLE_NAME, userVertex.getId().toString().getBytes());
        Audit audit = auditRepository.createAudit(AuditAction.CREATE, userVertex.getId(), "Friendster MR", "", user, visibility);
        context.write(key, AccumuloSession.createMutationFromRow(audit));

        String friends = lineString.substring(colonOffet + 1).trim();
        if ("notfound".equals(friends) || "private".equals(friends)) {
            // do nothing?
        } else {
            String[] friendsArray = friends.split(",");
            for (String friend : friendsArray) {
                friend = friend.trim();
                if (friend.length() == 0) {
                    continue;
                }
                long friendId = Long.parseLong(friend);
                Vertex friendVertex = createUserVertex(friendId);
                addEdge(ImportMR.getFriendEdgeId(userVertex, friendVertex),
                        userVertex,
                        friendVertex,
                        FriendsterOntology.EDGE_LABEL_FRIEND,
                        visibility,
                        authorizations);
                context.getCounter(FriendsterImportCounters.FRIEND_EDGES_CREATED).increment(1);
            }
        }

        context.getCounter(FriendsterImportCounters.USERS_PROCESSED).increment(1);
    }

    private Vertex createUserVertex(long userId) {
        String userVertexId = ImportMR.getUserVertexId(userId);
        VertexBuilder userVertexBuilder = prepareVertex(userVertexId, visibility);
        LumifyProperties.CONCEPT_TYPE.setProperty(userVertexBuilder, FriendsterOntology.CONCEPT_TYPE_USER, visibility);
        Metadata titleMetadata = new Metadata();
        LumifyProperties.TITLE.addPropertyValue(userVertexBuilder, ImportMR.MULTI_VALUE_KEY, "Friendster User " + userId, titleMetadata, visibility);
        LumifyProperties.SOURCE.addPropertyValue(userVertexBuilder, ImportMR.MULTI_VALUE_KEY, ImportMR.FRIENDSTER_SOURCE, visibility);
        return userVertexBuilder.save(authorizations);
    }

    @Override
    protected void saveDataMutation(Context context, Text dataTableName, Mutation m) throws IOException, InterruptedException {
        context.write(ImportMR.getKey(dataTableName.toString(), m.getRow()), m);
    }

    @Override
    protected void saveEdgeMutation(Context context, Text edgesTableName, Mutation m) throws IOException, InterruptedException {
        context.write(ImportMR.getKey(edgesTableName.toString(), m.getRow()), m);
    }

    @Override
    protected void saveVertexMutation(Context context, Text verticesTableName, Mutation m) throws IOException, InterruptedException {
        context.write(ImportMR.getKey(verticesTableName.toString(), m.getRow()), m);
    }

    @Override
    public IdGenerator getIdGenerator() {
        return this.graph.getIdGenerator();
    }

    @Inject
    public void setUserRepository(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
}
