package io.lumify.core.model.user;

import io.lumify.core.bootstrap.InjectHelper;
import io.lumify.core.user.User;

import java.util.List;
import java.util.ServiceLoader;

import static org.securegraph.util.IterableUtils.toList;

public class UserListenerUtil {
    private List<UserListener> userListeners;

    public void fireNewUserAddedEvent(User user) {
        for (UserListener userListener : getUserListeners()) {
            userListener.newUserAdded(user);
        }
    }

    public List<UserListener> getUserListeners() {
        if (userListeners == null) {
            userListeners = toList(ServiceLoader.load(UserListener.class));
            for (UserListener userListener : userListeners) {
                InjectHelper.inject(userListener);
            }
        }
        return userListeners;
    }
}
