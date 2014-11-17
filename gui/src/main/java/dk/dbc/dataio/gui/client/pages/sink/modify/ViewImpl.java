package dk.dbc.dataio.gui.client.pages.sink.modify;

import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import dk.dbc.dataio.gui.client.components.TextEntry;
import dk.dbc.dataio.gui.client.views.ContentPanel;

/**
 *
 * The implementation of the Sink Modify View
 *
 */
public class ViewImpl extends ContentPanel<Presenter> implements ViewOld {

    public static final String GUIID_SINK_MODIFY_WIDGET = "sinkmodifywidget";
    public static final String GUIID_SINK_MODIFY_SINK_NAME_PANEL = "sinkmodifysinknamepanel";
    public static final String GUIID_SINK_MODIFY_RESOURCE_NAME_PANEL = "sinkmodifyresourcenamepanel";
    public static final String GUIID_SINK_MODIFY_SAVE_BUTTON_PANEL = "sinkmodifysavebuttonpanel";
    public static final String CLASS_FLOW_ELEMENT = "dio-flow-element";

    private final TextEntry namePanel;
    private final TextEntry resourcePanel;
    private final FlowPanel buttonPanel;
    private final Label statusText;
    private final Button saveButton;

    /**
     * Constructor
     */
    public ViewImpl(String header, Texts texts) {
        super(header);
        namePanel = new TextEntry(GUIID_SINK_MODIFY_SINK_NAME_PANEL, texts.label_SinkName());
        resourcePanel = new TextEntry(GUIID_SINK_MODIFY_RESOURCE_NAME_PANEL, texts.label_ResourceName());
        buttonPanel = new FlowPanel();
        statusText = new Label("");
        saveButton = new Button(texts.button_Save());
    }

    /**
     * Initializations of the view
     */
    public void init() {
        getElement().setId(GUIID_SINK_MODIFY_WIDGET);
        statusText.addStyleName(CLASS_FLOW_ELEMENT);
        saveButton.getElement().setId(GUIID_SINK_MODIFY_SAVE_BUTTON_PANEL);

        InputFieldKeyDownHandler inputFieldKeyDownHandler = new InputFieldKeyDownHandler();
        namePanel.addKeyDownHandler(inputFieldKeyDownHandler);
        resourcePanel.addKeyDownHandler(inputFieldKeyDownHandler);

        namePanel.addBlurHandler(new SinkNameBlurHandler());
        resourcePanel.addBlurHandler(new ResourceNameBlurHandler());

        saveButton.addClickHandler(new SaveButtonHandler());

        add(namePanel);
        add(resourcePanel);
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
     * initializeFields initializes and disables all fields in the view
     */
    @Override
    public void initializeFields() {
        namePanel.clearText();
        namePanel.setEnabled(false);
        resourcePanel.clearText();
        resourcePanel.setEnabled(false);
    }

    /**
     * setName sets the name field in the form
     * @param name, the name to set
     */
    @Override
    public void setName(String name) {
        namePanel.setText(name);
        namePanel.setEnabled(true);
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
     * setResource sets the resource field in the form
     * @param resource, the resource to set
     */
    @Override
    public void setResource(String resource) {
        resourcePanel.setText(resource);
        resourcePanel.setEnabled(true);
    }

    /**
     * getResource gets the resource field from the form
     * @return the resource from the form
     */
    @Override
    public String getResource() {
        return resourcePanel.getText();
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

    private class SinkNameBlurHandler implements BlurHandler {
        @Override
        public void onBlur(BlurEvent blurEvent) {
            presenter.nameChanged(namePanel.getText());
        }
    }

    private class ResourceNameBlurHandler implements BlurHandler {
        @Override
        public void onBlur(BlurEvent blurEvent) {
            presenter.resourceChanged(resourcePanel.getText());
        }
    }
}
