package dk.dbc.dataio.jobstore.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;
import dk.dbc.dataio.commons.utils.lang.StringUtil;

public class Diagnostic {
    public enum Level { FATAL, WARNING }

    private final Level level;
    private final String message;
    private String stacktrace;

    public Diagnostic(Level level, String message) throws NullPointerException, IllegalArgumentException {
        this.level = InvariantUtil.checkNotNullOrThrow(level, "level");
        this.message = InvariantUtil.checkNotNullNotEmptyOrThrow(message, "message");
    }

    public Diagnostic(Level level, String message, Throwable cause) throws NullPointerException, IllegalArgumentException {
        this(level, message);
        if (cause != null) {
            this.stacktrace = StringUtil.getStackTraceString(cause, "");
        }
    }

    @JsonCreator
    private Diagnostic(@JsonProperty("level") Level level,
                       @JsonProperty("message") String message,
                       @JsonProperty("stacktrace") String stacktrace) {
        this(level, message);
        this.stacktrace = stacktrace;
    }

    public Level getLevel() {
        return level;
    }

    public String getMessage() {
        return message;
    }

    public String getStacktrace() {
        return stacktrace;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Diagnostic)) return false;

        Diagnostic that = (Diagnostic) o;

        if (level != that.level) return false;
        if (!message.equals(that.message)) return false;
        return !(stacktrace != null ? !stacktrace.equals(that.stacktrace) : that.stacktrace != null);

    }

    @Override
    public int hashCode() {
        int result = level.hashCode();
        result = 31 * result + message.hashCode();
        result = 31 * result + (stacktrace != null ? stacktrace.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return " [level: '" + level + "', message: '" + message + "']";
    }
}
