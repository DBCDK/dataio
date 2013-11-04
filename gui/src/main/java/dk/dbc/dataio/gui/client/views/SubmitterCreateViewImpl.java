package dk.dbc.dataio.gui.client.views;

import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.VerticalPanel;
import dk.dbc.dataio.gui.client.components.SaveButton;
import dk.dbc.dataio.gui.client.components.TextAreaEntry;
import dk.dbc.dataio.gui.client.components.TextEntry;
import dk.dbc.dataio.gui.client.exceptions.FlowStoreProxyError;
import dk.dbc.dataio.gui.client.presenters.SubmitterCreatePresenter;


public class SubmitterCreateViewImpl extends VerticalPanel implements SubmitterCreateView {
    // Constants (These are not all private since we use them in the selenium tests)
    public static final String CONTEXT_HEADER = "Submitter - opsætning";

    public static final String GUIID_SUBMITTER_CREATION_WIDGET = "submittercreationwidget";
    public static final String GUIID_SUBMITTER_CREATION_NUMBER_PANEL = "submittercreationnumberpanel";
    public static final String GUIID_SUBMITTER_CREATION_NAME_PANEL = "submittercreationnamepanel";
    public static final String GUIID_SUBMITTER_CREATION_DESCRIPTION_PANEL = "submittercreationdescriptionpanel";
    public static final String GUIID_SUBMITTER_CREATION_SAVE_BUTTON_PANEL = "submittercreationsavebuttonpanel";
    
    public static final String SAVE_RESULT_LABEL_SUCCES_MESSAGE = "Opsætningen blev gemt";
    public static final String SUBMITTER_CREATION_SUBMITTER_NUMBER_LABEL = "Submitternummer";
    public static final String SUBMITTER_CREATION_SUBMITTER_NAME_LABEL = "Submitternavn";
    public static final String SUBMITTER_CREATION_DESCRIPTION_LABEL = "Beskrivelse";
    
    public static final String SUBMITTER_CREATION_INPUT_FIELD_VALIDATION_ERROR = "Alle felter skal udfyldes.";
    public static final String SUBMITTER_CREATION_NUMBER_INPUT_FIELD_VALIDATION_ERROR = "Nummer felt skal indeholde en numerisk talværdi.";
    public static final String FLOW_STORE_PROXY_KEY_VIOLATION_ERROR_MESSAGE = "Et eller flere af de unikke felter { navn, nummer } er allerede oprettet i flow store.";
    public static final String FLOW_STORE_PROXY_DATA_VALIDATION_ERROR_MESSAGE = "De udfyldte felter forårsagede en data valideringsfejl i flow store.";
    private static final int SUBMITTER_CREATION_DESCRIPTION_MAX_LENGTH = 160;

    // Local variables
    private SubmitterCreatePresenter presenter;
    private final TextEntry submitterNumberPanel = new TextEntry(GUIID_SUBMITTER_CREATION_NUMBER_PANEL, SUBMITTER_CREATION_SUBMITTER_NUMBER_LABEL);
    private final TextEntry submitterNamePanel = new TextEntry(GUIID_SUBMITTER_CREATION_NAME_PANEL, SUBMITTER_CREATION_SUBMITTER_NAME_LABEL);
    private final TextAreaEntry submitterDescriptionPanel = new TextAreaEntry(GUIID_SUBMITTER_CREATION_DESCRIPTION_PANEL ,SUBMITTER_CREATION_DESCRIPTION_LABEL, SUBMITTER_CREATION_DESCRIPTION_MAX_LENGTH);
    private final SaveButton saveButton = new SaveButton(GUIID_SUBMITTER_CREATION_SAVE_BUTTON_PANEL, "Gem", new SaveButtonEvent());

    public SubmitterCreateViewImpl() {
        getElement().setId(GUIID_SUBMITTER_CREATION_WIDGET);
        
        submitterNumberPanel.addKeyDownHandler(new InputFieldKeyDownHandler());
        add(submitterNumberPanel);
        
        submitterNamePanel.addKeyDownHandler(new InputFieldKeyDownHandler());
        add(submitterNamePanel);

        submitterDescriptionPanel.addKeyDownHandler(new InputFieldKeyDownHandler());
        add(submitterDescriptionPanel);

        add(saveButton);
    }

    @Override
    public void setPresenter(SubmitterCreatePresenter presenter) {
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
    public void onSaveSubmitterSuccess() {
        onSuccess(SubmitterCreateViewImpl.SAVE_RESULT_LABEL_SUCCES_MESSAGE);
    }


    private class SaveButtonEvent implements SaveButton.ButtonEvent {
        @Override
        public void buttonPressed() {
            final String nameValue = submitterNamePanel.getText();
            final String numberValue = submitterNumberPanel.getText();
            final String descriptionValue = submitterDescriptionPanel.getText();
            final String validationError = validateFields(nameValue, numberValue, descriptionValue);
            if (!validationError.isEmpty()) {
                Window.alert(validationError);
            } else {
                presenter.saveSubmitter(nameValue, numberValue, descriptionValue);
            }
        }

        private String validateFields(final String nameValue, final String numberValue, final String descriptionValue) {
            if (nameValue.isEmpty() || numberValue.isEmpty() || descriptionValue.isEmpty()) {
                return SUBMITTER_CREATION_INPUT_FIELD_VALIDATION_ERROR;
            }
            try {
                Long.valueOf(numberValue);
            } catch (NumberFormatException e) {
                return SUBMITTER_CREATION_NUMBER_INPUT_FIELD_VALIDATION_ERROR;
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
