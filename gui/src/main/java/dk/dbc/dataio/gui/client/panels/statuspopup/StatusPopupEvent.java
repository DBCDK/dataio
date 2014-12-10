package dk.dbc.dataio.gui.client.panels.statuspopup;

import com.google.web.bindery.event.shared.binder.GenericEvent;
import dk.dbc.dataio.commons.types.ItemCompletionState;
import dk.dbc.dataio.commons.types.JobState;

/**
 * Status Popup Panel Event used in the communication between StatusPopup Panel and its owner
 */
public class StatusPopupEvent extends GenericEvent {
    private final StatusPopupEventType eventType;
    private final String jobId;
    private final JobState.OperationalState operationalState;
    private final ItemCompletionState.State completionState;



    public enum StatusPopupEventType {DETAILED_STATUS, TOTAL_STATUS_INFO, MORE_INFORMATION_REQUESTED}
    /**
     * Constructor, carrying only the Status Popup Event Type and the Job Id parameters
     * To be used for the Event Types: TOTAL_STATUS_INFO and MORE_INFORMATION_REQUESTED
     *
     * @param eventType Status Popup Event Type
     * @param jobId Job Id
     */
    public StatusPopupEvent(StatusPopupEventType eventType, String jobId) {
        this.eventType = eventType;
        this.jobId = jobId;
        this.operationalState = null;
        this.completionState = null;
    }

    /**
     * Constructor, carrying the parameters eventType, jobId, operationalState and completionState
     *
     * @param eventType Status Popup Event Type
     * @param jobId Job Id
     * @param operationalState Operational State
     * @param completionState  Completion State
     */
    public StatusPopupEvent(StatusPopupEventType eventType, String jobId, JobState.OperationalState operationalState, ItemCompletionState.State completionState) {
        this.eventType = eventType;
        this.jobId = jobId;
        this.operationalState = operationalState;
        this.completionState = completionState;
    }

    /**
     * Getter for Event Type
     *
     * @return Event Type
     */
    public StatusPopupEventType getStatusPopupEventType() {
        return eventType;
    }

    /**
     * Getter for Job Id
     * @return Job Id
     */
    public String getJobId() {
        return jobId;
    }

    /**
     * Getter for Operational State
     *
     * @return Operational State
     */
    public JobState.OperationalState getOperationalState() {
        return operationalState;
    }

    /**
     * Getter for Completion State
     *
     * @return Completion State
     */
    public ItemCompletionState.State getCompletionState() {
        return completionState;
    }
}
