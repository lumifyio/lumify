package com.altamiracorp.lumify.core.config;

public enum SandboxLevel {
    /**
     * All actions will be immediately public
     */
    PUBLIC,

    /**
     * All workspace actions will be sandboxed to the workspace and will need to be published
     */
    WORKSPACE
}
