package io.lumify.core.user;

import com.altamiracorp.bigtable.model.user.ModelUserContext;
import io.lumify.core.model.user.UserType;

import java.io.Serializable;
import java.util.Set;

public interface User extends Serializable {
    public static final long serialVersionUID = 1L;

    public String getUserId();

    public ModelUserContext getModelUserContext();

    public String getDisplayName();

    public UserType getUserType();

    public String getUserStatus();
}
