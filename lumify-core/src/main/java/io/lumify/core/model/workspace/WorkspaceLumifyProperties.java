package io.lumify.core.model.workspace;

import io.lumify.core.model.properties.types.BooleanLumifyProperty;
import io.lumify.core.model.properties.types.IntegerLumifyProperty;
import io.lumify.core.model.properties.types.TextLumifyProperty;
import org.securegraph.TextIndexHint;

public class WorkspaceLumifyProperties {
    public static final TextLumifyProperty TITLE = new TextLumifyProperty(WorkspaceRepository.WORKSPACE_CONCEPT_NAME + "/title", TextIndexHint.ALL);
    public static final BooleanLumifyProperty WORKSPACE_TO_USER_IS_CREATOR = new BooleanLumifyProperty(WorkspaceRepository.WORKSPACE_TO_USER_RELATIONSHIP_NAME + "/creator");
    public static final TextLumifyProperty WORKSPACE_TO_USER_ACCESS = new TextLumifyProperty(WorkspaceRepository.WORKSPACE_TO_USER_RELATIONSHIP_NAME + "/access", TextIndexHint.NONE);
    public static final IntegerLumifyProperty WORKSPACE_TO_ENTITY_GRAPH_POSITION_X = new IntegerLumifyProperty(WorkspaceRepository.WORKSPACE_TO_ENTITY_RELATIONSHIP_NAME + "/graphPositionX");
    public static final IntegerLumifyProperty WORKSPACE_TO_ENTITY_GRAPH_POSITION_Y = new IntegerLumifyProperty(WorkspaceRepository.WORKSPACE_TO_ENTITY_RELATIONSHIP_NAME + "/graphPositionY");
    public static final BooleanLumifyProperty WORKSPACE_TO_ENTITY_VISIBLE = new BooleanLumifyProperty(WorkspaceRepository.WORKSPACE_TO_ENTITY_RELATIONSHIP_NAME + "/visible");
}
