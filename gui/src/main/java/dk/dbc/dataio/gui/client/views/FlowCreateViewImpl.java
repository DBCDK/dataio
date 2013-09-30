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
import com.google.gwt.user.client.ui.TextArea;
import dk.dbc.dataio.gui.client.components.DualList;
import dk.dbc.dataio.gui.client.components.TextEntry;
import dk.dbc.dataio.gui.client.presenters.FlowCreatePresenter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;


public class FlowCreateViewImpl extends FlowPanel implements FlowCreateView {
    // Constants (These are not all private since we use them in the selenium tests)
    public static final String CONTEXT_HEADER = "Flow - opsætning";
    public static final String GUIID_FLOW_CREATION_WIDGET = "flowcreationwidget";
    public static final String GUIID_FLOW_CREATION_FLOW_NAME_PANEL = "flow-name-panel-id";
    
    public static final String GUIID_FLOW_CREATION_DESCRIPTION_TEXT_AREA = "flowcreationdescriptiontextarea";
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
    private final TextEntry<String> flowNamePanel = new TextEntry<String>("Flownavn");

    private final FlowDescriptionPanel flowDescriptionPanel = new FlowDescriptionPanel();
    private final FlowComponentSelectionPanel flowComponentSelectionPanel = new FlowComponentSelectionPanel();
    private final FlowSavePanel flowSavePanel = new FlowSavePanel();
    
    public FlowCreateViewImpl() {
        getElement().setId(GUIID_FLOW_CREATION_WIDGET);
        flowNamePanel.getElement().setId(GUIID_FLOW_CREATION_FLOW_NAME_PANEL);
        add(flowNamePanel);
        flowNamePanel.addKeyDownHandler(new InputFieldKeyDownHandler());

        add(flowDescriptionPanel);
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
        flowComponentSelectionPanel.clearItems();
        for (String key: availableFlowComponents.keySet()) {
           flowComponentSelectionPanel.addAvailableItem(key, availableFlowComponents.get(key));
        }
    }

    
    /*
     * Private methods
     */
    private void changeDetected() {
        flowSavePanel.setStatusText("");
    }

    private List<String> getSelectedFlowComponents() {
        List<String> selectedFlowComponents = new ArrayList<String>();
        Collection<Map.Entry<String, String>> selectedFlowComponentsFromPanel = flowComponentSelectionPanel.getSelectedItems();
        for (Map.Entry<String, String> item : selectedFlowComponentsFromPanel) {
            selectedFlowComponents.add(item.getKey());
        }
        return selectedFlowComponents;
    }

    
   /*
    * Private classes
    */
    private class SaveButtonHandler implements ClickHandler {
        @Override
        public void onClick(ClickEvent event) {
            String nameValue = flowNamePanel.getValue();
            String descriptionValue = flowDescriptionPanel.getText();
            if (!nameValue.isEmpty() && !descriptionValue.isEmpty() && (flowComponentSelectionPanel.size()>0)) {
                presenter.saveFlow(flowNamePanel.getValue(), flowDescriptionPanel.getText(), getSelectedFlowComponents());
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
    private class FlowDescriptionPanel extends HorizontalPanel {
        private final TextArea flowDescriptionTextArea = new FlowDescriptionTextArea();
        public FlowDescriptionPanel() {
            add(new Label("Beskrivelse"));
            getElement().setId(GUIID_FLOW_CREATION_FLOW_DESCRIPTION_PANEL);
            add(flowDescriptionTextArea);
        }
        public String getText() {
            return flowDescriptionTextArea.getValue();
        }
        private class FlowDescriptionTextArea extends TextArea {
            public FlowDescriptionTextArea() {
                super();
                setCharacterWidth(40);
                setVisibleLines(4);
                getElement().setAttribute("Maxlength", String.valueOf(FLOW_CREATION_DESCRIPTION_MAX_LENGTH));
                getElement().setId(GUIID_FLOW_CREATION_DESCRIPTION_TEXT_AREA);
                addKeyDownHandler(new InputFieldKeyDownHandler());
            }
        }
    }
    
    private class FlowComponentSelectionPanel extends HorizontalPanel {
        private final DualList flowComponentSelectionLists = new DualList();
        public FlowComponentSelectionPanel() {
            add(new Label("Flow komponenter"));
            getElement().setId(GUIID_FLOW_CREATION_FLOW_COMPONENT_SELECTION_PANEL);
            add(flowComponentSelectionLists);
            flowComponentSelectionLists.addChangeHandler(new ChangeHandler() {
                @Override
                public void onChange(ChangeEvent event) {
                    changeDetected();
                }
            });
        }
        private int size() {
            return flowComponentSelectionLists.getSelectedItems().size();
        }
        private void clearItems() {
            flowComponentSelectionLists.clear();
        }
        private void addAvailableItem(String key, String value) {
            flowComponentSelectionLists.addAvailableItem(key, value);
        }
        private Collection<Map.Entry<String, String>> getSelectedItems() {
            return flowComponentSelectionLists.getSelectedItems();
        }
    }
        
    private class FlowSavePanel extends HorizontalPanel {
        private final Button flowSaveButton = new Button("Gem");
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
