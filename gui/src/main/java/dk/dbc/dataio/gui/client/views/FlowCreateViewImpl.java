package dk.dbc.dataio.gui.client.views;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import dk.dbc.dataio.gui.client.components.DualListEntry;
import dk.dbc.dataio.gui.client.components.TextAreaEntry;
import dk.dbc.dataio.gui.client.components.TextEntry;
import dk.dbc.dataio.gui.client.presenters.FlowCreatePresenter;
import java.util.Collection;
import java.util.Map;



public class FlowCreateViewImpl extends FlowPanel implements FlowCreateView {
    // Constants (These are not all private since we use them in the selenium tests)
    public static final String CONTEXT_HEADER = "Flow - opsætning";
    public static final String FLOW_CREATION_FLOW_NAME_LABEL = "Flownavn";
    public static final String FLOW_CREATION_DESCRIPTION_LABEL = "Beskrivelse";
    public static final String FLOW_CREATION_FLOW_COMPONENTS_LABEL = "Flow komponenter";
    public static final String FLOW_CREATION_SAVE_BUTTON = "Gem";
    
    public static final String GUIID_FLOW_CREATION_WIDGET = "flowcreationwidget";
    public static final String GUIID_FLOW_CREATION_FLOW_NAME_PANEL = "flow-name-panel-id";
    
    public static final String GUIID_FLOW_CREATION_SAVE_BUTTON = "flowcreationsavebutton";
    public static final String GUIID_FLOW_CREATION_SAVE_RESULT_LABEL = "flowcreationsaveresultlabel";
    public static final String GUIID_FLOW_CREATION_FLOW_DESCRIPTION_PANEL = "flow-description-panel-id";
    public static final String GUIID_FLOW_CREATION_FLOW_COMPONENT_SELECTION_PANEL = "flow-component-selection-panel-id";
    public static final String GUIID_FLOW_CREATION_FLOW_SAVE_PANEL = "flow-save-panel-id";
    
    public static final String SAVE_RESULT_LABEL_SUCCES_MESSAGE = "Opsætningen blev gemt";
    public static final String FLOW_CREATION_INPUT_FIELD_VALIDATION_ERROR = "Alle felter skal udfyldes.";
    private static final int FLOW_CREATION_DESCRIPTION_MAX_LENGTH = 160;
    
    // Local variables
    private FlowCreatePresenter presenter;
    private final TextEntry flowNamePanel = new TextEntry(GUIID_FLOW_CREATION_FLOW_NAME_PANEL, FLOW_CREATION_FLOW_NAME_LABEL);
    private final TextAreaEntry flowDescriptionPanel = new TextAreaEntry(GUIID_FLOW_CREATION_FLOW_DESCRIPTION_PANEL, FLOW_CREATION_DESCRIPTION_LABEL, FLOW_CREATION_DESCRIPTION_MAX_LENGTH);
    private final DualListEntry flowComponentSelectionPanel = new DualListEntry(GUIID_FLOW_CREATION_FLOW_COMPONENT_SELECTION_PANEL, FLOW_CREATION_FLOW_COMPONENTS_LABEL);
    private final FlowSavePanel flowSavePanel = new FlowSavePanel();
    
    public FlowCreateViewImpl() {
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
        
        add(flowSavePanel);
    }

    
    /*
     * Implementation of interface methods
     */    
    @Override
    public void setPresenter(FlowCreatePresenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public void refresh() {
    }

    @Override
    public void onFailure(String message) {
        Window.alert("Error: " + message);
    }

    @Override
    public void onSuccess(String message) {
        flowSavePanel.setStatusText(message);
    }

    @Override
    public void setAvailableFlowComponents(Map<String, String> availableFlowComponents) {
        flowComponentSelectionPanel.clear();
        if (!availableFlowComponents.isEmpty()) {
            for (String key: availableFlowComponents.keySet()) {
               flowComponentSelectionPanel.addAvailableItem(availableFlowComponents.get(key), key);
            }
            flowComponentSelectionPanel.setEnabled(true);
        }
    }

    
    /*
     * Private methods
     */
    private void changeDetected() {
        flowSavePanel.setStatusText("");
    }

    private Collection<String> getSelectedFlowComponents() {
        return flowComponentSelectionPanel.getSelectedItems().keySet();
    }

    
   /*
    * Private classes
    */
    private class SaveButtonHandler implements ClickHandler {
        @Override
        public void onClick(ClickEvent event) {
            String nameValue = flowNamePanel.getText();
            String descriptionValue = flowDescriptionPanel.getText();
            if (!nameValue.isEmpty() && !descriptionValue.isEmpty() && (flowComponentSelectionPanel.getSelectedItemCount() > 0)) {
                presenter.saveFlow(flowNamePanel.getText(), flowDescriptionPanel.getText(), getSelectedFlowComponents());
            } else {
                Window.alert(FLOW_CREATION_INPUT_FIELD_VALIDATION_ERROR);
            }
        }
    }

    private class InputFieldKeyDownHandler implements KeyDownHandler {
        @Override
        public void onKeyDown(KeyDownEvent keyDownEvent) {
            changeDetected();
        }
    }

    
    /*
     * Panels
     */
    private class FlowSavePanel extends HorizontalPanel {
        private final Button flowSaveButton = new Button(FLOW_CREATION_SAVE_BUTTON);
        private final Label flowSaveResultLabel = new Label("");
        public FlowSavePanel() {
            getElement().setId(GUIID_FLOW_CREATION_FLOW_SAVE_PANEL);
            flowSaveResultLabel.getElement().setId(GUIID_FLOW_CREATION_SAVE_RESULT_LABEL);
            add(flowSaveResultLabel);
            flowSaveButton.getElement().setId(GUIID_FLOW_CREATION_SAVE_BUTTON);
            flowSaveButton.addClickHandler(new SaveButtonHandler());
            add(flowSaveButton);
        }
        public void setStatusText(String statusText) {
            flowSaveResultLabel.setText(statusText);
        }
    }

}
