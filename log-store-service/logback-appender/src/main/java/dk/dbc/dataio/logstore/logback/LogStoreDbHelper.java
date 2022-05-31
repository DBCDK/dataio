package dk.dbc.dataio.logstore.logback;

public class LogStoreDbHelper {
    public static final int TIMESTAMP = 1;
    public static final int FORMATTED_MESSAGE = 2;
    public static final int LOGGER_NAME = 3;
    public static final int LEVEL_STRING = 4;
    public static final int THREAD_NAME = 5;
    public static final int CALLER_FILENAME = 6;
    public static final int CALLER_CLASS = 7;
    public static final int CALLER_METHOD = 8;
    public static final int CALLER_LINE = 9;
    public static final int STACK_TRACE = 10;
    public static final int MDC = 11;
    public static final int JOB_ID = 12;
    public static final int CHUNK_ID = 13;
    public static final int ITEM_ID = 14;

    private LogStoreDbHelper() {
    }

    public static String getInsertSQL() {
        return "INSERT INTO LOGENTRY(TIMESTAMP, FORMATTED_MESSAGE, LOGGER_NAME, LEVEL_STRING, THREAD_NAME, CALLER_FILENAME, CALLER_CLASS, CALLER_METHOD, CALLER_LINE, STACK_TRACE, MDC, JOB_ID, CHUNK_ID, ITEM_ID)" +
                " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
    }
}
