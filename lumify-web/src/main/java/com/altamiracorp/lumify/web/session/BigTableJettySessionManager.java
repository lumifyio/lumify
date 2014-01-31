package com.altamiracorp.lumify.web.session;

import com.altamiracorp.bigtable.model.Column;
import com.altamiracorp.bigtable.model.FlushFlag;
import com.altamiracorp.lumify.core.bootstrap.InjectHelper;
import com.altamiracorp.lumify.core.bootstrap.LumifyBootstrap;
import com.altamiracorp.lumify.core.config.Configuration;
import com.altamiracorp.lumify.core.user.UserProvider;
import com.altamiracorp.lumify.web.session.model.*;
import com.google.common.base.Optional;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.inject.Inject;
import org.eclipse.jetty.nosql.NoSqlSession;
import org.eclipse.jetty.nosql.NoSqlSessionManager;

import java.util.Set;
import java.util.concurrent.TimeUnit;

public class BigTableJettySessionManager extends NoSqlSessionManager {
    private static final int CACHE_MAX_SIZE = 50;
    private static final int CACHE_EXPIRE_MINUTES = 10;
    private static final String CONFIGURATION_LOCATION = "/opt/lumify/config/";

    private JettySessionRepository jettySessionRepository;
    private UserProvider userProvider;
    private LoadingCache<String, Optional<JettySessionRow>> cache;

    public BigTableJettySessionManager() {
        InjectHelper.inject(this, LumifyBootstrap.bootstrapModuleMaker(Configuration.loadConfigurationFile(CONFIGURATION_LOCATION)));

        cache = CacheBuilder.newBuilder()
                .maximumSize(CACHE_MAX_SIZE)
                .expireAfterWrite(CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES)
                .build(new CacheLoader<String, Optional<JettySessionRow>>() {
                    @Override
                    public Optional<JettySessionRow> load(String clusterId) throws Exception {
                        return Optional.fromNullable(jettySessionRepository.findByRowKey(clusterId, userProvider.getSystemUser().getModelUserContext()));
                    }
                });
    }

    @Inject
    public void setJettySessionRepository(JettySessionRepository jettySessionRepository) {
        this.jettySessionRepository = jettySessionRepository;
    }

    @Inject
    public void setUserProvider(UserProvider userProvider) {
        this.userProvider = userProvider;
    }

    @Override
    protected NoSqlSession loadSession(String clusterId) {
        Optional<JettySessionRow> row = cache.getUnchecked(clusterId);
        if (!row.isPresent()) {
            return null;
        }

        JettySessionMetadata metadata = row.get().getMetadata();
        NoSqlSession session = new NoSqlSession(this, metadata.getCreated(), metadata.getAccessed(), metadata.getClusterId(), metadata.getVersion());
        setData(session, row.get().getData());
        session.didActivate();

        return session;
    }

    @Override
    protected Object save(NoSqlSession session, Object version, boolean activateAfterSave) {
        session.willPassivate();

        if (session.isValid()) {
            boolean isNew = false;
            JettySessionRow row;
            JettySessionMetadata metadata;

            Optional<JettySessionRow> optionalRow = cache.getUnchecked(session.getClusterId());

            if (!optionalRow.isPresent()) {
                // new session
                isNew = true;
                row = new JettySessionRow(new JettySessionRowKey(session.getClusterId()));
                cache.put(session.getClusterId(), Optional.of(row));
                metadata = row.getMetadata();
                metadata.setCreated(session.getCreationTime());
                metadata.setClusterId(session.getClusterId());
                version = 0;
            } else {
                // existing session
                row = optionalRow.get();
                metadata = row.getMetadata();
                version = ((Number) version).longValue() + 1;
            }
            metadata.setVersion(((Number) version).longValue());
            metadata.setAccessed(session.getAccessed());

            JettySessionData data = row.getData();
            Set<String> attributesToSave = session.takeDirty();
            if (isNew || isSaveAllAttributes()) {
                attributesToSave.addAll(session.getNames());
            }
            for (String name : attributesToSave) {
                data.setObject(name, session.getAttribute(name));
            }

            jettySessionRepository.save(row, FlushFlag.FLUSH, userProvider.getSystemUser().getModelUserContext());
        } else {
            // invalid session
            jettySessionRepository.delete(new JettySessionRowKey(session.getClusterId()), userProvider.getSystemUser().getModelUserContext());
            cache.invalidate(session.getClusterId());
        }

        if (activateAfterSave) {
            session.didActivate();
        }

        return version;
    }

    @Override
    protected Object refresh(NoSqlSession session, Object version) {
        Optional<JettySessionRow> optRow = cache.getUnchecked(session.getClusterId());

        if (version != null) {
            if (optRow.isPresent()) {
                Long savedVersion = optRow.get().getMetadata().getVersion();
                if (savedVersion != null && savedVersion == ((Number) version).longValue()) {
                    // refresh not required
                    return version;
                }
            }
        }


        if (!optRow.isPresent()) {
            session.invalidate();
            return null;
        }

        JettySessionRow row = optRow.get();
        session.willPassivate();
        session.clearAttributes();
        setData(session, row.getData());

        row.getMetadata().setAccessed(System.currentTimeMillis());
        jettySessionRepository.save(row, userProvider.getSystemUser().getModelUserContext());

        session.didActivate();

        return version;
    }

    @Override
    protected boolean remove(NoSqlSession session) {
        Optional<JettySessionRow> optRow = cache.getUnchecked(session.getClusterId());

        if (optRow.isPresent()) {
            jettySessionRepository.delete(optRow.get().getRowKey(), userProvider.getSystemUser().getModelUserContext());
            cache.invalidate(session.getClusterId());
            return true;
        } else {
            return false;
        }
    }

    private void setData(NoSqlSession session, JettySessionData data) {
        for (Column col : data.getColumns()) {
            String name = col.getName();
            Object value = data.getObject(name);

            session.doPutOrRemove(name, value);
            session.bindValue(name, value);
        }
    }
}
