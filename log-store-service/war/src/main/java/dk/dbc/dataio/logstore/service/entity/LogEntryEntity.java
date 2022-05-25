package dk.dbc.dataio.logstore.service.entity;

import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import java.sql.Timestamp;

@Entity
@javax.persistence.Table(name = "logentry")
@NamedQueries({
        @NamedQuery(name = LogEntryEntity.QUERY_FIND_ITEM_ENTRIES, query = "SELECT e FROM LogEntryEntity e WHERE e.jobId=:jobId AND e.chunkId=:chunkId AND e.itemId=:itemId ORDER BY e.id ASC"),
        @NamedQuery(name = LogEntryEntity.QUERY_DELETE_ITEM_ENTRIES_FOR_JOB, query = "DELETE FROM LogEntryEntity e WHERE e.jobId=:jobId")
})
public class LogEntryEntity {
    public static final String QUERY_FIND_ITEM_ENTRIES = "LogEntryEntity.findItemEntries";
    public static final String QUERY_DELETE_ITEM_ENTRIES_FOR_JOB = "LogEntryEntity.deleteItemEntriesForJob";

    private long id;

    @Id
    @javax.persistence.Column(name = "id")
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    private Timestamp timestamp;

    @Basic
    @javax.persistence.Column(name = "timestamp")
    public Timestamp getTimestamp() {
        return new Timestamp(timestamp.getTime());
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = new Timestamp(timestamp.getTime());
    }

    private String formattedMessage;

    @Basic
    @javax.persistence.Column(name = "formatted_message")
    public String getFormattedMessage() {
        return formattedMessage;
    }

    public void setFormattedMessage(String formattedMessage) {
        this.formattedMessage = formattedMessage;
    }

    private String threadName;

    @Basic
    @javax.persistence.Column(name = "thread_name")
    public String getThreadName() {
        return threadName;
    }

    public void setThreadName(String threadName) {
        this.threadName = threadName;
    }

    private String loggerName;

    @Basic
    @javax.persistence.Column(name = "logger_name")
    public String getLoggerName() {
        return loggerName;
    }

    public void setLoggerName(String loggerName) {
        this.loggerName = loggerName;
    }

    private String levelString;

    @Basic
    @javax.persistence.Column(name = "level_string")
    public String getLevelString() {
        return levelString;
    }

    public void setLevelString(String levelString) {
        this.levelString = levelString;
    }

    private String callerFilename;

    @Basic
    @javax.persistence.Column(name = "caller_filename")
    public String getCallerFilename() {
        return callerFilename;
    }

    public void setCallerFilename(String callerFilename) {
        this.callerFilename = callerFilename;
    }

    private String callerClass;

    @Basic
    @javax.persistence.Column(name = "caller_class")
    public String getCallerClass() {
        return callerClass;
    }

    public void setCallerClass(String callerClass) {
        this.callerClass = callerClass;
    }

    private String callerMethod;

    @Basic
    @javax.persistence.Column(name = "caller_method")
    public String getCallerMethod() {
        return callerMethod;
    }

    public void setCallerMethod(String callerMethod) {
        this.callerMethod = callerMethod;
    }

    private String callerLine;

    @Basic
    @javax.persistence.Column(name = "caller_line")
    public String getCallerLine() {
        return callerLine;
    }

    public void setCallerLine(String callerLine) {
        this.callerLine = callerLine;
    }

    private String stackTrace;

    @Basic
    @javax.persistence.Column(name = "stack_trace")
    public String getStackTrace() {
        return stackTrace;
    }

    public void setStackTrace(String stackTrace) {
        this.stackTrace = stackTrace;
    }

    private String mdc;

    @Basic
    @javax.persistence.Column(name = "mdc")
    public String getMdc() {
        return mdc;
    }

    public void setMdc(String mdc) {
        this.mdc = mdc;
    }

    private String jobId;

    @Basic
    @javax.persistence.Column(name = "job_id")
    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    private Long chunkId;

    @Basic
    @javax.persistence.Column(name = "chunk_id")
    public Long getChunkId() {
        return chunkId;
    }

    public void setChunkId(Long chunkId) {
        this.chunkId = chunkId;
    }

    private Long itemId;

    @Basic
    @javax.persistence.Column(name = "item_id")
    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        LogEntryEntity that = (LogEntryEntity) o;

        if (id != that.id) {
            return false;
        }
        if (callerClass != null ? !callerClass.equals(that.callerClass) : that.callerClass != null) {
            return false;
        }
        if (callerFilename != null ? !callerFilename.equals(that.callerFilename) : that.callerFilename != null) {
            return false;
        }
        if (callerLine != null ? !callerLine.equals(that.callerLine) : that.callerLine != null) {
            return false;
        }
        if (callerMethod != null ? !callerMethod.equals(that.callerMethod) : that.callerMethod != null) {
            return false;
        }
        if (chunkId != null ? !chunkId.equals(that.chunkId) : that.chunkId != null) {
            return false;
        }
        if (formattedMessage != null ? !formattedMessage.equals(that.formattedMessage) : that.formattedMessage != null) {
            return false;
        }
        if (itemId != null ? !itemId.equals(that.itemId) : that.itemId != null) {
            return false;
        }
        if (jobId != null ? !jobId.equals(that.jobId) : that.jobId != null) {
            return false;
        }
        if (levelString != null ? !levelString.equals(that.levelString) : that.levelString != null) {
            return false;
        }
        if (loggerName != null ? !loggerName.equals(that.loggerName) : that.loggerName != null) {
            return false;
        }
        if (mdc != null ? !mdc.equals(that.mdc) : that.mdc != null) {
            return false;
        }
        if (stackTrace != null ? !stackTrace.equals(that.stackTrace) : that.stackTrace != null) {
            return false;
        }
        if (threadName != null ? !threadName.equals(that.threadName) : that.threadName != null) {
            return false;
        }
        if (timestamp != null ? !timestamp.equals(that.timestamp) : that.timestamp != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = (int) (id ^ (id >>> 32));
        result = 31 * result + (timestamp != null ? timestamp.hashCode() : 0);
        result = 31 * result + (formattedMessage != null ? formattedMessage.hashCode() : 0);
        result = 31 * result + (threadName != null ? threadName.hashCode() : 0);
        result = 31 * result + (loggerName != null ? loggerName.hashCode() : 0);
        result = 31 * result + (levelString != null ? levelString.hashCode() : 0);
        result = 31 * result + (callerFilename != null ? callerFilename.hashCode() : 0);
        result = 31 * result + (callerClass != null ? callerClass.hashCode() : 0);
        result = 31 * result + (callerMethod != null ? callerMethod.hashCode() : 0);
        result = 31 * result + (callerLine != null ? callerLine.hashCode() : 0);
        result = 31 * result + (stackTrace != null ? stackTrace.hashCode() : 0);
        result = 31 * result + (mdc != null ? mdc.hashCode() : 0);
        result = 31 * result + (jobId != null ? jobId.hashCode() : 0);
        result = 31 * result + (chunkId != null ? chunkId.hashCode() : 0);
        result = 31 * result + (itemId != null ? itemId.hashCode() : 0);
        return result;
    }
}
