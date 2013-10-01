package dk.dbc.dataio.gui.client.views;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.VerticalPanel;
import dk.dbc.dataio.gui.client.components.TextEntry;
import dk.dbc.dataio.gui.client.exceptions.FlowStoreProxyError;
import dk.dbc.dataio.gui.client.presenters.SubmitterCreatePresenter;


public class SubmitterCreateViewImpl extends VerticalPanel implements SubmitterCreateView {
    // Constants (These are not all private since we use them in the selenium tests)
    public static final String CONTEXT_HEADER = "Submitter - opsætning";

    public static final String GUIID_SUBMITTER_CREATION_WIDGET = "submittercreationwidget";
    public static final String GUIID_SUBMITTER_CREATION_NUMBER_PANEL = "submittercreationnumberpanel";
    public static final String GUIID_SUBMITTER_CREATION_NAME_PANEL = "submittercreationnamepanel";
    public static final String GUIID_SUBMITTER_CREATION_DESCRIPTION_TEXT_AREA = "submittercreationdescriptiontextarea";
    public static final String GUIID_SUBMITTER_CREATION_SAVE_BUTTON = "submittercreationsavebutton";
    public static final String GUIID_SUBMITTER_CREATION_SAVE_RESULT_LABEL = "submittercreationsaveresultlabel";
    
    public static final String SAVE_RESULT_LABEL_SUCCES_MESSAGE = "Opsætningen blev gemt";
    public static final String SUBMITTER_CREATION_INPUT_FIELD_VALIDATION_ERROR = "Alle felter skal udfyldes.";
    public static final String SUBMITTER_CREATION_NUMBER_INPUT_FIELD_VALIDATION_ERROR = "Nummer felt skal indeholde en numerisk talværdi.";
    public static final String FLOW_STORE_PROXY_KEY_VIOLATION_ERROR_MESSAGE = "Et eller flere af de unikke felter { navn, nummer } er allerede oprettet i flow store.";
    public static final String FLOW_STORE_PROXY_DATA_VALIDATION_ERROR_MESSAGE = "De udfyldte felter forårsagede en data valideringsfejl i flow store.";
    private static final int SUBMITTER_CREATION_DESCRIPTION_MAX_LENGTH = 160;

    // Local variables
    private SubmitterCreatePresenter presenter;
    private final TextEntry submitterNumberPanel = new TextEntry("Submitternummer");
    private final TextEntry submitterNamePanel = new TextEntry("Submitternavn");
    private final SubmitterCreateViewImpl.SubmitterDescriptionPanel submitterDescriptionPanel = new SubmitterCreateViewImpl.SubmitterDescriptionPanel();
    private final SubmitterCreateViewImpl.SubmitterSavePanel submitterSavePanel = new SubmitterCreateViewImpl.SubmitterSavePanel();

    public SubmitterCreateViewImpl() {
        getElement().setId(GUIID_SUBMITTER_CREATION_WIDGET);
        
        submitterNumberPanel.getElement().setId(GUIID_SUBMITTER_CREATION_NUMBER_PANEL);
        submitterNumberPanel.addKeyDownHandler(new InputFieldKeyDownHandler());
        add(submitterNumberPanel);
        
        submitterNamePanel.getElement().setId(GUIID_SUBMITTER_CREATION_NAME_PANEL);
        submitterNamePanel.addKeyDownHandler(new InputFieldKeyDownHandler());
        add(submitterNamePanel);

        add(submitterDescriptionPanel);
        add(submitterSavePanel);
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
        submitterSavePanel.setStatusText(message);
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
                case KEY_CONFLICT: errorMessage = FLOW_STORE_PROXY_KEY_VIOLATION_ERROR_MESSAGE;
                    break;
                case DATA_NOT_ACCEPTABLE: errorMessage = FLOW_STORE_PROXY_DATA_VALIDATION_ERROR_MESSAGE;
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

    private class SubmitterDescriptionPanel extends HorizontalPanel {

        private final Label submitterDescriptionLabel = new Label("Beskrivelse");
        private final TextArea submitterDescriptionTextArea = new SubmitterDescriptionTextArea();

        public SubmitterDescriptionPanel() {
            add(submitterDescriptionLabel);
            getElement().setId("submitter-description-panel-id");
            add(submitterDescriptionTextArea);
        }

        public String getText() {
            return submitterDescriptionTextArea.getValue();
        }

        private class SubmitterDescriptionTextArea extends TextArea {

            public SubmitterDescriptionTextArea() {
                super();
                setCharacterWidth(40);
                setVisibleLines(4);
                getElement().setAttribute("Maxlength", String.valueOf(SUBMITTER_CREATION_DESCRIPTION_MAX_LENGTH));
                getElement().setId(GUIID_SUBMITTER_CREATION_DESCRIPTION_TEXT_AREA);
                addKeyDownHandler(new SubmitterCreateViewImpl.InputFieldKeyDownHandler());
            }
        }
    }

    private class SubmitterSavePanel extends HorizontalPanel {

        private final Button submitterSaveButton = new Button("Gem");
        private final Label submitterSaveResultLabel = new Label("");

        public SubmitterSavePanel() {
            submitterSaveResultLabel.getElement().setId(GUIID_SUBMITTER_CREATION_SAVE_RESULT_LABEL);
            add(submitterSaveResultLabel);
            getElement().setId("submitter-save-panel-id");
            submitterSaveButton.getElement().setId(GUIID_SUBMITTER_CREATION_SAVE_BUTTON);
            submitterSaveButton.addClickHandler(new SubmitterCreateViewImpl.SaveButtonHandler());
            add(submitterSaveButton);
        }

        public void setStatusText(String statusText) {
            submitterSaveResultLabel.setText(statusText);
        }
    }

    private class SaveButtonHandler implements ClickHandler {
        @Override
        public void onClick(ClickEvent event) {
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
            submitterSavePanel.setStatusText("");
        }
    }
}
