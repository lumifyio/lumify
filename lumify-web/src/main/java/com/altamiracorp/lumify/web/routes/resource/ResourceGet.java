package com.altamiracorp.lumify.web.routes.resource;

import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.core.model.resources.Resource;
import com.altamiracorp.lumify.core.model.resources.ResourceRepository;
import com.altamiracorp.lumify.web.BaseRequestHandler;
import com.altamiracorp.miniweb.HandlerChain;
import com.google.inject.Inject;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ResourceGet extends BaseRequestHandler {
    private ResourceRepository resourceRepository;

    @Inject
    public ResourceGet(ResourceRepository resourceRepository) {
        this.resourceRepository = resourceRepository;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        final String rowKey = getAttributeString(request, "_rowKey");
        User user = getUser(request);

        Resource resource = resourceRepository.findByRowKey(rowKey, user.getModelUserContext());
        if (resource == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        response.setContentType(resource.getContent().getContentType());
        ServletOutputStream out = response.getOutputStream();
        out.write(resource.getContent().getData());
        out.close();
    }
}
