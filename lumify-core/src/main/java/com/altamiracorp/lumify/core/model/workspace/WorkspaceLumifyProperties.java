package com.altamiracorp.lumify.core.model.workspace;

import com.altamiracorp.lumify.core.model.properties.TextLumifyProperty;
import com.altamiracorp.securegraph.TextIndexHint;

public class WorkspaceLumifyProperties {
    public static final TextLumifyProperty TITLE = new TextLumifyProperty("http://lumify.io/workspace/title", TextIndexHint.ALL);
    public static final TextLumifyProperty CREATOR_USER_ID = new TextLumifyProperty("http://lumify.io/workspace/creator", TextIndexHint.EXACT_MATCH);
}
