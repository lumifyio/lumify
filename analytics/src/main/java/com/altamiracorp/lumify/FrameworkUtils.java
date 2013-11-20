package com.altamiracorp.lumify;

import static com.google.common.base.Preconditions.checkNotNull;

import com.altamiracorp.bigtable.model.ModelSession;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.core.util.ModelUtil;
import com.altamiracorp.lumify.ontology.BaseOntology;
import com.altamiracorp.lumify.core.model.search.SearchProvider;
import com.google.inject.Injector;

public class FrameworkUtils {

    public static void initializeFramework(final Injector injector, final User user) {
        checkNotNull(injector);
        checkNotNull(user);

        final ModelSession modelSession = injector.getInstance(ModelSession.class);
        final SearchProvider searchProvider = injector.getInstance(SearchProvider.class);
        final BaseOntology baseOntology = injector.getInstance(BaseOntology.class);

        ModelUtil.initializeTables(modelSession, user);
        searchProvider.initializeIndex(user);
        baseOntology.initialize(user);
    }
}
