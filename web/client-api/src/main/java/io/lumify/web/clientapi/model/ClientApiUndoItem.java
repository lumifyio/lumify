package io.lumify.web.clientapi.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.lumify.web.clientapi.model.util.ClientApiConverter;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = ClientApiVertexUndoItem.class, name = "vertex"),
        @JsonSubTypes.Type(value = ClientApiPropertyUndoItem.class, name = "property"),
        @JsonSubTypes.Type(value = ClientApiRelationshipUndoItem.class, name = "relationship")
})
public abstract class ClientApiUndoItem implements ClientApiObject {
    private Action action;
    private String errorMessage;

    public Action getAction() {
        return action;
    }

    public void setAction(Action action) {
        this.action = action;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public abstract String getType();

    @Override
    public String toString() {
        return ClientApiConverter.clientApiToString(this);
    }

    public static enum Action {
        delete, addOrUpdate;

        @JsonCreator
        public static Action create(String value) {
            if (value == null) {
                return addOrUpdate;
            }
            if (value.equalsIgnoreCase("delete")) {
                return delete;
            }
            return addOrUpdate;
        }
    }
}
