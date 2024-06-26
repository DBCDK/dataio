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

@SuppressWarnings("java:S1860")
public class LogStoreBufferedJdbcAppender extends DBAppenderBase<ILoggingEvent> {
    private static final StackTraceElement EMPTY_CALLER_DATA = CallerData.naInstance();

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
        //long startTime = System.currentTimeMillis();
        final Map<String, String> mdcPropertyMap = event.getMDCPropertyMap();
        if (mdcPropertyMap != null && !mdcPropertyMap.isEmpty()) {
            final String trackingId = mdcPropertyMap.get(LogStoreTrackingId.LOG_STORE_TRACKING_ID_MDC_KEY);
            if (trackingId != null) {
                final String lockObject = getLockObject(trackingId);
                synchronized (lockObject) {
                    if (!loggingEvents.containsKey(lockObject)) {
                        loggingEvents.put(lockObject, new ArrayList<>());
                    }
                    loggingEvents.get(lockObject).add(event);
                    if (!mdcPropertyMap.containsKey(LogStoreTrackingId.LOG_STORE_TRACKING_ID_COMMIT_MDC_KEY)) {
                        return;
                    }
                    try (Connection connection = connectionSource.getConnection();
                         PreparedStatement insertStatement = connection.prepareStatement(getInsertSQL())) {
                        connection.setAutoCommit(false);

                        for (ILoggingEvent cachedEvent : loggingEvents.get(lockObject)) {
                            bindEventToPreparedStatement(cachedEvent, insertStatement);
                        }

                        for (int result : insertStatement.executeBatch()) {
                            if (result != 1) {
                                addError(String.format("One or more inserts in batch for %s failed", trackingId));
                                break;
                            }
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
        /* finally {
            addInfo(String.format("Elapsed time for append: %d ms", System.currentTimeMillis() - startTime));
        } */
    }

    private void bindEventToPreparedStatement(ILoggingEvent event, PreparedStatement preparedStatement) throws SQLException {
        preparedStatement.setTimestamp(LogStoreDbHelper.TIMESTAMP, new Timestamp(event.getTimeStamp()));
        preparedStatement.setString(LogStoreDbHelper.FORMATTED_MESSAGE, event.getFormattedMessage());
        preparedStatement.setString(LogStoreDbHelper.LOGGER_NAME, event.getLoggerName());
        preparedStatement.setString(LogStoreDbHelper.LEVEL_STRING, event.getLevel().toString());
        preparedStatement.setString(LogStoreDbHelper.THREAD_NAME, event.getThreadName());

        appendCallerDataIfAvailable(event, preparedStatement);
        appendThrowableIfAvailable(event, preparedStatement);
        appendMdcIfAvailable(event, preparedStatement);
        preparedStatement.addBatch();
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    private void appendCallerDataIfAvailable(ILoggingEvent event, PreparedStatement preparedStatement) throws SQLException {
        // Caller data is currently disabled since it makes no sense from a JavaScript
        // developers point of view - we get info from deep inside rhino reflection.
        /*
        final StackTraceElement[] callerData = event.getCallerData();
        final StackTraceElement caller;
        if (callerData != null && callerData.length > 0 && callerData[0] != null) {
            caller = callerData[0];
        } else {
            caller = EMPTY_CALLER_DATA;
        }
        */
        final StackTraceElement caller = EMPTY_CALLER_DATA;
        preparedStatement.setString(LogStoreDbHelper.CALLER_FILENAME, caller.getFileName());
        preparedStatement.setString(LogStoreDbHelper.CALLER_CLASS, caller.getClassName());
        preparedStatement.setString(LogStoreDbHelper.CALLER_METHOD, caller.getMethodName());
        preparedStatement.setString(LogStoreDbHelper.CALLER_LINE, Integer.toString(caller.getLineNumber()));
    }

    private void appendThrowableIfAvailable(ILoggingEvent event, PreparedStatement preparedStatement) throws SQLException {
        if (event.getThrowableProxy() != null) {
            preparedStatement.setString(LogStoreDbHelper.STACK_TRACE, getStackTraceString(event.getThrowableProxy(), ""));
        } else {
            preparedStatement.setString(LogStoreDbHelper.STACK_TRACE, null);
        }
    }

    private void appendMdcIfAvailable(ILoggingEvent event, PreparedStatement preparedStatement) throws SQLException {
        final Map<String, String> mdcPropertyMap = event.getMDCPropertyMap();
        if (mdcPropertyMap != null && !mdcPropertyMap.isEmpty()) {
            preparedStatement.setString(LogStoreDbHelper.MDC, event.getMDCPropertyMap().toString());
            appendLogStoreTrackingIdIfAvailable(mdcPropertyMap, preparedStatement);
        } else {
            preparedStatement.setString(LogStoreDbHelper.MDC, null);
        }
    }

    private void appendLogStoreTrackingIdIfAvailable(Map<String, String> mdcPropertyMap, PreparedStatement preparedStatement) throws SQLException {
        if (mdcPropertyMap.containsKey(LogStoreTrackingId.LOG_STORE_TRACKING_ID_MDC_KEY)) {
            final String logStoreTrackingIdString = mdcPropertyMap.get(LogStoreTrackingId.LOG_STORE_TRACKING_ID_MDC_KEY);
            try {
                final LogStoreTrackingId logStoreTrackingId = new LogStoreTrackingId(logStoreTrackingIdString);
                preparedStatement.setString(LogStoreDbHelper.JOB_ID, logStoreTrackingId.getJobId());
                preparedStatement.setLong(LogStoreDbHelper.CHUNK_ID, logStoreTrackingId.getChunkId());
                preparedStatement.setLong(LogStoreDbHelper.ITEM_ID, logStoreTrackingId.getItemId());
                return;
            } catch (NullPointerException | IllegalArgumentException e) {
                addError(String.format("Unable to parse log-store tracking ID: %s", logStoreTrackingIdString), e);
            }
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
