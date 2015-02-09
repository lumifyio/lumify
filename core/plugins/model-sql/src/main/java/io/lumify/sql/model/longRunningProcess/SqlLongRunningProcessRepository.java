package io.lumify.sql.model.longRunningProcess;

import com.google.inject.Inject;
import io.lumify.core.exception.LumifyException;
import io.lumify.core.model.longRunningProcess.LongRunningProcessRepository;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.model.workQueue.WorkQueueRepository;
import io.lumify.core.user.ProxyUser;
import io.lumify.core.user.User;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import io.lumify.sql.model.HibernateSessionManager;
import io.lumify.sql.model.user.SqlUser;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;
import org.json.JSONObject;
import org.securegraph.Authorizations;
import org.securegraph.Graph;
import org.securegraph.util.ConvertingIterable;

import java.util.List;

import static org.securegraph.util.IterableUtils.toList;

public class SqlLongRunningProcessRepository extends LongRunningProcessRepository {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(SqlLongRunningProcessRepository.class);
    private final HibernateSessionManager sessionManager;
    private final UserRepository userRepository;
    private final Graph graph;
    private final WorkQueueRepository workQueueRepository;

    @Inject
    public SqlLongRunningProcessRepository(
            final HibernateSessionManager sessionManager,
            final UserRepository userRepository,
            final Graph graph,
            final WorkQueueRepository workQueueRepository) {
        this.sessionManager = sessionManager;
        this.userRepository = userRepository;
        this.graph = graph;
        this.workQueueRepository = workQueueRepository;
    }

    @Override
    public String enqueue(JSONObject longRunningProcessQueueItem, User user, Authorizations authorizations) {
        String longRunningProcessId;
        Session session = sessionManager.getSession();

        Transaction transaction = null;
        SqlLongRunningProcess longRunningProcess;
        try {
            transaction = session.beginTransaction();
            longRunningProcess = new SqlLongRunningProcess();
            longRunningProcessId = graph.getIdGenerator().nextId();
            longRunningProcess.setCanceled(false);
            longRunningProcess.setLongRunningProcessId(longRunningProcessId);
            longRunningProcessQueueItem.put("id", longRunningProcessId);
            longRunningProcess.setJson(longRunningProcessQueueItem.toString());
            if (user instanceof ProxyUser) {
                user = userRepository.findById(user.getUserId());
            }
            longRunningProcess.setUser((SqlUser) user);

            LOGGER.debug("add %s to long running process table", longRunningProcessId);
            session.save(longRunningProcess);
            session.update(user);
            transaction.commit();
        } catch (HibernateException e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw new LumifyException("Could not add long running process: " + longRunningProcessQueueItem.toString(), e);
        }
        return longRunningProcessId;
    }

    @Override
    public void beginWork(final JSONObject longRunningProcessQueueItem) {
        super.beginWork(longRunningProcessQueueItem);

        updateLongRunningProcess(longRunningProcessQueueItem, new UpdateLongRunningProcessAction() {
            @Override
            public void run(SqlLongRunningProcess longRunningProcess) {
                long startTime = longRunningProcessQueueItem.optLong("startTime", -1);
                if (startTime == -1) {
                    startTime = System.currentTimeMillis();
                }
                longRunningProcess.setStartTime(startTime);
            }
        });
    }

    @Override
    public void ack(final JSONObject longRunningProcessQueueItem) {
        updateLongRunningProcess(longRunningProcessQueueItem, new UpdateLongRunningProcessAction() {
            @Override
            public void run(SqlLongRunningProcess longRunningProcess) {
                long endTime = longRunningProcessQueueItem.optLong("endTime", -1);
                if (endTime == -1) {
                    endTime = System.currentTimeMillis();
                }
                longRunningProcess.setEndTime(endTime);
            }
        });
    }

    @Override
    public void nak(final JSONObject longRunningProcessQueueItem, Throwable ex) {
        updateLongRunningProcess(longRunningProcessQueueItem, new UpdateLongRunningProcessAction() {
            @Override
            public void run(SqlLongRunningProcess longRunningProcess) {
                long endTime = longRunningProcessQueueItem.optLong("endTime", -1);
                if (endTime == -1) {
                    endTime = System.currentTimeMillis();
                }
                longRunningProcess.setEndTime(endTime);
                longRunningProcess.setErred(true);
            }
        });
    }

