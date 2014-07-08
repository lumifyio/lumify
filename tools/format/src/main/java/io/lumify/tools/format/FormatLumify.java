package io.lumify.tools.format;

import com.altamiracorp.bigtable.model.ModelSession;
import io.lumify.core.cmdline.CommandLineBase;
import io.lumify.core.model.user.AuthorizationRepository;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import io.lumify.core.util.ModelUtil;
import com.google.inject.Inject;
import org.apache.commons.cli.CommandLine;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.transport.InetSocketTransportAddress;

public class FormatLumify extends CommandLineBase {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(FormatLumify.class);
    private ModelSession modelSession;
    private AuthorizationRepository authorizationRepository;

    public static void main(String[] args) throws Exception {
        int res = new FormatLumify().run(args);
        if (res != 0) {
            System.exit(res);
        }
    }

    @Override
    protected int run(CommandLine cmd) throws Exception {
        ModelUtil.deleteTables(modelSession, getUser());
        getWorkQueueRepository().format();
        // TODO provide a way to delete the graph and it's search index
        // graph.delete(getUser());

        LOGGER.debug("BEGIN remove all authorizations");
        for (String auth : authorizationRepository.getGraphAuthorizations()) {
            LOGGER.debug("removing auth %s", auth);
            authorizationRepository.removeAuthorizationFromGraph(auth);
        }
        LOGGER.debug("END remove all authorizations");

        getGraph().shutdown();

        // TODO refactor to config file info. But since this is only for development this is low priority
        String ES_INDEX = "securegraph";
        LOGGER.debug("BEGIN deleting elastic search index: " + ES_INDEX);
        TransportClient client = new TransportClient();
        for (String esLocation : new String[]{"192.168.33.10:9300"}) {
            String[] locationSocket = esLocation.split(":");
            client.addTransportAddress(new InetSocketTransportAddress(locationSocket[0], Integer.parseInt(locationSocket[1])));
        }
        DeleteIndexResponse response = client.admin().indices().delete(new DeleteIndexRequest(ES_INDEX)).actionGet();
        if (!response.isAcknowledged()) {
            LOGGER.error("Failed to delete elastic search index named %s", ES_INDEX);
        }
        LOGGER.debug("END deleting elastic search index: " + ES_INDEX);
        client.close();

        return 0;
    }

    @Inject
    public void setModelSession(ModelSession modelSession) {
        this.modelSession = modelSession;
    }

    @Inject
    public void setAuthorizationRepository(AuthorizationRepository authorizationRepository) {
        this.authorizationRepository = authorizationRepository;
    }
}
