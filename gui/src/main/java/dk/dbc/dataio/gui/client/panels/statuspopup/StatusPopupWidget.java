package dk.dbc.dataio.gui.client.panels.statuspopup;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.web.bindery.event.shared.EventBus;
import dk.dbc.dataio.commons.types.ItemCompletionState;
import dk.dbc.dataio.commons.types.JobState;


/**
 * Status Popup panel
 */
public class StatusPopupWidget extends PopupPanel {
    private final EventBus eventBus;

    interface MyBinder extends UiBinder<Widget, StatusPopupWidget> {}
    private static MyBinder uiBinder = GWT.create(MyBinder.class);

    @UiField public Anchor totalFailed;
    @UiField public Anchor chunkifyingSuccess;
    @UiField public Anchor chunkifyingFailed;
    @UiField public Anchor chunkifyingIgnored;
    @UiField public Anchor processingSuccess;
    @UiField public Anchor processingFailed;
    @UiField public Anchor processingIgnored;
    @UiField public Anchor deliveringSuccess;
    @UiField public Anchor deliveringFailed;
    @UiField public Anchor deliveringIgnored;
    @UiField public Anchor moreInfo;
    public String jobId;


    /**
     * Constructor for the Status Popup Panel
     * @param eventBus Event bus to be used in the communication with the Status Popup Panel
     */
    public StatusPopupWidget(EventBus eventBus, String jobId) {
        super(true);  // True as a constructor parameter to the PopupPanel sets autohide feature on
        this.eventBus = eventBus;
        this.jobId = jobId;
        add(uiBinder.createAndBindUi(this));
    }

    @UiHandler("totalFailed")
    public void handleTotalFailedEvent(ClickEvent event) {
        this.hide();
        eventBus.fireEvent(new StatusPopupEvent(StatusPopupEvent.StatusPopupEventType.TOTAL_STATUS_INFO, jobId));
    }

    @UiHandler("chunkifyingSuccess")
    public void handleChunkifyingSuccessEvent(ClickEvent event) {
        this.hide();
        eventBus.fireEvent(new StatusPopupEvent(StatusPopupEvent.StatusPopupEventType.DETAILED_STATUS, jobId, JobState.OperationalState.CHUNKIFYING, ItemCompletionState.State.SUCCESS));
    }

    @UiHandler("chunkifyingFailed")
    public void handleChunkifyingFailedEvent(ClickEvent event) {
        this.hide();
        eventBus.fireEvent(new StatusPopupEvent(StatusPopupEvent.StatusPopupEventType.DETAILED_STATUS, jobId, JobState.OperationalState.CHUNKIFYING, ItemCompletionState.State.FAILURE));
    }

    @UiHandler("chunkifyingIgnored")
    public void handleChunkifyingIgnoredEvent(ClickEvent event) {
        this.hide();
        eventBus.fireEvent(new StatusPopupEvent(StatusPopupEvent.StatusPopupEventType.DETAILED_STATUS, jobId, JobState.OperationalState.CHUNKIFYING, ItemCompletionState.State.IGNORED));
    }

    @UiHandler("processingSuccess")
    public void handleProcessingSuccessEvent(ClickEvent event) {
        this.hide();
        eventBus.fireEvent(new StatusPopupEvent(StatusPopupEvent.StatusPopupEventType.DETAILED_STATUS, jobId, JobState.OperationalState.PROCESSING, ItemCompletionState.State.SUCCESS));
    }

    @UiHandler("processingFailed")
    public void handleProcessingFailedEvent(ClickEvent event) {
        this.hide();
        eventBus.fireEvent(new StatusPopupEvent(StatusPopupEvent.StatusPopupEventType.DETAILED_STATUS, jobId, JobState.OperationalState.PROCESSING, ItemCompletionState.State.FAILURE));
    }

    @UiHandler("processingIgnored")
    public void handleProcessingIgnoredEvent(ClickEvent event) {
        this.hide();
        eventBus.fireEvent(new StatusPopupEvent(StatusPopupEvent.StatusPopupEventType.DETAILED_STATUS, jobId, JobState.OperationalState.PROCESSING, ItemCompletionState.State.IGNORED));
    }

    @UiHandler("deliveringSuccess")
    public void handleDeliveringSuccessEvent(ClickEvent event) {
        this.hide();
        eventBus.fireEvent(new StatusPopupEvent(StatusPopupEvent.StatusPopupEventType.DETAILED_STATUS, jobId, JobState.OperationalState.DELIVERING, ItemCompletionState.State.SUCCESS));
    }

    @UiHandler("deliveringFailed")
    public void handleDeliveringFailedEvent(ClickEvent event) {
        this.hide();
        eventBus.fireEvent(new StatusPopupEvent(StatusPopupEvent.StatusPopupEventType.DETAILED_STATUS, jobId, JobState.OperationalState.DELIVERING, ItemCompletionState.State.FAILURE));
    }

    @UiHandler("deliveringIgnored")
    public void handleDeliveringIgnoredEvent(ClickEvent event) {
        this.hide();
        eventBus.fireEvent(new StatusPopupEvent(StatusPopupEvent.StatusPopupEventType.DETAILED_STATUS, jobId, JobState.OperationalState.DELIVERING, ItemCompletionState.State.IGNORED));
    }

    @UiHandler("moreInfo")
    public void handleMoreInfo(ClickEvent event) {
        this.hide();
        eventBus.fireEvent(new StatusPopupEvent(StatusPopupEvent.StatusPopupEventType.MORE_INFORMATION_REQUESTED, jobId));
    }

}