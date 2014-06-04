package io.lumify.web.routes.artifact;

import com.altamiracorp.bigtable.model.user.ModelUserContext;
import com.altamiracorp.miniweb.HandlerChain;
import com.google.inject.Inject;
import io.lumify.core.EntityHighlighter;
import io.lumify.core.config.Configuration;
import io.lumify.core.model.properties.RawLumifyProperties;
import io.lumify.core.model.termMention.TermMentionModel;
import io.lumify.core.model.termMention.TermMentionRepository;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.model.workspace.WorkspaceRepository;
import io.lumify.core.user.User;
import io.lumify.web.BaseRequestHandler;
import org.apache.commons.io.IOUtils;
import org.securegraph.Authorizations;
import org.securegraph.Graph;
import org.securegraph.Vertex;
import org.securegraph.property.StreamingPropertyValue;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ArtifactHighlightedText extends BaseRequestHandler {
    private final Graph graph;
    private final TermMentionRepository termMentionRepository;
    private final EntityHighlighter entityHighlighter;
    private final UserRepository userRepository;

    @Inject
    public ArtifactHighlightedText(
            final Graph graph,
            final UserRepository userRepository,
            final TermMentionRepository termMentionRepository,
            final EntityHighlighter entityHighlighter,
            final WorkspaceRepository workspaceRepository,
            final Configuration configuration) {
        super(userRepository, workspaceRepository, configuration);
        this.graph = graph;
        this.termMentionRepository = termMentionRepository;
        this.entityHighlighter = entityHighlighter;
        this.userRepository = userRepository;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        User user = getUser(request);
        Authorizations authorizations = getAuthorizations(request, user);
        String workspaceId = getActiveWorkspaceId(request);
        ModelUserContext modelUserContext = userRepository.getModelUserContext(authorizations, workspaceId);

        String graphVertexId = getRequiredParameter(request, "graphVertexId");
        String propertyKey = getRequiredParameter(request, "propertyKey");

        Vertex artifactVertex = graph.getVertex(graphVertexId, authorizations);
        if (artifactVertex == null) {
            respondWithNotFound(response);
            return;
        }

        StreamingPropertyValue textPropertyValue = RawLumifyProperties.TEXT.getPropertyValue(artifactVertex, propertyKey);
        if (textPropertyValue == null) {
            respondWithNotFound(response);
            return;
        }

        String highlightedText;
        String text = IOUtils.toString(textPropertyValue.getInputStream(), "UTF-8");
        if (text == null) {
            highlightedText = "";
        } else {
            Iterable<TermMentionModel> termMentions = termMentionRepository.findByGraphVertexId(artifactVertex.getId().toString(), modelUserContext);
            highlightedText = entityHighlighter.getHighlightedText(text, termMentions);
        }

        response.setContentType("text/html");
        response.setCharacterEncoding("UTF-8");

        IOUtils.write(highlightedText, response.getOutputStream(), "UTF-8");
    }
}
