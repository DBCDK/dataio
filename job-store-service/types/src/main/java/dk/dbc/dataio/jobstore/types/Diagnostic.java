package dk.dbc.dataio.jobstore.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;

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
            this.stacktrace = getStackTraceString(cause, "");
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

    public static String getStackTraceString(Throwable e, String indent) {
        final StringBuilder sb = new StringBuilder();
        sb.append(e.toString());
        sb.append("\n");

        final StackTraceElement[] stack = e.getStackTrace();
        if (stack != null) {
            for (StackTraceElement stackTraceElement : stack) {
                sb.append(indent);
                sb.append("\tat ");
                sb.append(stackTraceElement.toString());
                sb.append("\n");
            }
        }

        final Throwable[] suppressedExceptions = e.getSuppressed();
        // Print suppressed exceptions indented one level deeper.
        if (suppressedExceptions != null) {
            for (Throwable throwable : suppressedExceptions) {
                sb.append(indent);
                sb.append("\tSuppressed: ");
                sb.append(getStackTraceString(throwable, indent + "\t"));
            }
        }

        final Throwable cause = e.getCause();
        if (cause != null) {
            sb.append(indent);
            sb.append("Caused by: ");
            sb.append(getStackTraceString(cause, indent));
        }

        return sb.toString();
    }
}
