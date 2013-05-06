/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package dk.dbc.dataio.gui.views;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import dk.dbc.dataio.gui.client.proxy.FlowStoreProxy;
import dk.dbc.dataio.gui.client.proxy.FlowStoreProxyAsync;


/**
 *
 * @author slf
 */
public class FlowEditViewImpl extends VerticalPanel implements FlowEditView {
    // Constants (These are not private since we use them in the selenium tests)
    public final static String CONTEXT_HEADER = "Flow - opsætning";
    public final static String GUIID_FLOW_CREATION_WIDGET = "flowcreationwidget";
    public final static String GUIID_FLOW_CREATION_NAME_TEXT_BOX = "flowcreationnametextbox";
    public final static String GUIID_FLOW_CREATION_DESCRIPTION_TEXT_AREA = "flowcreationdescriptiontextarea";
    public final static String GUIID_FLOW_CREATION_SAVE_BUTTON = "flowcreationsavebutton";
    public final static String GUIID_FLOW_CREATION_SAVE_RESULT_LABEL = "flowcreationsaveresultlabel";
    public final static String SAVE_RESULT_LABEL_SUCCES_MESSAGE = "Opsætningen blev gemt";
    private static final int FLOW_CREATION_DESCRIPTION_MAX_LENGTH = 160;
    public static final String FLOW_CREATION_INPUT_FIELD_VALIDATION_ERROR = "Alle felter skal udfyldes.";
    
    // Local variables
    private Presenter presenter;
    private final HorizontalPanel flowNamePanel = new HorizontalPanel();
    private final Label flowNameLabel = new Label("Flownavn");
    private final TextBox flowNameTextBox = new TextBox();
    private final HorizontalPanel flowDescriptionPanel = new HorizontalPanel();
    private final Label flowDescriptionLabel = new Label("Beskrivelse");
    private final TextArea flowDescriptionTextArea = new TextArea();
    private final HorizontalPanel flowSavePanel = new HorizontalPanel();
    private final Button flowSaveButton = new Button("Gem");
    private final Label flowSaveResultLabel = new Label("");

    private FlowStoreProxyAsync flowStoreProxy = FlowStoreProxy.Factory.getAsyncInstance();
    
    public FlowEditViewImpl() {
        getElement().setId(GUIID_FLOW_CREATION_WIDGET);

        // Example of how a flow description text area could be set up
        setUpFlowNamePanel();
        setUpFlowDescriptionPanel();
        setUpFlowSavePanel();
        // This class extends VerticalPanel and can therefore use 
        // methods in a VerticalPanel directly:
        add(flowNamePanel);
        add(flowDescriptionPanel);
        add(flowSavePanel);
    }

    private void setUpFlowNamePanel() {
        flowNamePanel.add(flowNameLabel);
        flowNameTextBox.getElement().setId(GUIID_FLOW_CREATION_NAME_TEXT_BOX);
        flowNameTextBox.addKeyDownHandler(new InputFieldKeyDownHandler());
        flowNamePanel.add(flowNameTextBox);
    }

    private void setUpFlowDescriptionPanel() {
        flowDescriptionPanel.add(flowDescriptionLabel);
        setUpFlowDescriptionTextArea();
        flowDescriptionPanel.add(flowDescriptionTextArea);
    }

    private void setUpFlowDescriptionTextArea() {
        flowDescriptionTextArea.setCharacterWidth(40);
        flowDescriptionTextArea.setVisibleLines(4);
        // MaxLength is an attribute on a textarea, and can be set with setAttribute():
        flowDescriptionTextArea.getElement().setAttribute("Maxlength", String.valueOf(FLOW_CREATION_DESCRIPTION_MAX_LENGTH));
        flowDescriptionTextArea.getElement().setId(GUIID_FLOW_CREATION_DESCRIPTION_TEXT_AREA);
        flowDescriptionTextArea.addKeyDownHandler(new InputFieldKeyDownHandler());
    }

    private void setUpFlowSavePanel() {
        flowSaveResultLabel.getElement().setId(GUIID_FLOW_CREATION_SAVE_RESULT_LABEL);
        flowSavePanel.add(flowSaveResultLabel);
        flowSaveButton.getElement().setId(GUIID_FLOW_CREATION_SAVE_BUTTON);
        flowSaveButton.addClickHandler(new SaveButtonHandler());
        flowSavePanel.add(flowSaveButton);
    }

    private class SaveButtonHandler implements ClickHandler {

        @Override
        public void onClick(ClickEvent event) {
            String nameValue = flowNameTextBox.getValue();
            String descriptionValue = flowDescriptionTextArea.getValue();
            if(!nameValue.isEmpty() && !descriptionValue.isEmpty()) {
                presenter.saveFlow(flowNameTextBox.getValue(), flowDescriptionTextArea.getValue());
            } else {
                Window.alert(FLOW_CREATION_INPUT_FIELD_VALIDATION_ERROR);
            }

        }
    }

    private class InputFieldKeyDownHandler implements KeyDownHandler {
        @Override
        public void onKeyDown(KeyDownEvent keyDownEvent) {
            flowSaveResultLabel.setText("");
        }
    }
    
    @Override
    public void setPresenter(Presenter presenter) {
		this.presenter = presenter;
    }

    @Override
    public void setData(String name, String description) {
        // set data
    }

    @Override
    public void displayError(String message) {
        Window.alert("Error: " + message);
    }

    @Override
    public void displaySuccess(String message) {
        flowSaveResultLabel.setText(message);
    }

    public void refresh() {

    }
    
}
