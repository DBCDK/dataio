package dk.dbc.dataio.jobstore.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import dk.dbc.invariant.InvariantUtil;

import java.util.Objects;

public class InvalidTransfileNotificationContext implements NotificationContext {
    private String transfileName;
    private String transfileContent;
    private String cause;

    public InvalidTransfileNotificationContext() {
    }  // GWT demands this empty constructor - therefore: Do not delete it, though nobody uses it :)

    @JsonCreator
    public InvalidTransfileNotificationContext(
            @JsonProperty("transfileName") String transfileName,
            @JsonProperty("transfileContent") String transfileContent,
            @JsonProperty("cause") String cause) {

        InvariantUtil.checkNotNullOrThrow(transfileName, "transfileName");
        InvariantUtil.checkNotNullOrThrow(transfileContent, "transfileContent");
        InvariantUtil.checkNotNullOrThrow(cause, "cause");

        this.transfileName = transfileName;
        this.transfileContent = transfileContent;
        this.cause = cause;
    }

    public String getTransfileName() {
        return transfileName;
    }

    public String getTransfileContent() {
        return transfileContent;
    }

    public String getCause() {
        return cause;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        InvalidTransfileNotificationContext that = (InvalidTransfileNotificationContext) o;
        return Objects.equals(transfileName, that.transfileName) &&
                Objects.equals(transfileContent, that.transfileContent) &&
                Objects.equals(cause, that.cause);
    }

    @Override
    public int hashCode() {
        return Objects.hash(transfileName, transfileContent, cause);
    }
}
