package com.altamiracorp.lumify.core.model.workspace;

import com.altamiracorp.lumify.core.model.properties.BooleanLumifyProperty;
import com.altamiracorp.lumify.core.model.properties.TextLumifyProperty;
import com.altamiracorp.securegraph.TextIndexHint;

public class WorkspaceLumifyProperties {
    public static final TextLumifyProperty TITLE = new TextLumifyProperty("http://lumify.io/workspace/title", TextIndexHint.ALL);
    public static final BooleanLumifyProperty WORKSPACE_TO_USER = new BooleanLumifyProperty("http://lumify.io/workspace/toUser");
}
