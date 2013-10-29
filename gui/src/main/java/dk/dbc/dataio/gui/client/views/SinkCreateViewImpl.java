/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package dk.dbc.dataio.gui.client.views;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import dk.dbc.dataio.gui.client.components.TextEntry;
import dk.dbc.dataio.gui.client.exceptions.FlowStoreProxyError;
import dk.dbc.dataio.gui.client.presenters.SinkCreatePresenter;


public class SinkCreateViewImpl extends VerticalPanel implements SinkCreateView {
    public static final String CONTEXT_HEADER = "Sink - opsætning";

    public static final String GUIID_SINK_CREATION_WIDGET = "sinkcreationwidget";
    public static final String GUIID_SINK_CREATION_SINK_NAME_PANEL = "sinkcreationsinknamepanel";
    public static final String GUIID_SINK_CREATION_RESOURCE_NAME_PANEL = "sinkcreationresourcenamepanel";
    public static final String GUIID_SINK_CREATION_SAVE_RESULT_LABEL = "sinkcreationsaveresultlabel";
    public static final String GUIID_SINK_CREATION_SAVE_BUTTON = "sinkcreationsavebutton";
    
    public static final String SAVE_RESULT_LABEL_SUCCES_MESSAGE = "Opsætningen blev gemt";

    public static final String SINK_CREATION_SINK_NAME_LABEL = "Sink navn";
    public static final String SINK_CREATION_RESOURCE_NAME_LABEL = "Resource navn";
    public static final String SINK_CREATION_INPUT_FIELD_VALIDATION_ERROR = "Alle felter skal udfyldes.";
    
    public static final String SINK_CREATION_xxxxx_ERROR = "xxxxx";
    
    // Local variables
    private SinkCreatePresenter presenter;
    private final TextEntry sinkNamePanel = new TextEntry(GUIID_SINK_CREATION_SINK_NAME_PANEL, SINK_CREATION_SINK_NAME_LABEL);
    private final TextEntry resourceNamePanel = new TextEntry(GUIID_SINK_CREATION_RESOURCE_NAME_PANEL, SINK_CREATION_RESOURCE_NAME_LABEL);
    private final SinkSavePanel sinkSavePanel = new SinkSavePanel();

    
    
    public SinkCreateViewImpl() {
        getElement().setId(GUIID_SINK_CREATION_WIDGET);

        sinkNamePanel.addKeyDownHandler(new SinkCreateViewImpl.InputFieldKeyDownHandler());
        add(sinkNamePanel);
        
        resourceNamePanel.addKeyDownHandler(new SinkCreateViewImpl.InputFieldKeyDownHandler());
        add(resourceNamePanel);

        add(sinkSavePanel);
    }
    
    @Override
    public void setPresenter(SinkCreatePresenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public void onFailure(String message) {
        Window.alert("Error: " + message);
    }

    @Override
    public void onSuccess(String message) {
        sinkSavePanel.setStatusText(message);
    }

    @Override
    public void refresh() {
    }

    @Override
    public void onFlowStoreProxyFailure(FlowStoreProxyError errorCode, String detail) {
        final String errorMessage;
        if (errorCode == null) {
            errorMessage = detail;
        } else {
            switch (errorCode) {
//                case NOT_ACCEPTABLE: errorMessage = FLOW_STORE_PROXY_KEY_VIOLATION_ERROR_MESSAGE;
//                    break;
//                case BAD_REQUEST: errorMessage = FLOW_STORE_PROXY_DATA_VALIDATION_ERROR_MESSAGE;
//                    break;
                default: errorMessage = detail;
                    break;
            }
        }
        onFailure(errorMessage);
    }

    @Override
    public void onSaveSinkSuccess() {
        onSuccess(SAVE_RESULT_LABEL_SUCCES_MESSAGE);
    }

    
    private class SinkSavePanel extends HorizontalPanel {

        private final Button sinkSaveButton = new Button("Gem");
        private final Label sinkSaveResultLabel = new Label("");

        public SinkSavePanel() {
            sinkSaveResultLabel.getElement().setId(GUIID_SINK_CREATION_SAVE_RESULT_LABEL);
            add(sinkSaveResultLabel);
            getElement().setId("sink-save-panel-id");
            sinkSaveButton.getElement().setId(GUIID_SINK_CREATION_SAVE_BUTTON);
            sinkSaveButton.addClickHandler(new SinkCreateViewImpl.SaveButtonHandler());
            add(sinkSaveButton);
        }

        public void setStatusText(String statusText) {
            sinkSaveResultLabel.setText(statusText);
        }
    }

    private class SaveButtonHandler implements ClickHandler {
        @Override
        public void onClick(ClickEvent event) {
            final String nameValue = sinkNamePanel.getText();
            final String resourceValue = resourceNamePanel.getText();
            final String validationError = validateFields(nameValue, resourceValue);
            if (!validationError.isEmpty()) {
                Window.alert(validationError);
            } else {
                presenter.saveSink(nameValue, resourceValue);
            }
        }

        private String validateFields(final String nameValue, final String resourceValue) {
            if (nameValue.isEmpty() || resourceValue.isEmpty()) {
                return SINK_CREATION_INPUT_FIELD_VALIDATION_ERROR;
            }
            return "";
        }
    }

    private class InputFieldKeyDownHandler implements KeyDownHandler {
        @Override
        public void onKeyDown(KeyDownEvent keyDownEvent) {
            sinkSavePanel.setStatusText("");
        }
    }
}
