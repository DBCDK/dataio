package dk.dbc.dataio.gui.client.components;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.InlineHTML;
import dk.dbc.dataio.gui.client.components.prompted.PromptedLabel;

public class JobNotificationPanel extends Composite {
    interface JobNotificationUiBinder extends UiBinder<HTMLPanel, JobNotificationPanel> {
    }

    private static JobNotificationUiBinder ourUiBinder = GWT.create(JobNotificationUiBinder.class);

    public JobNotificationPanel() {
        initWidget(ourUiBinder.createAndBindUi(this));
    }

    // UI Fields
    @UiField
    PromptedLabel jobId;
    @UiField
    PromptedLabel destination;
    @UiField
    PromptedLabel timeOfCreation;
    @UiField
    PromptedLabel timeOfLastModification;
    @UiField
    PromptedLabel type;
    @UiField
    PromptedLabel status;
    @UiField
    PromptedLabel statusMessage;
    @UiField
    InlineHTML content;

    /**
     * Set job id
     *
     * @param jobId The Job Id
     */
    public void setJobId(String jobId) {
        this.jobId.setText(jobId);
    }

    /**
     * Set destination
     *
     * @param destination The destination mail address
     */
    public void setDestination(String destination) {
        this.destination.setText(destination);
    }

    /**
     * Set Time of Creation
     *
     * @param timeOfCreation The time of creation
     */
    public void setTimeOfCreation(String timeOfCreation) {
        this.timeOfCreation.setText(timeOfCreation);
    }

    /**
     * Set time of last modification
     *
     * @param timeOfLastModification The time of the last modification
     */
    public void setTimeOfLastModification(String timeOfLastModification) {
        this.timeOfLastModification.setText(timeOfLastModification);
    }

    /**
     * Set Type
     *
     * @param type The type of the Job Notification
     */
    public void setType(String type) {
        this.type.setText(type);
    }

    /**
     * Set Status
     *
     * @param status The status of the Job Notification mail
     */
    public void setStatus(String status) {
        this.status.setText(status);
    }

    /**
     * The status message
     *
     * @param statusMessage The status message
     */
    public void setStatusMessage(String statusMessage) {
        this.statusMessage.setText(statusMessage);
    }

    /**
     * Set the content of the Job Notification (the body of the mail)
     *
     * @param content The content of the Job Notification
     */
    public void setContent(String content) {
        this.content.setText(content);
    }

}
