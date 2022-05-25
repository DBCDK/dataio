package dk.dbc.dataio.logstore.logback;

import ch.qos.logback.classic.spi.CallerData;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import ch.qos.logback.core.db.DBAppenderBase;
import dk.dbc.dataio.logstore.types.LogStoreTrackingId;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LogStoreMergingJdbcAppender extends DBAppenderBase<ILoggingEvent> {
    private static final StackTraceElement EMPTY_CALLER_DATA = CallerData.naInstance();
    private static final String LOG_FORMAT_WITHOUT_STACKTRACE = "%s %s %s %s%n";
    private static final String LOG_FORMAT_WITH_STACKTRACE = "%s %s %s %s%n%s%n";

    private static final ConcurrentHashMap<String, List<ILoggingEvent>> loggingEvents = new ConcurrentHashMap<>(16, 0.9F, 1);

    @Override
    public void start() {
        try {
            super.start();
        } catch (IllegalStateException e) {
            addError("Error during appender start", e);
        }
        // trigger super class to think we are started even in case of failure
        started = true;
    }

    @Override
    protected void subAppend(ILoggingEvent event, Connection connection, PreparedStatement preparedStatement) throws Throwable {
    }

    @Override
    protected void secondarySubAppend(ILoggingEvent iLoggingEvent, Connection connection, long l) throws Throwable {
    }

    @Override
    protected Method getGeneratedKeysMethod() {
        return null;
    }

    @Override
    protected String getInsertSQL() {
        return LogStoreDbHelper.getInsertSQL();
    }

    @Override
    public void append(ILoggingEvent event) {
        final Map<String, String> mdcPropertyMap = event.getMDCPropertyMap();
        if (mdcPropertyMap != null && !mdcPropertyMap.isEmpty()) {
            final String trackingId = mdcPropertyMap.get(LogStoreTrackingId.LOG_STORE_TRACKING_ID_MDC_KEY);
            if (trackingId != null) {
                final String lockObject = getLockObject(trackingId);
                synchronized (lockObject) {
                    if (!loggingEvents.containsKey(lockObject)) {
                        loggingEvents.put(lockObject, new ArrayList<ILoggingEvent>());
                    }
                    loggingEvents.get(lockObject).add(event);
                    if (!mdcPropertyMap.containsKey(LogStoreTrackingId.LOG_STORE_TRACKING_ID_COMMIT_MDC_KEY)) {
                        return;
                    }

                    final String mergedMessage = mergeLoggingEvents(loggingEvents.get(lockObject));

                    try (Connection connection = connectionSource.getConnection();
                         PreparedStatement insertStatement = connection.prepareStatement(getInsertSQL())) {
                        connection.setAutoCommit(false);

                        insertStatement.setTimestamp(LogStoreDbHelper.TIMESTAMP, new Timestamp(event.getTimeStamp()));
                        insertStatement.setString(LogStoreDbHelper.FORMATTED_MESSAGE, mergedMessage);
                        insertStatement.setString(LogStoreDbHelper.LOGGER_NAME, event.getLoggerName());
                        insertStatement.setString(LogStoreDbHelper.LEVEL_STRING, event.getLevel().toString());
                        insertStatement.setString(LogStoreDbHelper.THREAD_NAME, event.getThreadName());
                        insertStatement.setString(LogStoreDbHelper.CALLER_FILENAME, EMPTY_CALLER_DATA.getFileName());
                        insertStatement.setString(LogStoreDbHelper.CALLER_CLASS, EMPTY_CALLER_DATA.getClassName());
                        insertStatement.setString(LogStoreDbHelper.CALLER_METHOD, EMPTY_CALLER_DATA.getMethodName());
                        insertStatement.setString(LogStoreDbHelper.CALLER_LINE, Integer.toString(EMPTY_CALLER_DATA.getLineNumber()));
                        insertStatement.setString(LogStoreDbHelper.STACK_TRACE, null);
                        insertStatement.setString(LogStoreDbHelper.MDC, mdcPropertyMap.toString());
                        appendLogStoreTrackingIdIfAvailable(trackingId, insertStatement);

                        if (insertStatement.executeUpdate() != 1) {
                            addWarn("Failed to insert logging event for " + trackingId);
                        }

                        // Not necessary when running under EJB transaction scope
                        connection.commit();
                    } catch (Throwable t) {
                        addError("Exception caught while appending logging events", t);
                    } finally {
                        loggingEvents.remove(lockObject);
                    }
                }
            }
        }
    }

    private String mergeLoggingEvents(List<ILoggingEvent> loggingEvents) {
        final StringBuilder mergedMessage = new StringBuilder();
        for (ILoggingEvent event : loggingEvents) {
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

    private void appendLogStoreTrackingIdIfAvailable(String trackingId, PreparedStatement preparedStatement) throws SQLException {
        try {
            final LogStoreTrackingId logStoreTrackingId = new LogStoreTrackingId(trackingId);
            preparedStatement.setString(LogStoreDbHelper.JOB_ID, logStoreTrackingId.getJobId());
            preparedStatement.setLong(LogStoreDbHelper.CHUNK_ID, logStoreTrackingId.getChunkId());
            preparedStatement.setLong(LogStoreDbHelper.ITEM_ID, logStoreTrackingId.getItemId());
            return;
        } catch (NullPointerException | IllegalArgumentException e) {
            addError(String.format("Unable to parse log-store tracking ID: %s", trackingId), e);
        }
        preparedStatement.setString(LogStoreDbHelper.JOB_ID, null);
        preparedStatement.setLong(LogStoreDbHelper.CHUNK_ID, 0);
        preparedStatement.setLong(LogStoreDbHelper.ITEM_ID, 0);
    }

    private static String getStackTraceString(IThrowableProxy e, String indent) {
        StringBuilder sb = new StringBuilder();
        sb.append(e.getClassName());
        sb.append(": ");
        sb.append(e.getMessage());
        sb.append("\n");

        StackTraceElementProxy[] stack = e.getStackTraceElementProxyArray();
        if (stack != null) {
            for (StackTraceElementProxy stackTraceElement : stack) {
                sb.append(indent);
                sb.append("\t");
                sb.append(stackTraceElement.toString());
                sb.append("\n");
            }
        }

        IThrowableProxy[] suppressedExceptions = e.getSuppressed();
        // Print suppressed exceptions indented one level deeper.
        if (suppressedExceptions != null) {
            for (IThrowableProxy throwable : suppressedExceptions) {
                sb.append(indent);
                sb.append("\tSuppressed: ");
                sb.append(getStackTraceString(throwable, indent + "\t"));
            }
        }

        IThrowableProxy cause = e.getCause();
        if (cause != null) {
            sb.append(indent);
            sb.append("Caused by: ");
            sb.append(getStackTraceString(cause, indent));
        }

        return sb.toString();
    }

    private String getLockObject(String id) {
        // Add namespace to given string to avoid global locking issues.
        // Use intern() method to get reference from String pool, so
        // that the returned string can be used as a monitor object in
        // a synchronized block.
        final String lock = this.getClass().getName() + "." + id;
        return lock.intern();
    }
}
