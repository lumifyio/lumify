package io.lumify.web.clientapi.codegen;

public class EntityApiExt extends EntityApi {
    public static final int VIDEO_TRANSCRIPT_INDEX_BITS = 12; // duplicated in io.lumify.core.model.textHighlighting.OffsetItem
    public static final int VIDEO_TRANSCRIPT_OFFSET_BITS = 20; // duplicated in io.lumify.core.model.textHighlighting.OffsetItem

    public void resolveTerm(String artifactId, String propertyKey, int mentionStart, int mentionEnd, String sign, String conceptId, String visibilitySource) throws ApiException {
        resolveTerm(artifactId, propertyKey, mentionStart, mentionEnd, sign, conceptId, visibilitySource, null, null, null);
    }

    public void resolveVideoTranscriptTerm(String artifactId, String propertyKey, int videoFrameIndex, int mentionStart, int mentionEnd, String sign, String conceptId, String visibilitySource) throws ApiException {
        int mentionStartWithVideoFrame = (videoFrameIndex << VIDEO_TRANSCRIPT_OFFSET_BITS) | mentionStart;
        int mentionEndWithVideoFrame = (videoFrameIndex << VIDEO_TRANSCRIPT_OFFSET_BITS) | mentionEnd;
        resolveTerm(artifactId, propertyKey, mentionStartWithVideoFrame, mentionEndWithVideoFrame, sign, conceptId, visibilitySource, null, null, null);
    }

    public void unresolveTerm(String vertexId, int startOffset, int endOffset, String sign, String conceptIri, String edgeId) throws ApiException {
        unresolveTerm(vertexId, startOffset, endOffset, sign, conceptIri, edgeId, null);
    }
}
