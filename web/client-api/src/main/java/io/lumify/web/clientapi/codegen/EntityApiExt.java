package io.lumify.web.clientapi.codegen;

public class EntityApiExt extends EntityApi {
    public void resolveTerm(String artifactId, String propertyKey, Integer mentionStart, Integer mentionEnd, String sign, String conceptId, String visibilitySource) throws ApiException {
        resolveTerm(artifactId, propertyKey, mentionStart, mentionEnd, sign, conceptId, visibilitySource, null, null, null);
    }
}
