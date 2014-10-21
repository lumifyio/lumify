package io.lumify.core.model.longRunningProcess;

import com.google.inject.Injector;
import io.lumify.core.user.User;

import java.util.Map;

public class LongRunningWorkerPrepareData {
    private final Map config;
    private final User user;
    private final Injector injector;

    public LongRunningWorkerPrepareData(Map config, User user, Injector injector) {
        this.config = config;
        this.user = user;
        this.injector = injector;
    }

    public Map getConfig() {
        return config;
    }

    public User getUser() {
        return user;
    }

    public Injector getInjector() {
        return injector;
    }
}
