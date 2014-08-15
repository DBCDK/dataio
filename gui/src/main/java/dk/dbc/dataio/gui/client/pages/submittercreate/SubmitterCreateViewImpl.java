package dk.dbc.dataio.gui.client.pages.submittercreate;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.user.client.Window;
import dk.dbc.dataio.gui.client.components.SaveButton;
import dk.dbc.dataio.gui.client.components.TextAreaEntry;
import dk.dbc.dataio.gui.client.components.TextEntry;
import dk.dbc.dataio.gui.client.exceptions.ProxyError;
import dk.dbc.dataio.gui.client.views.ContentPanel;


/**
 *
 * This is the implementation of the Submitter Creation View
 *
 */
public class SubmitterCreateViewImpl extends ContentPanel<SubmitterCreatePresenter>  implements SubmitterCreateView {
    public static final String GUIID_SUBMITTER_CREATION_WIDGET = "submittercreationwidget";
    public static final String GUIID_SUBMITTER_CREATION_NUMBER_PANEL = "submittercreationnumberpanel";
    public static final String GUIID_SUBMITTER_CREATION_NAME_PANEL = "submittercreationnamepanel";
    public static final String GUIID_SUBMITTER_CREATION_DESCRIPTION_PANEL = "submittercreationdescriptionpanel";
    public static final String GUIID_SUBMITTER_CREATION_SAVE_BUTTON_PANEL = "submittercreationsavebuttonpanel";
    private static final int SUBMITTER_CREATION_DESCRIPTION_MAX_LENGTH = 160;

    // Local variables
    private final static SubmitterCreateConstants constants = GWT.create(SubmitterCreateConstants.class);
    private final TextEntry submitterNumberPanel = new TextEntry(GUIID_SUBMITTER_CREATION_NUMBER_PANEL, constants.label_SubmitterNumber());
    private final TextEntry submitterNamePanel = new TextEntry(GUIID_SUBMITTER_CREATION_NAME_PANEL, constants.label_SubmitterName());
    private final TextAreaEntry submitterDescriptionPanel = new TextAreaEntry(GUIID_SUBMITTER_CREATION_DESCRIPTION_PANEL, constants.label_Description(), SUBMITTER_CREATION_DESCRIPTION_MAX_LENGTH);
    private final SaveButton saveButton = new SaveButton(GUIID_SUBMITTER_CREATION_SAVE_BUTTON_PANEL, constants.button_Save(), new SaveButtonEvent());


    /**
     * Constructor
     */
    public SubmitterCreateViewImpl() {
        super(constants.menu_SubmitterCreation());
    }

    /**
     * Initializations of the view
     */
    public void init() {
        getElement().setId(GUIID_SUBMITTER_CREATION_WIDGET);

        submitterNumberPanel.addKeyDownHandler(new InputFieldKeyDownHandler());
        add(submitterNumberPanel);

        submitterNamePanel.addKeyDownHandler(new InputFieldKeyDownHandler());
        add(submitterNamePanel);

        submitterDescriptionPanel.addKeyDownHandler(new InputFieldKeyDownHandler());
        add(submitterDescriptionPanel);

        add(saveButton);
    }


    /*
     * Implementation of interface methods
     */

    /**
     * Refresh
     */
    @Override
    public void refresh() {
    }

    /**
     * Clear all fields in this view
     */
    @Override
    public void clearFields() {
        submitterNumberPanel.clearText();
        submitterNamePanel.clearText();
        submitterDescriptionPanel.clearText();
    }

    /**
     * OnSuccess
     * @param message The message to display to the user
     */
    @Override
    public void onSuccess(String message) {
        saveButton.setStatusText(message);
    }

    /**
     * This method is called by the presenter, when detecting an erroe in the Flow Store Proxy
     * @param errorCode The error code
     * @param detail Details about the error
     */
    @Override
    public void onFlowStoreProxyFailure(ProxyError errorCode, String detail) {
        final String errorMessage;
        if (errorCode == null) {
            errorMessage = detail;
        } else {
            switch (errorCode) {
                case NOT_ACCEPTABLE: errorMessage = constants.error_ProxyKeyViolationError();
                    break;
                case BAD_REQUEST: errorMessage = constants.error_ProxyDataValidationError();
                    break;
                default: errorMessage = detail;
                    break;
            }
        }
        onFailure(errorMessage);
    }

    /**
     * This method is called by the presenter, when a successful save has been done
     */
    @Override
    public void onSaveSubmitterSuccess() {
        onSuccess(constants.status_SubmitterSuccessfullySaved());
    }


    /*
     * Private Classes
     */
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
                return constants.error_InputFieldValidationError();
            }
            try {
                Long.valueOf(numberValue);
            } catch (NumberFormatException e) {
                return constants.error_NumberInputFieldValidationError();
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
