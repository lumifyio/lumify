package io.lumify.core.model.user;

import io.lumify.core.user.User;

public interface UserListener {
    void newUserAdded(User user);
}
