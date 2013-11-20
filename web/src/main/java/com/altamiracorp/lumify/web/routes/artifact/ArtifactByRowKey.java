package com.altamiracorp.lumify.web.routes.artifact;

import com.altamiracorp.lumify.core.model.graph.GraphVertex;
import com.altamiracorp.lumify.core.model.ontology.PropertyName;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.core.model.graph.GraphRepository;
import com.altamiracorp.lumify.core.model.artifact.Artifact;
import com.altamiracorp.lumify.core.model.artifact.ArtifactRepository;
import com.altamiracorp.lumify.core.model.artifact.ArtifactRowKey;
import com.altamiracorp.lumify.core.model.artifact.ArtifactType;
import com.altamiracorp.lumify.web.BaseRequestHandler;
import com.altamiracorp.miniweb.HandlerChain;
import com.altamiracorp.miniweb.utils.UrlUtils;
import com.google.inject.Inject;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ArtifactByRowKey extends BaseRequestHandler {
    private final ArtifactRepository artifactRepository;
    private final GraphRepository graphRepository;

    @Inject
    public ArtifactByRowKey(final ArtifactRepository artifactRepository, GraphRepository graphRepository) {
        this.artifactRepository = artifactRepository;
        this.graphRepository = graphRepository;
    }

    public static String getUrl(HttpServletRequest request, String artifactKey) {
        return UrlUtils.getRootRef(request) + "/artifact/" + UrlUtils.urlEncode(artifactKey);
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        User user = getUser(request);
        ArtifactRowKey artifactKey = new ArtifactRowKey(UrlUtils.urlDecode(getAttributeString(request, "_rowKey")));
        GraphVertex artifactVertex = this.graphRepository.findVertexByRowKey(artifactKey.toString(), user);
        Artifact artifact = artifactRepository.findByRowKey(artifactKey.toString(), user.getModelUserContext());

        if (artifactVertex == null || artifact == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        } else {
            JSONObject artifactJson = artifact.toJson();
            artifactJson.put("rawUrl", ArtifactRawByRowKey.getUrl(artifact.getRowKey()));
            artifactJson.put("thumbnailUrl", ArtifactThumbnailByRowKey.getUrl(artifact.getRowKey()));
            artifactJson.put("source", artifactVertex.getProperty(PropertyName.SOURCE));

            ArtifactType artifactType = ArtifactType.convert((String) artifactVertex.getProperty(PropertyName.SUBTYPE));
            if (artifactType == ArtifactType.VIDEO) {
                artifactJson.put("posterFrameUrl", ArtifactPosterFrameByRowKey.getUrl(request, artifact.getRowKey()));
                artifactJson.put("videoPreviewImageUrl", ArtifactVideoPreviewImageByRowKey.getUrl(request, artifact.getRowKey()));
            }
            respondWithJson(response, artifactJson);
        }

        chain.next(request, response);
    }
}
