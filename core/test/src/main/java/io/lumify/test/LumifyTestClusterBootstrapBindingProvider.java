package io.lumify.test;

import com.google.inject.Binder;
import io.lumify.core.bootstrap.BootstrapBindingProvider;
import io.lumify.core.config.Configuration;
import io.lumify.core.model.lock.LocalLockRepository;
import io.lumify.core.model.lock.LockRepository;

public class LumifyTestClusterBootstrapBindingProvider implements BootstrapBindingProvider {
    @Override
    public void addBindings(Binder binder, Configuration configuration) {
        binder.bind(LockRepository.class).to(LocalLockRepository.class);
    }
}
