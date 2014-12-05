package dk.dbc.dataio.gui.client.panels.statuspopup;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTMLPanel;


/**
 * Status Popup panel
 */
public class StatusPopup extends FlowPanel {
    interface StatusPopupUiBinder extends UiBinder<HTMLPanel, StatusPopup> {}
    private static StatusPopupUiBinder uiBinder = GWT.create(StatusPopupUiBinder.class);

    public StatusPopup() {
        super();
        add(uiBinder.createAndBindUi(this));
    }

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

//    @UiHandler("totalFailed")
//    public void handleTotalFailedEvent(ClickEvent event) {
//        Window.alert("totalFailed");
//    }

    @UiHandler("chunkifyingSuccess")
    public void handleChunkifyingSuccessEvent(ClickEvent event) {
        Window.alert("chunkifyingSuccess");
    }

    @UiHandler("chunkifyingFailed")
    public void handleChunkifyingFailedEvent(ClickEvent event) {
        Window.alert("chunkifyingFailed");
    }

    @UiHandler("chunkifyingIgnored")
    public void handleChunkifyingIgnoredEvent(ClickEvent event) {
        Window.alert("chunkifyingIgnored");
    }

    @UiHandler("processingSuccess")
    public void handleProcessingSuccessEvent(ClickEvent event) {
        Window.alert("processingSuccess");
    }

    @UiHandler("processingFailed")
    public void handleProcessingFailedEvent(ClickEvent event) {
        Window.alert("processingFailed");
    }

    @UiHandler("processingIgnored")
    public void handleProcessingIgnoredEvent(ClickEvent event) {
        Window.alert("processingIgnored");
    }

    @UiHandler("deliveringSuccess")
    public void handleDeliveringSuccessEvent(ClickEvent event) {
        Window.alert("deliveringSuccess");
    }

    @UiHandler("deliveringFailed")
    public void handleDeliveringFailedEvent(ClickEvent event) {
        Window.alert("deliveringFailed");
    }

    @UiHandler("deliveringIgnored")
    public void handleDeliveringIgnoredEvent(ClickEvent event) {
        Window.alert("deliveringIgnored");
    }

//    @UiHandler("moreInfo")
//    public void handleMoreInfo(ClickEvent event) {
//        Window.alert("More Information");
//    }

}