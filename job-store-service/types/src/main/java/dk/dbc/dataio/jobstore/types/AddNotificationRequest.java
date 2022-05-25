package dk.dbc.dataio.jobstore.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import dk.dbc.invariant.InvariantUtil;

public class AddNotificationRequest {
    private String destinationEmail;
    private NotificationContext context;
    private Notification.Type notificationType;

    @JsonCreator
    public AddNotificationRequest(
            @JsonProperty("destinationEmail") String destinationEmail,
            @JsonProperty("context") NotificationContext context,
            @JsonProperty("notificationType") Notification.Type notificationType) {

        InvariantUtil.checkNotNullOrThrow(destinationEmail, "destinationEmail");
        InvariantUtil.checkNotNullOrThrow(context, "context");
        InvariantUtil.checkNotNullOrThrow(notificationType, "notificationType");

        this.destinationEmail = destinationEmail;
        this.context = context;
        this.notificationType = notificationType;
    }

    public String getDestinationEmail() {
        return destinationEmail;
    }

    public NotificationContext getContext() {
        return context;
    }

    public Notification.Type getNotificationType() {
        return notificationType;
    }
}
