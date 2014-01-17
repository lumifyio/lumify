package com.altamiracorp.lumify.web.session;

import com.altamiracorp.bigtable.model.Column;
import com.altamiracorp.bigtable.model.FlushFlag;
import com.altamiracorp.bigtable.model.ModelSession;
import com.altamiracorp.lumify.core.FrameworkUtils;
import com.altamiracorp.lumify.core.bootstrap.InjectHelper;
import com.altamiracorp.lumify.core.bootstrap.LumifyBootstrap;
import com.altamiracorp.lumify.core.config.Configuration;
import com.altamiracorp.lumify.core.user.SystemUser;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.web.session.model.*;
import com.google.inject.Inject;
import org.eclipse.jetty.nosql.NoSqlSession;
import org.eclipse.jetty.nosql.NoSqlSessionManager;

import java.util.Set;

public class BigTableJettySessionManager extends NoSqlSessionManager {
    private JettySessionRepository jettySessionRepository;
    private String configLocation = "/opt/lumify/config/";

    public BigTableJettySessionManager() {
        InjectHelper.inject(this, LumifyBootstrap.bootstrapModuleMaker(Configuration.loadConfigurationFile(configLocation)));
    }

    @Inject
    public void setJettySessionRepository(JettySessionRepository jettySessionRepository) {
        this.jettySessionRepository = jettySessionRepository;
    }

    public JettySessionRepository getJettySessionRepository() {
        return jettySessionRepository;
    }

    @Override
    protected NoSqlSession loadSession(String clusterId) {
        JettySessionRow row = jettySessionRepository.findByRowKey(clusterId, SystemUser.getSystemUserContext());
        if (row == null) {
            return null;
        }

        JettySessionMetadata metadata = row.getMetadata();
        NoSqlSession session = new NoSqlSession(this, metadata.getCreated(), metadata.getAccessed(), metadata.getClusterId(), metadata.getVersion());
        setData(session, row.getData());
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

            if (version == null) {
                // new session
                isNew = true;
                row = new JettySessionRow(new JettySessionRowKey(session.getClusterId()));
                metadata = row.getMetadata();
                metadata.setCreated(session.getCreationTime());
                metadata.setClusterId(session.getClusterId());
                version = 0;
            } else {
                // existing session
                row = jettySessionRepository.findByRowKey(session.getClusterId(), SystemUser.getSystemUserContext());
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

            jettySessionRepository.save(row, FlushFlag.FLUSH, SystemUser.getSystemUserContext());
        } else {
            // invalid session
            jettySessionRepository.delete(new JettySessionRowKey(session.getClusterId()), SystemUser.getSystemUserContext());
        }

        if (activateAfterSave) {
            session.didActivate();
        }

        return version;
    }

    @Override
    protected Object refresh(NoSqlSession session, Object version) {
        JettySessionRow row = jettySessionRepository.findByRowKey(session.getClusterId(), SystemUser.getSystemUserContext());

        if (version != null) {
            if (row != null) {
                Long savedVersion = row.getMetadata().getVersion();
                if (savedVersion != null && savedVersion == ((Number) version).longValue()) {
                    // refresh not required
                    return version;
                }
            }
        }

        if (row == null) {
            session.invalidate();
            return null;
        }

        session.willPassivate();
        session.clearAttributes();
        setData(session, row.getData());

        row.getMetadata().setAccessed(System.currentTimeMillis());
        jettySessionRepository.save(row, SystemUser.getSystemUserContext());

        session.didActivate();

        return version;
    }

    @Override
    protected boolean remove(NoSqlSession session) {
        JettySessionRow row = jettySessionRepository.findByRowKey(session.getClusterId(), SystemUser.getSystemUserContext());

        if (row != null) {
            jettySessionRepository.delete(row.getRowKey(), SystemUser.getSystemUserContext());
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
