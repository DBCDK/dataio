package dk.dbc.dataio.logstore.service.ejb;

import dk.dbc.dataio.commons.types.rest.LogStoreServiceConstants;
import dk.dbc.dataio.logstore.service.entity.LogEntryEntity;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.util.List;

 /**
 * This stateless Enterprise Java Bean (EJB) class handles retrieval of log entries
 */
 @Stateless
 public class LogStoreBean {
     private static final String LOG_FORMAT_WITHOUT_STACKTRACE = "%s %s %s:%s %s%n";
     private static final String LOG_FORMAT_WITH_STACKTRACE = "%s %s %s:%s %s%n%s%n";

     @PersistenceContext
     EntityManager entityManager;

     /**
      * Retrieves log for given item in given chunk in given job
      * @param jobId ID of job
      * @param chunkId ID of chunk in job
      * @param itemId ID of item in chunk
      * @return log as string, or empty string if no entries could be found
      */
     public String getItemLog(String jobId, long chunkId, long itemId) {
         final List<LogEntryEntity> logentryEntities = getLogEntryEntities(jobId, chunkId, itemId);
         if (logentryEntities.isEmpty()) {
             return "";
         }
         final StringBuilder sb = new StringBuilder();
         for (LogEntryEntity logEntryEntity : logentryEntities) {
             sb.append(format(logEntryEntity));
         }
         return sb.toString();
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
                     logEntryEntity.getCallerFilename(),
                     logEntryEntity.getCallerLine(),
                     logEntryEntity.getFormattedMessage());
         } else {
             return String.format(LOG_FORMAT_WITH_STACKTRACE,
                     logEntryEntity.getTimestamp(),
                     logEntryEntity.getLevelString(),
                     logEntryEntity.getCallerFilename(),
                     logEntryEntity.getCallerLine(),
                     logEntryEntity.getFormattedMessage(),
                     logEntryEntity.getStackTrace());
         }
     }
 }
