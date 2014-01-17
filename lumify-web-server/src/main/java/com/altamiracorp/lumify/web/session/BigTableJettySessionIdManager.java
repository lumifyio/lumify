package com.altamiracorp.lumify.web.session;

import com.altamiracorp.lumify.core.user.SystemUser;
import com.altamiracorp.lumify.web.session.model.JettySessionRow;
import com.google.inject.Inject;
import org.eclipse.jetty.nosql.NoSqlSession;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.SessionManager;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.session.AbstractSessionIdManager;
import org.eclipse.jetty.server.session.SessionHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

public class BigTableJettySessionIdManager extends AbstractSessionIdManager {

    private BigTableJettySessionManager sessionManager;
    private Server server;

    BigTableJettySessionIdManager() { }

    public BigTableJettySessionIdManager(Server server, BigTableJettySessionManager sessionManager) {
        this.server = server;
        this.sessionManager = sessionManager;
    }

    @Override
    public boolean idInUse(String id) {
        JettySessionRow row = sessionManager.getJettySessionRepository().findByRowKey(id, SystemUser.getSystemUserContext());
        return row != null;
    }

    @Override
    public void addSession(HttpSession session) {
        if (session != null) {
            // TODO: will someone else have called BigTableJettySessionManager.save()?
        }
    }

    @Override
    public void removeSession(HttpSession session) {
        if (session != null) {
            // TODO: will someone else have called BigTableJettySessionManager.remove()?
        }
    }

    @Override
    public void invalidateAll(String id) {
        // TODO: our job? sessionManager.jettySessionRepository.delete(new JettySessionRowKey(id), SystemUser.getSystemUserContext());

        Handler[] contexts = server.getChildHandlersByClass(ContextHandler.class);
        for (int i=0; contexts!=null && i<contexts.length; i++) {
            SessionHandler sessionHandler = ((ContextHandler)contexts[i]).getChildHandlerByClass(SessionHandler.class);
            if (sessionHandler != null) {
                SessionManager manager = sessionHandler.getSessionManager();
                if (manager != null && manager instanceof BigTableJettySessionManager) {
                    // TODO: ((BigTableJettySessionManager)manager).remove(id);
                }
            }
        }
    }

    @Override
    public String getClusterId(String nodeId) {
        int dot = nodeId.lastIndexOf('.');
        return dot > 0 ? nodeId.substring(0, dot) : nodeId;
    }

    @Override
    public String getNodeId(String clusterId, HttpServletRequest request) {
        if (getWorkerName() != null) {
            return clusterId + '.' + getWorkerName();
        } else {
            return clusterId;
        }
    }
}
