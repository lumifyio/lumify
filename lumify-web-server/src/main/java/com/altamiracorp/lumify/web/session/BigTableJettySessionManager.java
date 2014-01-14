package com.altamiracorp.lumify.web.session;

import com.altamiracorp.bigtable.model.Column;
import com.altamiracorp.lumify.core.user.SystemUser;
import com.altamiracorp.lumify.web.session.model.*;
import com.google.inject.Inject;
import org.eclipse.jetty.nosql.NoSqlSession;

import java.util.Set;

public class BigTableJettySessionManager extends org.eclipse.jetty.nosql.NoSqlSessionManager {
    final JettySessionRepository jettySessionRepository;

    @Inject
    public BigTableJettySessionManager(final JettySessionRepository jettySessionRepository) {
        this.jettySessionRepository = jettySessionRepository;
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

            JettySessionData data = row.get(JettySessionData.COLUMN_FAMILY_NAME);
            Set<String> attributesToSave = session.takeDirty();
            if (isNew || isSaveAllAttributes()) {
                attributesToSave.addAll(session.getNames());
            }
            for (String name : attributesToSave) {
                data.setObject(name, session.getAttribute(name));
            }

            jettySessionRepository.save(row, SystemUser.getSystemUserContext());
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
                long savedVersion = row.getMetadata().getVersion();
                if (savedVersion == ((Number) version).longValue()) {
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
