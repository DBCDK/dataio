package dk.dbc.dataio.gui.client.pages.item.show;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTMLPanel;
import dk.dbc.dataio.gui.client.components.JobNotificationPanel;

public class JobNotificationsTabContent extends Composite {
    interface JobDiagnosticTabContentUiBinder extends UiBinder<HTMLPanel, JobNotificationsTabContent> {
    }

    private static JobDiagnosticTabContentUiBinder ourUiBinder = GWT.create(JobDiagnosticTabContentUiBinder.class);

    public JobNotificationsTabContent() {
        initWidget(ourUiBinder.createAndBindUi(this));
    }

    // UI Fields
    @UiField
    FlowPanel jobNotificationContainer;

    /*
     * Public methods
     */

    /**
     * Get the number of Job Notifications in the panel
     *
     * @return Number of Job Notifications in the panel
     */
    public int getNotificationsCount() {
        return jobNotificationContainer.getWidgetCount();
    }

    /**
     * Adds a Job Notification panel to the container
     *
     * @param panel The Job Notification panel to add to the container
     */
    public void add(JobNotificationPanel panel) {
        jobNotificationContainer.add(panel);
    }

    /**
     * Clears all panels from the container
     */
    public void clear() {
        jobNotificationContainer.clear();
    }

}
