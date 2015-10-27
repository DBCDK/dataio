package dk.dbc.dataio.jobstore.types;

import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;

/**
 * Created by ThomasBerg on 27/10/15.
 */
public class AddNotificationRequest {

    private String destinationEmail;
    private IncompleteTransfileNotificationContext incompleteTransfileNotificationContext;
    private JobNotification.Type notificationType;

    public AddNotificationRequest(String destinationEmail, IncompleteTransfileNotificationContext incompleteTransfileNotificationContext, JobNotification.Type notificationType) {

        InvariantUtil.checkNotNullOrThrow(destinationEmail, "destinationEmail");
        InvariantUtil.checkNotNullOrThrow(incompleteTransfileNotificationContext, "incompleteTransfileNotificationContext");
        InvariantUtil.checkNotNullOrThrow(notificationType, "notificationType");

        this.destinationEmail = destinationEmail;
        this.incompleteTransfileNotificationContext = incompleteTransfileNotificationContext;
        this.notificationType = notificationType;
    }

    public String getDestinationEmail() {
        return destinationEmail;
    }
    public IncompleteTransfileNotificationContext getIncompleteTransfileNotificationContext() {
        return incompleteTransfileNotificationContext;
    }
    public JobNotification.Type getNotificationType() {
        return notificationType;
    }
}
