package dk.dbc.dataio.gui.client.pages.submittermodify;

import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import dk.dbc.dataio.gui.client.components.SaveButton;
import dk.dbc.dataio.gui.client.components.TextAreaEntry;
import dk.dbc.dataio.gui.client.components.TextEntry;
import dk.dbc.dataio.gui.client.views.ContentPanel;


/**
 *
 * This is the implementation of the Submitter Modify View
 *
 */
public class ViewImpl extends ContentPanel<Presenter>  implements View {
    public static final String GUIID_SUBMITTER_MODIFY_WIDGET = "submittermodifywidget";
    public static final String GUIID_SUBMITTER_MODIFY_NUMBER_PANEL = "submittermodifynumberpanel";
    public static final String GUIID_SUBMITTER_MODIFY_NAME_PANEL = "submittermodifynamepanel";
    public static final String GUIID_SUBMITTER_MODIFY_DESCRIPTION_PANEL = "submittermodifydescriptionpanel";
    public static final String GUIID_SUBMITTER_MODIFY_SAVE_BUTTON_PANEL = "submittermodifysavebuttonpanel";
    private static final int SUBMITTER_MODIFY_DESCRIPTION_MAX_LENGTH = 160;

    // Local variables
    private final TextEntry submitterNumberPanel;
    private final TextEntry submitterNamePanel;
    private final TextAreaEntry submitterDescriptionPanel;
    private final SaveButton saveButton;



    /**
     * Constructor
     */
    public ViewImpl(String header, SubmitterModifyConstants constants) {
        super(header);
        submitterNumberPanel = new TextEntry(GUIID_SUBMITTER_MODIFY_NUMBER_PANEL, constants.label_SubmitterNumber());
        submitterNamePanel = new TextEntry(GUIID_SUBMITTER_MODIFY_NAME_PANEL, constants.label_SubmitterName());
        submitterDescriptionPanel = new TextAreaEntry(GUIID_SUBMITTER_MODIFY_DESCRIPTION_PANEL, constants.label_Description(), SUBMITTER_MODIFY_DESCRIPTION_MAX_LENGTH);
        saveButton = new SaveButton(GUIID_SUBMITTER_MODIFY_SAVE_BUTTON_PANEL, constants.button_Save(), new SaveButtonEvent());
    }

    /**
     * Initializations of the view
     */
    public void init() {
        getElement().setId(GUIID_SUBMITTER_MODIFY_WIDGET);

        submitterNumberPanel.addKeyDownHandler(new InputFieldKeyDownHandler());
        submitterNamePanel.addKeyDownHandler(new InputFieldKeyDownHandler());
        submitterDescriptionPanel.addKeyDownHandler(new InputFieldKeyDownHandler());

        submitterNumberPanel.addBlurHandler(new SubmitterNumberBlurHandler());
        submitterNamePanel.addBlurHandler(new SubmitterNameBlurHandler());
        submitterDescriptionPanel.addBlurHandler(new SubmitterDescriptionBlurHandler());

        add(submitterNumberPanel);
        add(submitterNamePanel);
        add(submitterDescriptionPanel);
        add(saveButton);
    }


    /*
     * Implementation of interface methods - Setters and Getters
     * Direction: Presenter -> View
     */


    /**
     * setStatusText
     * @param message The message to display to the user
     */
    @Override
    public void setStatusText(String message) {
        saveButton.setStatusText(message);
    }

    /**
     * setNumber sets the number field in the form
     * @param number
     */
    @Override
    public void setNumber(String number) {
        submitterNumberPanel.setText(number);
    }

    /**
     * getNumber gets the number field from the form
     * @return the number from the form
     */
    @Override
    public String getNumber() {
        return submitterNumberPanel.getText();
    }

    /**
     * setName sets the name field in the form
     * @param name
     */
    @Override
    public void setName(String name) {
        submitterNamePanel.setText(name);
    }

    /**
     * getName gets the name field from the form
     * @return the name from the form
     */
    @Override
    public String getName() {
        return submitterNamePanel.getText();
    }

    /**
     * setDescription sets the description field in the form
     * @param description
     */
    @Override
    public void setDescription(String description) {
        submitterDescriptionPanel.setText(description);
    }

    /**
     * getDescription gets the description field from the form
     * @return the description from the form
     */
    @Override
    public String getDescription() {
        return submitterDescriptionPanel.getText();
    }



    /*
     * Implementation of Event Handling
     * Direction: View -> Presenter
     */

    private class SaveButtonEvent implements SaveButton.ButtonEvent {
        @Override
        public void buttonPressed() {
            presenter.saveButtonPressed();
        }
    }

    private class InputFieldKeyDownHandler implements KeyDownHandler {
        @Override
        public void onKeyDown(KeyDownEvent keyDownEvent) {
            presenter.keyPressed();
        }
    }

    private class SubmitterNumberBlurHandler implements BlurHandler {
        @Override
        public void onBlur(BlurEvent blurEvent) {
            presenter.numberChanged(submitterNumberPanel.getText());
        }
    }

    private class SubmitterNameBlurHandler implements BlurHandler {
        @Override
        public void onBlur(BlurEvent blurEvent) {
            presenter.nameChanged(submitterNamePanel.getText());
        }
    }

    private class SubmitterDescriptionBlurHandler implements BlurHandler {
        @Override
        public void onBlur(BlurEvent blurEvent) {
            presenter.descriptionChanged(submitterDescriptionPanel.getText());
        }
    }

}

