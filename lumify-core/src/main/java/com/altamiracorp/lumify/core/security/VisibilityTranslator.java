package com.altamiracorp.lumify.core.security;

import java.util.Map;

public interface VisibilityTranslator {
    void init(Map configuration);

    LumifyVisibility toVisibility(String source, String... additionalRequiredVisibilities);

    LumifyVisibility toVisibilityWithWorkspace(String source, String workspaceId);
}
