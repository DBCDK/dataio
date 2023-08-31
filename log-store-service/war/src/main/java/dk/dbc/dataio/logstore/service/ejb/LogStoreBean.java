package dk.dbc.dataio.logstore.service.ejb;

import dk.dbc.dataio.commons.types.interceptor.Stopwatch;
import dk.dbc.dataio.commons.types.rest.LogStoreServiceConstants;
import dk.dbc.dataio.logstore.service.entity.LogEntryEntity;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

import java.util.List;

/**
 * This stateless Enterprise Java Bean (EJB) class handles retrieval of log entries
 */
@Stateless
public class LogStoreBean {
    public static final String ENTRY_SEPARATOR = System.lineSeparator()
            + "___END_OF_LOG_ENTRY___" + System.lineSeparator() + System.lineSeparator();

    private static final String LOG_FORMAT_WITHOUT_STACKTRACE = "%s %s %s %s%n";
    private static final String LOG_FORMAT_WITH_STACKTRACE = "%s %s %s %s%n%s%n";

    @PersistenceContext
    EntityManager entityManager;

    /**
     * Retrieves log for given item in given chunk in given job
     *
     * @param jobId   ID of job
     * @param chunkId ID of chunk in job
     * @param itemId  ID of item in chunk
     * @return log as string, or empty string if no entries could be found
     */
    @Stopwatch
    public String getItemLog(String jobId, long chunkId, long itemId) {
        final List<LogEntryEntity> logentryEntities = getLogEntryEntities(jobId, chunkId, itemId);
        if (logentryEntities.isEmpty()) {
            return "";
        }
        final StringBuilder sb = new StringBuilder();
        if (logentryEntities.size() < 3) {
            for (LogEntryEntity logEntryEntity : logentryEntities) {
                sb.append(logEntryEntity.getFormattedMessage()).append(ENTRY_SEPARATOR);
            }
        } else {
            for (LogEntryEntity logEntryEntity : logentryEntities) {
                sb.append(format(logEntryEntity));
            }
        }
        return sb.toString();
    }

    /**
     * Deletes the log for given job
     *
     * @param jobId ID of job
     * @return number of affected rows.
     */
    @Stopwatch
    public int deleteJobLog(String jobId) {
        final Query query = entityManager.createNamedQuery(LogEntryEntity.QUERY_DELETE_ITEM_ENTRIES_FOR_JOB);
        query.setParameter(LogStoreServiceConstants.JOB_ID_VARIABLE, jobId);
        return query.executeUpdate();
    }

    private List<LogEntryEntity> getLogEntryEntities(String jobId, long chunkId, long itemId) {
        final Query query = entityManager.createNamedQuery(LogEntryEntity.QUERY_FIND_ITEM_ENTRIES);
        query.setParameter(LogStoreServiceConstants.JOB_ID_VARIABLE, jobId);
        query.setParameter(LogStoreServiceConstants.CHUNK_ID_VARIABLE, chunkId);
        query.setParameter(LogStoreServiceConstants.ITEM_ID_VARIABLE, itemId);
        return query.getResultList();
    }

    private String format(LogEntryEntity logEntryEntity) {
        if (logEntryEntity.getStackTrace() == null) {
            return String.format(LOG_FORMAT_WITHOUT_STACKTRACE,
                    logEntryEntity.getTimestamp(),
                    logEntryEntity.getLevelString(),
                    logEntryEntity.getLoggerName(),
                    logEntryEntity.getFormattedMessage());
        } else {
            return String.format(LOG_FORMAT_WITH_STACKTRACE,
                    logEntryEntity.getTimestamp(),
                    logEntryEntity.getLevelString(),
                    logEntryEntity.getLoggerName(),
                    logEntryEntity.getFormattedMessage(),
                    logEntryEntity.getStackTrace());
        }
    }
}
