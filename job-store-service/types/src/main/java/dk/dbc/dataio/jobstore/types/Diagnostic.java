package dk.dbc.dataio.jobstore.types;

import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;

public class Diagnostic {

    public enum Level { FATAL, WARNING }

    private final Level level;
    private final String message;
    private final String stacktrace;

    public Diagnostic(Level level, String message, String stacktrace) throws NullPointerException, IllegalArgumentException {

        this.level = InvariantUtil.checkNotNullOrThrow(level, "level");
        this.message = InvariantUtil.checkNotNullNotEmptyOrThrow(message, "message");
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
}
