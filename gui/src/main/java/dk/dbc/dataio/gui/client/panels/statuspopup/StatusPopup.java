package dk.dbc.dataio.gui.client.panels.statuspopup;

import com.google.gwt.dom.client.Element;
import com.google.web.bindery.event.shared.EventBus;
import dk.dbc.dataio.gui.client.model.JobModel;

/**
 * Status Popup Panel
 */
public class StatusPopup extends StatusPopupWidget {
    private static final int POPUP_PANEL_WIDTH = 265;
    private static final int POPUP_PANEL_LEFT_OFFSET = 36;
    private static final int POPUP_PANEL_TOP_OFFSET = 18;



    /**
     * Constructor for the Status Popup Panel
     *
     * @param parent The future parent for the Status Popup Panel
     * @param model The Model containing job data
     */
    public StatusPopup(EventBus eventBus, Element parent, JobModel model) {
        super(eventBus, model.getJobId());
        setAutoHideOnHistoryEventsEnabled(true);
        setAnimationEnabled(true);
        int left = parent.getAbsoluteRight() - POPUP_PANEL_WIDTH - POPUP_PANEL_LEFT_OFFSET;
        int top = parent.getAbsoluteTop() + POPUP_PANEL_TOP_OFFSET;
        setPopupPosition(left, top);
        setWidth(POPUP_PANEL_WIDTH + "px");
        setPopupPanelContent(model);

        show();
    }

    /**
     * This method sets the data fields of the Popup Panel.
     * The data is fetched from the model parameter
     *
     * @param model The Job Model that holds the Status Popup Panel data to display
     */
    private void setPopupPanelContent(JobModel model) {
        totalFailed.setText(totalFailed.getText() + " " + String.valueOf(model.getChunkifyingTotalCounter()));
        chunkifyingSuccess.setText(String.valueOf(model.getChunkifyingSuccessCounter()));
        chunkifyingFailed.setText(String.valueOf(model.getChunkifyingFailureCounter()));
        chunkifyingIgnored.setText(String.valueOf(model.getChunkifyingIgnoredCounter()));
        processingSuccess.setText(String.valueOf(model.getProcessingSuccessCounter()));
        processingFailed.setText(String.valueOf(model.getProcessingFailureCounter()));
        processingIgnored.setText(String.valueOf(model.getProcessingIgnoredCounter()));
        deliveringSuccess.setText(String.valueOf(model.getDeliveringSuccessCounter()));
        deliveringFailed.setText(String.valueOf(model.getDeliveringFailureCounter()));
        deliveringIgnored.setText(String.valueOf(model.getDeliveringIgnoredCounter()));
    }
}
