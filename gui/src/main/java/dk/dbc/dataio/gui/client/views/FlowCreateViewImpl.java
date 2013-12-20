package dk.dbc.dataio.gui.client.views;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.user.client.Window;
import dk.dbc.dataio.gui.client.components.DualListEntry;
import dk.dbc.dataio.gui.client.components.SaveButton;
import dk.dbc.dataio.gui.client.components.TextAreaEntry;
import dk.dbc.dataio.gui.client.components.TextEntry;
import dk.dbc.dataio.gui.client.i18n.FlowCreateConstants;
import dk.dbc.dataio.gui.client.presenters.FlowCreatePresenter;
import java.util.Collection;
import java.util.Map;



public class FlowCreateViewImpl extends ContentPanel<FlowCreatePresenter> implements FlowCreateView {
    // Constants (These are not all private since we use them in the selenium tests)
    public static final String GUIID_FLOW_CREATION_WIDGET = "flowcreationwidget";
    public static final String GUIID_FLOW_CREATION_FLOW_NAME_PANEL = "flow-name-panel-id";
    public static final String GUIID_FLOW_CREATION_FLOW_DESCRIPTION_PANEL = "flow-description-panel-id";
    public static final String GUIID_FLOW_CREATION_FLOW_COMPONENT_SELECTION_PANEL = "flow-component-selection-panel-id";
    public static final String GUIID_FLOW_CREATION_FLOW_SAVE_PANEL = "flow-save-panel-id";

    private static final int FLOW_CREATION_DESCRIPTION_MAX_LENGTH = 160;

    // Local variables
    private final FlowCreateConstants constants = GWT.create(FlowCreateConstants.class);
    private final TextEntry flowNamePanel = new TextEntry(GUIID_FLOW_CREATION_FLOW_NAME_PANEL, constants.label_FlowName());
    private final TextAreaEntry flowDescriptionPanel = new TextAreaEntry(GUIID_FLOW_CREATION_FLOW_DESCRIPTION_PANEL, constants.label_Description(), FLOW_CREATION_DESCRIPTION_MAX_LENGTH);
    private final DualListEntry flowComponentSelectionPanel = new DualListEntry(GUIID_FLOW_CREATION_FLOW_COMPONENT_SELECTION_PANEL, constants.label_FlowComponents());
    private final SaveButton saveButton = new SaveButton(GUIID_FLOW_CREATION_FLOW_SAVE_PANEL, constants.button_Save(), new SaveButtonEvent());


    /**
     * Constructor
     */
    public FlowCreateViewImpl() {
        super(mainConstants.subMenu_FlowCreation());
    }


    /**
     * Initializations of the view
     */
    public void init() {
        Window.alert("init");
        getElement().setId(GUIID_FLOW_CREATION_WIDGET);

        flowNamePanel.addKeyDownHandler(new InputFieldKeyDownHandler());
        add(flowNamePanel);

        flowDescriptionPanel.addKeyDownHandler(new InputFieldKeyDownHandler());
        add(flowDescriptionPanel);

        flowComponentSelectionPanel.addChangeHandler(new ChangeHandler() {
            @Override
            public void onChange(ChangeEvent event) {
                changeDetected();
            }
        });
        add(flowComponentSelectionPanel);

        add(saveButton);
    }

    /*
     * Implementation of interface methods
     */
    @Override
    public void refresh() {
        Window.alert("Så er refresh kaldt");
    }

    @Override
    public void onSuccess(String message) {
        saveButton.setStatusText(message);
    }

    @Override
    public void setAvailableFlowComponents(Map<String, String> availableFlowComponents) {
        flowComponentSelectionPanel.clear();
        if (!availableFlowComponents.isEmpty()) {
            for(Map.Entry<String, String> entry : availableFlowComponents.entrySet()) {
               flowComponentSelectionPanel.addAvailableItem(entry.getValue(), entry.getKey());
            }
            flowComponentSelectionPanel.setEnabled(true);
        }
    }


    /*
     * Private methods
     */
    private void changeDetected() {
        saveButton.setStatusText("");
    }

    private Collection<String> getSelectedFlowComponents() {
        return flowComponentSelectionPanel.getSelectedItems().keySet();
    }


   /*
    * Private classes
    */

    private class SaveButtonEvent implements SaveButton.ButtonEvent {
        @Override
        public void buttonPressed() {
            String nameValue = flowNamePanel.getText();
            String descriptionValue = flowDescriptionPanel.getText();
            if (!nameValue.isEmpty() && !descriptionValue.isEmpty() && flowComponentSelectionPanel.getSelectedItemCount() > 0) {
                presenter.saveFlow(flowNamePanel.getText(), flowDescriptionPanel.getText(), getSelectedFlowComponents());
            } else {
                Window.alert(constants.error_InputFieldValidationError());
            }
        }
    }

    private class InputFieldKeyDownHandler implements KeyDownHandler {
        @Override
        public void onKeyDown(KeyDownEvent keyDownEvent) {
            changeDetected();
        }
    }

}
