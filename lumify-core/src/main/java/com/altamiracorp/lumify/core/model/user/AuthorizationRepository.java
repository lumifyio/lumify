package com.altamiracorp.lumify.core.model.user;

import com.altamiracorp.securegraph.Graph;
import com.altamiracorp.securegraph.accumulo.AccumuloGraph;
import com.google.inject.Inject;
import org.apache.accumulo.core.Constants;
import org.apache.accumulo.core.security.Authorizations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AuthorizationRepository {
    private final Graph graph;

    @Inject
    public AuthorizationRepository(final Graph graph) {
        this.graph = graph;
    }

    public void addAuthorizationToGraph(String auth) {
        // TODO this code is not safe across a cluster since it is not atomic. One possibility is to create a table of authorizations and always read/write from that table.
        synchronized (graph) {
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
    }

    public void removeAuthorizationFromGraph(String auth) {
        // TODO this code is not safe across a cluster since it is not atomic. One possibility is to create a table of authorizations and always read/write from that table.
        synchronized (graph) {
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
    }
}