    @Override
    public void cancel(String longRunningProcessId, User user) {
        updateLongRunningProcess(longRunningProcessId, new UpdateLongRunningProcessAction() {
            @Override
            public void run(SqlLongRunningProcess longRunningProcess) {
                longRunningProcess.setCanceled(true);
            }
        });
    }

    @Override
    public void reportProgress(JSONObject longRunningProcessQueueItem, final double progressPercent, final String message) {
        final JSONObject[] json = new JSONObject[1];
        updateLongRunningProcess(longRunningProcessQueueItem, new UpdateLongRunningProcessAction() {
            @Override
            public void run(SqlLongRunningProcess longRunningProcess) {
                json[0] = new JSONObject(longRunningProcess.getJson());
                json[0].put("progress", progressPercent);
                json[0].put("progressMessage", message);
                longRunningProcess.setJson(json[0].toString());
            }
        });
        workQueueRepository.broadcastLongRunningProcessChange(json[0]);
    }

    @Override
    public List<JSONObject> getLongRunningProcesses(User user) {
        Session session = sessionManager.getSession();
        List longRunningProcesses = session.createCriteria(SqlLongRunningProcess.class)
                .add(Restrictions.eq("user.userId", user.getUserId()))
                .list();
        return toList(new ConvertingIterable<Object, JSONObject>(longRunningProcesses) {
            @Override
            protected JSONObject convert(Object longRunningProcessObj) {
                SqlLongRunningProcess longRunningProcess = (SqlLongRunningProcess) longRunningProcessObj;
                JSONObject json = new JSONObject(longRunningProcess.getJson());
                json.put("id",longRunningProcess.getLongRunningProcessId());
                return json;
            }
        });
    }

    private SqlLongRunningProcess findSqlLongRunningProcessById(String longRunningProcessId) {
        Session session = sessionManager.getSession();
        List longRunningProcesses = session.createCriteria(SqlLongRunningProcess.class)
                .add(Restrictions.eq("longRunningProcessId", longRunningProcessId))
                .list();
        if (longRunningProcesses.size() == 0) {
            return null;
        } else if (longRunningProcesses.size() > 1) {
            throw new LumifyException("more than one long running process was returned");
        } else {
            return (SqlLongRunningProcess) longRunningProcesses.get(0);
        }
    }

    @Override
    public JSONObject findById(String longRunningProcessId, User user) {
        SqlLongRunningProcess sqlLongRunningProcess = findSqlLongRunningProcessById(longRunningProcessId);
        return new JSONObject(sqlLongRunningProcess.getJson());
    }

    @Override
    public void delete(String longRunningProcessId, User authUser) {
        Session session = sessionManager.getSession();
        SqlLongRunningProcess sqlLongRunningProcess = findSqlLongRunningProcessById(longRunningProcessId);
        Transaction transaction = null;
        try {
            transaction = session.beginTransaction();
            session.delete(sqlLongRunningProcess);
            transaction.commit();
        } catch (HibernateException e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw new RuntimeException(e);
        }
    }

    public void updateLongRunningProcess(String longRunningProcessId, UpdateLongRunningProcessAction updateLongRunningProcessAction) {
        SqlLongRunningProcess longRunningProcess = findSqlLongRunningProcessById(longRunningProcessId);
        Session session = sessionManager.getSession();
        Transaction transaction = null;
        try {
            transaction = session.beginTransaction();
            updateLongRunningProcessAction.run(longRunningProcess);
            session.update(longRunningProcess);
            transaction.commit();
        } catch (HibernateException e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw new LumifyException("Could not update long running process: " + longRunningProcess.toString(), e);
        }
    }

    public void updateLongRunningProcess(JSONObject longRunningProcessQueueItem, UpdateLongRunningProcessAction updateLongRunningProcessAction) {
        updateLongRunningProcess(longRunningProcessQueueItem.getString("id"), updateLongRunningProcessAction);
    }

    private abstract class UpdateLongRunningProcessAction {
        public abstract void run(SqlLongRunningProcess longRunningProcess);
    }
}
