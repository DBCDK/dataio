package dk.dbc.dataio.jobprocessorgjs.logstore;

import ch.qos.logback.classic.spi.CallerData;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import dk.dbc.dataio.logstore.logback.LogStoreDbHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.util.List;

/**
 * Writes the JavaScript log events captured for a single chunk item to the log-store
 * database as one merged {@code LOGENTRY} row.
 */
public class LogStoreWriter {
    private static final Logger LOGGER = LoggerFactory.getLogger(LogStoreWriter.class);

    private static final StackTraceElement EMPTY_CALLER_DATA = CallerData.naInstance();
    private static final String LOG_FORMAT_WITHOUT_STACKTRACE = "%s %s %s %s%n";
    private static final String LOG_FORMAT_WITH_STACKTRACE = "%s %s %s %s%n%s%n";

    /** No-op writer for use when no log-store data source is available (e.g. unit tests). */
    public static final LogStoreWriter NOOP = new LogStoreWriter(null);

    private final DataSource dataSource;

    public LogStoreWriter(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Persists the given log events for one item as a single merged log-store row.
     * Does nothing when no data source is configured or when there are no events.
     *
     * @param jobId   job ID part of the log-store tracking ID
     * @param chunkId chunk ID part of the log-store tracking ID
     * @param itemId  item ID part of the log-store tracking ID
     * @param events  JavaScript log events captured for the item, may be null or empty
     */
    public void write(String jobId, long chunkId, long itemId, List<ILoggingEvent> events) {
        if (dataSource == null || events == null || events.isEmpty()) {
            return;
        }

        final ILoggingEvent last = events.get(events.size() - 1);
        final String mergedMessage = mergeLoggingEvents(events);

        try (Connection connection = dataSource.getConnection();
             PreparedStatement insertStatement = connection.prepareStatement(LogStoreDbHelper.getInsertSQL())) {
            connection.setAutoCommit(false);

            insertStatement.setTimestamp(LogStoreDbHelper.TIMESTAMP, new Timestamp(last.getTimeStamp()));
            insertStatement.setString(LogStoreDbHelper.FORMATTED_MESSAGE, mergedMessage);
            insertStatement.setString(LogStoreDbHelper.LOGGER_NAME, last.getLoggerName());
            insertStatement.setString(LogStoreDbHelper.LEVEL_STRING, last.getLevel().toString());
            insertStatement.setString(LogStoreDbHelper.THREAD_NAME, last.getThreadName());
            insertStatement.setString(LogStoreDbHelper.CALLER_FILENAME, EMPTY_CALLER_DATA.getFileName());
            insertStatement.setString(LogStoreDbHelper.CALLER_CLASS, EMPTY_CALLER_DATA.getClassName());
            insertStatement.setString(LogStoreDbHelper.CALLER_METHOD, EMPTY_CALLER_DATA.getMethodName());
            insertStatement.setString(LogStoreDbHelper.CALLER_LINE, Integer.toString(EMPTY_CALLER_DATA.getLineNumber()));
            insertStatement.setString(LogStoreDbHelper.STACK_TRACE, null);
            insertStatement.setString(LogStoreDbHelper.MDC, null);
            insertStatement.setString(LogStoreDbHelper.JOB_ID, jobId);
            insertStatement.setLong(LogStoreDbHelper.CHUNK_ID, chunkId);
            insertStatement.setLong(LogStoreDbHelper.ITEM_ID, itemId);

            if (insertStatement.executeUpdate() != 1) {
                LOGGER.warn("Failed to insert log-store entry for {}/{}/{}", jobId, chunkId, itemId);
            }
            connection.commit();
        } catch (Exception e) {
            // Persisting item logs must never fail the processing of the item itself.
            LOGGER.error("Unable to write log-store entry for {}/{}/{}", jobId, chunkId, itemId, e);
        }
    }

    private String mergeLoggingEvents(List<ILoggingEvent> events) {
        final StringBuilder mergedMessage = new StringBuilder();
        for (ILoggingEvent event : events) {
            if (event.getThrowableProxy() == null) {
                mergedMessage.append(String.format(LOG_FORMAT_WITHOUT_STACKTRACE,
                        new Timestamp(event.getTimeStamp()),
                        event.getLevel().toString(),
                        event.getLoggerName(),
                        event.getFormattedMessage()));
            } else {
                mergedMessage.append(String.format(LOG_FORMAT_WITH_STACKTRACE,
                        new Timestamp(event.getTimeStamp()),
                        event.getLevel().toString(),
                        event.getLoggerName(),
                        event.getFormattedMessage(),
                        getStackTraceString(event.getThrowableProxy(), "")));
            }
        }
        return mergedMessage.toString();
    }

    private static String getStackTraceString(IThrowableProxy e, String indent) {
        final StringBuilder sb = new StringBuilder();
        sb.append(e.getClassName());
        sb.append(": ");
        sb.append(e.getMessage());
        sb.append("\n");

        final StackTraceElementProxy[] stack = e.getStackTraceElementProxyArray();
        if (stack != null) {
            for (StackTraceElementProxy stackTraceElement : stack) {
                sb.append(indent);
                sb.append("\t");
                sb.append(stackTraceElement.toString());
                sb.append("\n");
            }
        }

        final IThrowableProxy[] suppressedExceptions = e.getSuppressed();
        if (suppressedExceptions != null) {
            for (IThrowableProxy throwable : suppressedExceptions) {
                sb.append(indent);
                sb.append("\tSuppressed: ");
                sb.append(getStackTraceString(throwable, indent + "\t"));
            }
        }

        final IThrowableProxy cause = e.getCause();
        if (cause != null) {
            sb.append(indent);
            sb.append("Caused by: ");
            sb.append(getStackTraceString(cause, indent));
        }

        return sb.toString();
    }
}
