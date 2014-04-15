package com.altamiracorp.lumify.core;

import com.altamiracorp.bigtable.model.ModelSession;
import com.altamiracorp.lumify.core.model.ontology.OntologyRepository;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.core.util.ModelUtil;
import com.google.inject.Injector;

import static com.google.common.base.Preconditions.checkNotNull;

public class FrameworkUtils {

    public static void initializeFramework(final Injector injector, final User user) {
        checkNotNull(injector);
        checkNotNull(user);

        final ModelSession modelSession = injector.getInstance(ModelSession.class);
        injector.getInstance(OntologyRepository.class);

        ModelUtil.initializeTables(modelSession, user);
    }
}
