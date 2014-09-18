package dk.dbc.dataio.gui.client.pages.flow.modify;

import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import dk.dbc.dataio.gui.client.components.DualListEntry;
import dk.dbc.dataio.gui.client.components.TextAreaEntry;
import dk.dbc.dataio.gui.client.components.TextEntry;
import dk.dbc.dataio.gui.client.views.ContentPanel;

import java.util.Map;

/**
 *
 * The implementation of the Flow Modify View
 *
 */
public class ViewImpl extends ContentPanel<Presenter> implements View {
    public static final String GUIID_FLOW_CREATION_WIDGET = "flowcreationwidget";
    public static final String GUIID_FLOW_CREATION_FLOW_NAME_PANEL = "flow-name-panel-id";
    public static final String GUIID_FLOW_CREATION_FLOW_DESCRIPTION_PANEL = "flow-description-panel-id";
    public static final String GUIID_FLOW_CREATION_FLOW_COMPONENT_SELECTION_PANEL = "flow-component-selection-panel-id";
    public static final String GUIID_FLOW_MODIFY_SAVE_BUTTON = "flowmodifysavebutton";
    public static final String CLASS_FLOW_ELEMENT = "dio-flow-element";
    private static final int FLOW_CREATION_DESCRIPTION_MAX_LENGTH = 160;

    private final TextEntry flowNamePanel;
    private final TextAreaEntry flowDescriptionPanel;
    private final DualListEntry flowComponentSelectionPanel;
    private final FlowPanel buttonPanel;
    private final Label statusText;
    private final Button saveButton;

    /**
     * Constructor
     */
    public ViewImpl(String header, Texts texts) {
        super(header);
        flowNamePanel = new TextEntry(GUIID_FLOW_CREATION_FLOW_NAME_PANEL, texts.label_FlowName());
        flowDescriptionPanel = new TextAreaEntry(GUIID_FLOW_CREATION_FLOW_DESCRIPTION_PANEL, texts.label_Description(), FLOW_CREATION_DESCRIPTION_MAX_LENGTH);
        flowComponentSelectionPanel = new DualListEntry(GUIID_FLOW_CREATION_FLOW_COMPONENT_SELECTION_PANEL, texts.label_FlowComponents());
        buttonPanel = new FlowPanel();
        statusText = new Label("");
        saveButton = new Button(texts.button_Save());
    }

    /**
     * Initializations of the view
     */
    public void init() {
        getElement().setId(GUIID_FLOW_CREATION_WIDGET);
        statusText.addStyleName(CLASS_FLOW_ELEMENT);
        saveButton.getElement().setId(GUIID_FLOW_MODIFY_SAVE_BUTTON);

        InputFieldKeyDownHandler inputFieldKeyDownHandler = new InputFieldKeyDownHandler();
        flowNamePanel.addKeyDownHandler(inputFieldKeyDownHandler);
        flowDescriptionPanel.addKeyDownHandler(inputFieldKeyDownHandler);
        flowComponentSelectionPanel.addChangeHandler(new FlowComponentSelectionChangeHandler());
        flowNamePanel.addBlurHandler(new FlowNameBlurHandler());
        flowDescriptionPanel.addBlurHandler(new DescriptionBlurHandler());
        saveButton.addClickHandler(new SaveButtonHandler());

        add(flowNamePanel);
        add(flowDescriptionPanel);
        add(flowComponentSelectionPanel);
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
     * setName sets the name field in the form
     * @param name, the name to set
     */
    @Override
    public void setName(String name) {
        flowNamePanel.setText(name);
    }

    /**
     * getName gets the name field from the form
     * @return the name from the form
     */
    @Override
    public String getName() {
        return flowNamePanel.getText();
    }

    /**
     * setName sets the description field in the form
     * @param description, the description to set
     */
    @Override
    public void setDescription(String description) {
        flowDescriptionPanel.setText(description);
    }

    /**
     * getName gets the description field from the form
     * @return the description from the form
     */
    @Override
    public String getDescription() {
        return flowDescriptionPanel.getText();
    }

    /**
     * setName sets the map of available flow components in the form
     * @param availableFlowComponents, the map of available flow components to set
     */
    @Override
    public void setAvailableFlowComponents(Map<String, String> availableFlowComponents) {
        flowComponentSelectionPanel.setAvailableItems(availableFlowComponents);
        flowComponentSelectionPanel.setEnabled(true);
    }

    /**
     * setName sets the map of selected flow components in the form
     * @param selectedFlowComponents, the map of selected flow components to set
     */
    @Override
    public void setSelectedFlowComponents(Map<String, String> selectedFlowComponents) {
        flowComponentSelectionPanel.setSelectedItems(selectedFlowComponents);
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

    private class FlowComponentSelectionChangeHandler implements ChangeHandler {
        @Override
        public void onChange(ChangeEvent event) {
            presenter.keyPressed();
            presenter.flowComponentsChanged(flowComponentSelectionPanel.getSelectedItems());
        }
    }

    private class FlowNameBlurHandler implements BlurHandler {
        @Override
        public void onBlur(BlurEvent blurEvent) {
            presenter.nameChanged(flowNamePanel.getText());
        }
    }

    private class DescriptionBlurHandler implements BlurHandler {
        @Override
        public void onBlur(BlurEvent blurEvent) {
            presenter.descriptionChanged(flowDescriptionPanel.getText());
        }
    }

}
