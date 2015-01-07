package io.lumify.core.security;

import io.lumify.web.clientapi.model.VisibilityJson;
import org.securegraph.Visibility;

import java.util.Map;

public abstract class VisibilityTranslator {
    public static final String JSON_SOURCE = "source";
    public static final String JSON_WORKSPACES = "workspaces";

    public abstract void init(Map configuration);

    public abstract LumifyVisibility toVisibility(VisibilityJson visibilityJson);

    public abstract Visibility toVisibilityNoSuperUser(VisibilityJson visibilityJson);

    public abstract Visibility getDefaultVisibility();
}
