package dk.dbc.dataio.commons.types;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.invariant.InvariantUtil;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Diagnostic {
    public enum Level {
        ERROR,      // an action returned an error response
        FATAL,      // an unexpected exception prevented us from completing an action
        ABORTED, // the user aborted the job
        WARNING
    }

    private final Level level;
    private final String message;
    private String stacktrace;
    private String tag;
    private String attribute;

    @JsonCreator
    public Diagnostic(@JsonProperty("level") Level level,
                      @JsonProperty("message") String message,
                      @JsonProperty("stacktrace") String stacktrace,
                      @JsonProperty("tag") String tag,
                      @JsonProperty("attribute") String attribute) {
        this.level = InvariantUtil.checkNotNullOrThrow(level, "level");
        this.message = InvariantUtil.checkNotNullNotEmptyOrThrow(message, "message");
        this.stacktrace = stacktrace;  // stacktrace is optional
        this.tag = tag;  // tag is optional
        this.attribute = attribute;  // attribute is optional
    }

    public Diagnostic(Level level, String message) throws NullPointerException, IllegalArgumentException {
        this(level, message, null, null, null);
    }

    public Diagnostic(Level level, String message, Throwable cause) throws NullPointerException, IllegalArgumentException {
        this(level, message);
        if (cause != null) {
            this.stacktrace = StringUtil.getStackTraceString(cause, "");
        }
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

    public String getTag() {
        return tag;
    }

    public Diagnostic withTag(String tag) {
        this.tag = tag;
        return this;
    }

    public String getAttribute() {
        return attribute;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Diagnostic)) return false;

        Diagnostic that = (Diagnostic) o;

        if (level != that.level) return false;
        if (!message.equals(that.message)) return false;
        if (stacktrace != null ? !stacktrace.equals(that.stacktrace) : that.stacktrace != null) return false;
        if (tag != null ? !tag.equals(that.tag) : that.tag != null) return false;
        return attribute != null ? attribute.equals(that.attribute) : that.attribute == null;

    }

    @Override
    public int hashCode() {
        int result = level.hashCode();
        result = 31 * result + message.hashCode();
        result = 31 * result + (stacktrace != null ? stacktrace.hashCode() : 0);
        result = 31 * result + (tag != null ? tag.hashCode() : 0);
        result = 31 * result + (attribute != null ? attribute.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Diagnostic{" +
                "level=" + level +
                ", message='" + message + '\'' +
                ", stacktrace='" + stacktrace + '\'' +
                ", tag='" + tag + '\'' +
                ", attribute='" + attribute + '\'' +
                '}';
    }
}
