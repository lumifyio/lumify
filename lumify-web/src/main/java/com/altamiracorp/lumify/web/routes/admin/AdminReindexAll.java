package com.altamiracorp.lumify.web.routes.admin;

import com.altamiracorp.lumify.core.model.graph.GraphRepository;
import com.altamiracorp.lumify.core.model.graph.GraphVertex;
import com.altamiracorp.lumify.core.model.workQueue.WorkQueueRepository;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.web.BaseRequestHandler;
import com.altamiracorp.miniweb.HandlerChain;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

public class AdminReindexAll extends BaseRequestHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(AdminReindexAll.class);
    private int pageSize = 100;
    private WorkQueueRepository workQueueRepository;
    private GraphRepository graphRepository;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        LOGGER.info("Adding all nodes to kafka");

        User user = getUser(request);
        ServletOutputStream respOut = response.getOutputStream();

        for (int offset = 0; ; offset += pageSize) {
            LOGGER.info("Adding vertices from offset " + offset);
            respOut.println("Adding vertices from offset " + offset);
            respOut.flush();
            List<GraphVertex> graphVertices = graphRepository.searchAllVertices(offset, pageSize, user);
            if (graphVertices.size() == 0) {
                break;
            }
            for (GraphVertex graphVertex : graphVertices) {
                workQueueRepository.pushSearchIndex(graphVertex.getId());
            }
        }

        respOut.println("DONE");
        respOut.flush();
    }

    @Inject
    public void setWorkQueueRepository(WorkQueueRepository workQueueRepository) {
        this.workQueueRepository = workQueueRepository;
    }

    @Inject
    public void setGraphRepository(GraphRepository graphRepository) {
        this.graphRepository = graphRepository;
    }
}
