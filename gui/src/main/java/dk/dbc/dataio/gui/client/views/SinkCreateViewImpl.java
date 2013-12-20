package dk.dbc.dataio.gui.client.views;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.user.client.Window;
import dk.dbc.dataio.gui.client.components.SaveButton;
import dk.dbc.dataio.gui.client.components.TextEntry;
import dk.dbc.dataio.gui.client.exceptions.FlowStoreProxyError;
import dk.dbc.dataio.gui.client.i18n.SinkCreateConstants;
import dk.dbc.dataio.gui.client.presenters.SinkCreatePresenter;


/**
 *
 * This is the implementation of the Sink Creation View
 *
 */
public class SinkCreateViewImpl extends ContentPanel<SinkCreatePresenter> implements SinkCreateView {
    // Constants (These are not all private since we use them in the selenium tests)
    public static final String GUIID_SINK_CREATION_WIDGET = "sinkcreationwidget";
    public static final String GUIID_SINK_CREATION_SINK_NAME_PANEL = "sinkcreationsinknamepanel";
    public static final String GUIID_SINK_CREATION_RESOURCE_NAME_PANEL = "sinkcreationresourcenamepanel";
    public static final String GUIID_SINK_CREATION_SAVE_BUTTON_PANEL = "sinkcreationsavebuttonpanel";

    // Local variables
    private final SinkCreateConstants constants = GWT.create(SinkCreateConstants.class);
    private final TextEntry sinkNamePanel = new TextEntry(GUIID_SINK_CREATION_SINK_NAME_PANEL, constants.label_SinkName());
    private final TextEntry resourceNamePanel = new TextEntry(GUIID_SINK_CREATION_RESOURCE_NAME_PANEL, constants.label_ResourceName());
    private final SaveButton saveButton = new SaveButton(GUIID_SINK_CREATION_SAVE_BUTTON_PANEL, constants.button_Save(), new SaveButtonEvent());


    /**
     * Constructor
     */
    public SinkCreateViewImpl() {
        super(mainConstants.subMenu_SinkCreation());
    }


    /**
     * Initializations of the view
     */
    public void init() {
        getElement().setId(GUIID_SINK_CREATION_WIDGET);

        sinkNamePanel.addKeyDownHandler(new InputFieldKeyDownHandler());
        add(sinkNamePanel);

        resourceNamePanel.addKeyDownHandler(new InputFieldKeyDownHandler());
        add(resourceNamePanel);

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
     * OnSuccess
     * @param message The message to display to the user
     */
    @Override
    public void onSuccess(String message) {
        saveButton.setStatusText(message);
    }

    /**
     * This method is called by the presenter, when signalling an error the the user
     * @param errorCode The error code
     * @param detail Details for the error
     */
    @Override
    public void onFlowStoreProxyFailure(FlowStoreProxyError errorCode, String detail) {
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
     * This method is called by the presenter, when signalling a successfull save to the user
     */
    @Override
    public void onSaveSinkSuccess() {
        onSuccess(constants.status_SinkSuccessfullySaved());
    }


   /*
    * Private classes
    */

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
                return constants.error_InputFieldValidationError();
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
