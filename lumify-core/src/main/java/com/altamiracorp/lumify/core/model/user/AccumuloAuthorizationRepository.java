package com.altamiracorp.lumify.core.model.user;

import com.altamiracorp.lumify.core.model.lock.Lock;
import com.altamiracorp.lumify.core.model.lock.LockRepository;
import com.altamiracorp.lumify.core.util.LumifyLogger;
import com.altamiracorp.lumify.core.util.LumifyLoggerFactory;
import com.altamiracorp.securegraph.Graph;
import com.altamiracorp.securegraph.accumulo.AccumuloAuthorizations;
import com.altamiracorp.securegraph.accumulo.AccumuloGraph;
import com.google.common.collect.Iterables;
import com.google.inject.Inject;
import org.apache.accumulo.core.Constants;
import org.apache.accumulo.core.security.Authorizations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class AccumuloAuthorizationRepository implements AuthorizationRepository {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(AccumuloAuthorizationRepository.class);
    public static final String LOCK_NAME = "AuthorizationRepository";
    private final Graph graph;
    private final LockRepository lockRepository;

    @Inject
    public AccumuloAuthorizationRepository(final Graph graph, final LockRepository lockRepository) {
        this.graph = graph;
        this.lockRepository = lockRepository;
    }

    public void addAuthorizationToGraph(final String auth) {
        LOGGER.info("adding authorization to graph user %s", auth);
        synchronized (graph) {
            Lock lock = this.lockRepository.createLock(LOCK_NAME);
            lock.run(new Runnable() {
                @Override
                public void run() {
                    LOGGER.debug("got lock adding authorization to graph user %s", auth);
                    if (graph instanceof AccumuloGraph) {
                        try {
                            AccumuloGraph accumuloGraph = (AccumuloGraph) graph;
                            String principal = accumuloGraph.getConnector().whoami();
                            Authorizations currentAuthorizations = accumuloGraph.getConnector().securityOperations().getUserAuthorizations(principal);
                            if (currentAuthorizations.contains(auth)) {
                                return;
                            }
                            List<byte[]> newAuthorizationsArray = new ArrayList<byte[]>();
                            for (byte[] currentAuth : currentAuthorizations) {
                                newAuthorizationsArray.add(currentAuth);
                            }
                            newAuthorizationsArray.add(auth.getBytes(Constants.UTF8));
                            Authorizations newAuthorizations = new Authorizations(newAuthorizationsArray);
                            accumuloGraph.getConnector().securityOperations().changeUserAuthorizations(principal, newAuthorizations);
                        } catch (Exception ex) {
                            throw new RuntimeException("Could not update authorizations in accumulo", ex);
                        }
                    } else {
                        throw new RuntimeException("graph type not supported to add authorizations.");
                    }
                }
            });
        }
    }

    public void removeAuthorizationFromGraph(final String auth) {
        LOGGER.info("removing authorization to graph user %s", auth);
        synchronized (graph) {
            Lock lock = this.lockRepository.createLock(LOCK_NAME);
            lock.run(new Runnable() {
                @Override
                public void run() {
                    LOGGER.debug("got lock removing authorization to graph user %s", auth);
                    if (graph instanceof AccumuloGraph) {
                        try {
                            AccumuloGraph accumuloGraph = (AccumuloGraph) graph;
                            String principal = accumuloGraph.getConnector().whoami();
                            Authorizations currentAuthorizations = accumuloGraph.getConnector().securityOperations().getUserAuthorizations(principal);
                            if (!currentAuthorizations.contains(auth)) {
                                return;
                            }
                            byte[] authBytes = auth.getBytes(Constants.UTF8);
                            List<byte[]> newAuthorizationsArray = new ArrayList<byte[]>();
                            for (byte[] currentAuth : currentAuthorizations) {
                                if (Arrays.equals(currentAuth, authBytes)) {
                                    continue;
                                }
                                newAuthorizationsArray.add(currentAuth);
                            }
                            Authorizations newAuthorizations = new Authorizations(newAuthorizationsArray);
                            accumuloGraph.getConnector().securityOperations().changeUserAuthorizations(principal, newAuthorizations);
                        } catch (Exception ex) {
                            throw new RuntimeException("Could not update authorizations in accumulo", ex);
                        }
                    } else {
                        throw new RuntimeException("graph type not supported to add authorizations.");
                    }
                }
            });
        }
    }

    public com.altamiracorp.securegraph.Authorizations createAuthorizations(Set<String> authorizationsSet) {
        return new AccumuloAuthorizations(Iterables.toArray(authorizationsSet, String.class));
    }
}
