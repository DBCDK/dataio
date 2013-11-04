package dk.dbc.dataio.gui.client.views;

import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.VerticalPanel;
import dk.dbc.dataio.gui.client.components.SaveButton;
import dk.dbc.dataio.gui.client.components.TextEntry;
import dk.dbc.dataio.gui.client.exceptions.FlowStoreProxyError;
import dk.dbc.dataio.gui.client.presenters.SinkCreatePresenter;


public class SinkCreateViewImpl extends VerticalPanel implements SinkCreateView {
    public static final String CONTEXT_HEADER = "Sink - opsætning";

    public static final String GUIID_SINK_CREATION_WIDGET = "sinkcreationwidget";
    public static final String GUIID_SINK_CREATION_SINK_NAME_PANEL = "sinkcreationsinknamepanel";
    public static final String GUIID_SINK_CREATION_RESOURCE_NAME_PANEL = "sinkcreationresourcenamepanel";
    public static final String GUIID_SINK_CREATION_SAVE_BUTTON_PANEL = "sinkcreationsavebuttonpanel";
    
    public static final String SAVE_RESULT_LABEL_SUCCES_MESSAGE = "Opsætningen blev gemt";

    public static final String SINK_CREATION_SINK_NAME_LABEL = "Sink navn";
    public static final String SINK_CREATION_RESOURCE_NAME_LABEL = "Resource navn";
    public static final String SINK_CREATION_INPUT_FIELD_VALIDATION_ERROR = "Alle felter skal udfyldes.";
    public static final String SINK_CREATION_SAVE_BUTTON_LABEL = "Gem";
    
    public static final String FLOW_STORE_PROXY_KEY_VIOLATION_ERROR_MESSAGE = "En sink med det pågældende navn er allerede oprettet i flow store.";
    public static final String FLOW_STORE_PROXY_DATA_VALIDATION_ERROR_MESSAGE = "De udfyldte felter forårsagede en data valideringsfejl i flow store.";
    
    // Local variables
    private SinkCreatePresenter presenter;
    private final TextEntry sinkNamePanel = new TextEntry(GUIID_SINK_CREATION_SINK_NAME_PANEL, SINK_CREATION_SINK_NAME_LABEL);
    private final TextEntry resourceNamePanel = new TextEntry(GUIID_SINK_CREATION_RESOURCE_NAME_PANEL, SINK_CREATION_RESOURCE_NAME_LABEL);
    private final SaveButton saveButton = new SaveButton(GUIID_SINK_CREATION_SAVE_BUTTON_PANEL, SINK_CREATION_SAVE_BUTTON_LABEL, new SaveButtonEvent());

    
    public SinkCreateViewImpl() {
        getElement().setId(GUIID_SINK_CREATION_WIDGET);

        sinkNamePanel.addKeyDownHandler(new InputFieldKeyDownHandler());
        add(sinkNamePanel);
        
        resourceNamePanel.addKeyDownHandler(new InputFieldKeyDownHandler());
        add(resourceNamePanel);

        add(saveButton);
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
        saveButton.setStatusText(message);
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
                case NOT_ACCEPTABLE: errorMessage = FLOW_STORE_PROXY_KEY_VIOLATION_ERROR_MESSAGE;
                    break;
                case BAD_REQUEST: errorMessage = FLOW_STORE_PROXY_DATA_VALIDATION_ERROR_MESSAGE;
                    break;
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

    
    private class SaveButtonEvent implements SaveButton.ButtonEvent {
        @Override
        public void buttonPressed() {
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
            saveButton.setStatusText("");
        }
    }
}
