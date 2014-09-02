package dk.dbc.dataio.gui.client.pages.submittermodify;

import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
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
    public static final String CLASS_FLOW_ELEMENT = "dio-flow-element";
    private static final int SUBMITTER_MODIFY_DESCRIPTION_MAX_LENGTH = 160;

    protected final TextEntry numberPanel;
    private final TextEntry namePanel;
    private final TextAreaEntry descriptionPanel;
    private final FlowPanel buttonPanel;
    private final Label statusText;
    private final Button saveButton;



    /**
     * Constructor
     */
    public ViewImpl(String header, SubmitterModifyConstants constants) {
        super(header);
        numberPanel = new TextEntry(GUIID_SUBMITTER_MODIFY_NUMBER_PANEL, constants.label_SubmitterNumber());
        namePanel = new TextEntry(GUIID_SUBMITTER_MODIFY_NAME_PANEL, constants.label_SubmitterName());
        descriptionPanel = new TextAreaEntry(GUIID_SUBMITTER_MODIFY_DESCRIPTION_PANEL, constants.label_Description(), SUBMITTER_MODIFY_DESCRIPTION_MAX_LENGTH);
        buttonPanel = new FlowPanel();
        statusText = new Label("");
        saveButton = new Button(constants.button_Save());
    }

    /**
     * Initializations of the view
     */
    public void init() {
        getElement().setId(GUIID_SUBMITTER_MODIFY_WIDGET);
        statusText.addStyleName(CLASS_FLOW_ELEMENT);
        saveButton.getElement().setId(GUIID_SUBMITTER_MODIFY_SAVE_BUTTON_PANEL);

        InputFieldKeyDownHandler inputFieldKeyDownHandler = new InputFieldKeyDownHandler();
        numberPanel.addKeyDownHandler(inputFieldKeyDownHandler);
        namePanel.addKeyDownHandler(inputFieldKeyDownHandler);
        descriptionPanel.addKeyDownHandler(inputFieldKeyDownHandler);

        numberPanel.addBlurHandler(new SubmitterNumberBlurHandler());
        namePanel.addBlurHandler(new SubmitterNameBlurHandler());
        descriptionPanel.addBlurHandler(new SubmitterDescriptionBlurHandler());

        saveButton.addClickHandler(new SaveButtonHandler());

        add(numberPanel);
        add(namePanel);
        add(descriptionPanel);
        buttonPanel.add(statusText);
        buttonPanel.add(saveButton);
        add(buttonPanel);
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
        statusText.setText(message);
    }

    /**
     * setNumber sets the number field in the form
     * @param number
     */
    @Override
    public void setNumber(String number) {
        numberPanel.setText(number);
    }

    /**
     * getNumber gets the number field from the form
     * @return the number from the form
     */
    @Override
    public String getNumber() {
        return numberPanel.getText();
    }

    /**
     * setName sets the name field in the form
     * @param name
     */
    @Override
    public void setName(String name) {
        namePanel.setText(name);
    }

    /**
     * getName gets the name field from the form
     * @return the name from the form
     */
    @Override
    public String getName() {
        return namePanel.getText();
    }

    /**
     * setDescription sets the description field in the form
     * @param description
     */
    @Override
    public void setDescription(String description) {
        descriptionPanel.setText(description);
    }

    /**
     * getDescription gets the description field from the form
     * @return the description from the form
     */
    @Override
    public String getDescription() {
        return descriptionPanel.getText();
    }



    /*
     * Implementation of Event Handling
     * Direction: View -> Presenter
     */


    private class SaveButtonHandler implements ClickHandler {
        @Override
        public void onClick(ClickEvent event) {
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
            presenter.numberChanged(numberPanel.getText());
        }
    }

    private class SubmitterNameBlurHandler implements BlurHandler {
        @Override
        public void onBlur(BlurEvent blurEvent) {
            presenter.nameChanged(namePanel.getText());
        }
    }

    private class SubmitterDescriptionBlurHandler implements BlurHandler {
        @Override
        public void onBlur(BlurEvent blurEvent) {
            presenter.descriptionChanged(descriptionPanel.getText());
        }
    }

}

