package dk.dbc.dataio.gui.client.views;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import dk.dbc.dataio.gui.client.components.DualList;
import dk.dbc.dataio.gui.client.components.TextEntry;
import dk.dbc.dataio.gui.client.components.Tooltip;
import dk.dbc.dataio.gui.client.presenters.FlowbinderCreatePresenter;
import static dk.dbc.dataio.gui.client.views.SubmitterCreateViewImpl.GUIID_SUBMITTER_CREATION_NUMBER_PANEL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class FlowbinderCreateViewImpl extends VerticalPanel implements FlowbinderCreateView {

    // Constants (These are not all private since we use them in the selenium tests)
    public static final String CONTEXT_HEADER = "Flowbinder - opsætning";
    public static final int    FLOWBINDER_CREATION_NAME_MAX_LENGTH = 160;
    public static final int    FLOWBINDER_CREATION_DESCRIPTION_MAX_LENGTH = 160;
    public static final String FLOWBINDER_CREATION_INPUT_FIELD_VALIDATION_ERROR = "Alle felter skal udfyldes.";
    public static final String FLOWBINDER_CREATION_SAVE_SUCCESS = "Flowbinderen blev gemt.";
    public static final String FLOWBINDER_CREATION_FLOWBINDER_NAME_LABEL = "Flowbinder navn";
    public static final String FLOWBINDER_CREATION_FLOWBINDER_DESCRIPTION_LABEL = "Beskrivelse";
    public static final String FLOWBINDER_CREATION_FRAMEFORMAT_LABEL = "Rammeformat";
    public static final String FLOWBINDER_CREATION_CONTENTFORMAT_LABEL = "Indholdsformat";
    public static final String FLOWBINDER_CREATION_CHARACTERSET_LABEL = "Tegnsæt";
    public static final String FLOWBINDER_CREATION_SINK_LABEL = "Destination";
    public static final String FLOWBINDER_CREATION_RECORD_SPLITTER_LABEL = "Recordsplitter";
    public static final String FLOWBINDER_CREATION_SUBMITTERS_LABEL = "Submittere";
    public static final String FLOWBINDER_CREATION_FLOW_LABEL = "Flow";
    public static final String FLOWBINDER_CREATION_DEFAULT_RECORD_SPLITTER_LABEL = "Default Record Splitter";
    
    public static final String GUIID_FLOWBINDER_CREATION_WIDGET = "flowbindercreationwidget";
    public static final String GUIID_FLOWBINDER_CREATION_NAME_PANEL = "flowbindercreationnamepanel";
    public static final String GUIID_FLOWBINDER_CREATION_NAME_TEXT_BOX = "flowbindercreationnametextbox";
    public static final String GUIID_FLOWBINDER_CREATION_DESCRIPTION_PANEL = "flowbindercreationdescriptionpanel";
    public static final String GUIID_FLOWBINDER_CREATION_DESCRIPTION_TEXT_AREA = "flowbindercreationdescriptiontextarea";
    public static final String GUIID_FLOWBINDER_CREATION_FRAME_PANEL = "flowbindercreationframepanel";
    public static final String GUIID_FLOWBINDER_CREATION_FRAME_TEXT_BOX = "flowbindercreationframetextbox";
    public static final String GUIID_FLOWBINDER_CREATION_CONTENTFORMAT_PANEL = "flowbindercreationcontentformatpanel";
    public static final String GUIID_FLOWBINDER_CREATION_CONTENTFORMAT_TEXT_BOX = "flowbindercreationcontentformattextbox";
    public static final String GUIID_FLOWBINDER_CREATION_CHARACTER_SET_PANEL = "flowbindercreationcharactersetpanel";
    public static final String GUIID_FLOWBINDER_CREATION_CHARACTER_SET_TEXT_BOX = "flowbindercreationcharactersettextbox";
    public static final String GUIID_FLOWBINDER_CREATION_SINK_PANEL = "flowbindercreationsinkpanel";
    public static final String GUIID_FLOWBINDER_CREATION_SINK_TEXT_BOX = "flowbindercreationsinktextbox";
    public static final String GUIID_FLOWBINDER_CREATION_RECORD_SPLITTER_PANEL = "flowbindercreationrecordsplitterpanel";
    public static final String GUIID_FLOWBINDER_CREATION_RECORD_SPLITTER_TEXT_BOX = "flowbindercreationrecordsplittertextbox";
    public static final String GUIID_FLOWBINDER_CREATION_SUBMITTERS_SELECTION_PANEL = "flowbindercreationsubmittersduallist";
    public static final String GUIID_FLOWBINDER_CREATION_FLOW_PANEL = "flowbindercreationflowtextbox";
    public static final String GUIID_FLOWBINDER_CREATION_FLOW_LIST_BOX = "flowbindercreationflowlistbox";
    public static final String GUIID_FLOWBINDER_CREATION_SAVE_PANEL = "flowbindercreationsavepanel";
    public static final String GUIID_FLOWBINDER_CREATION_SAVE_BUTTON = "flowbindercreationsavebutton";
    public static final String GUIID_FLOWBINDER_CREATION_SAVE_RESULT_LABEL = "flowbindercreationsaveresultlabel";
    // Local variables
    private FlowbinderCreatePresenter presenter;
    private final TextEntry flowbinderNamePanel = new TextEntry(GUIID_FLOWBINDER_CREATION_NAME_PANEL, FLOWBINDER_CREATION_FLOWBINDER_NAME_LABEL, FLOWBINDER_CREATION_NAME_MAX_LENGTH);
    private final FlowbinderDescriptionPanel flowbinderDescriptionPanel = new FlowbinderDescriptionPanel();
    private final TextEntry flowbinderFramePanel = new TextEntry(GUIID_FLOWBINDER_CREATION_FRAME_PANEL, FLOWBINDER_CREATION_FRAMEFORMAT_LABEL);
    private final TextEntry flowbinderContentFormatPanel = new TextEntry(GUIID_FLOWBINDER_CREATION_CONTENTFORMAT_PANEL, FLOWBINDER_CREATION_CONTENTFORMAT_LABEL);
    private final TextEntry flowbinderCharacterSetPanel = new TextEntry(GUIID_FLOWBINDER_CREATION_CHARACTER_SET_PANEL, FLOWBINDER_CREATION_CHARACTERSET_LABEL);
    private final TextEntry flowbinderSinkPanel = new TextEntry(GUIID_FLOWBINDER_CREATION_SINK_PANEL, FLOWBINDER_CREATION_SINK_LABEL);
    private final TextEntry flowbinderRecordSplitterPanel = new TextEntry(GUIID_FLOWBINDER_CREATION_RECORD_SPLITTER_PANEL, FLOWBINDER_CREATION_RECORD_SPLITTER_LABEL);
    private final FlowbinderSubmittersPanel flowbinderSubmittersPanel = new FlowbinderSubmittersPanel();
    private final FlowbinderFlowPanel flowbinderFlowPanel = new FlowbinderFlowPanel();
    private final FlowbinderSavePanel flowbinderSavePanel = new FlowbinderSavePanel();

    public FlowbinderCreateViewImpl() {
        getElement().setId(GUIID_FLOWBINDER_CREATION_WIDGET);
        
        flowbinderNamePanel.addKeyDownHandler(new InputFieldKeyDownHandler());
        add(flowbinderNamePanel);
        
        add(flowbinderDescriptionPanel);
        
        flowbinderFramePanel.addKeyDownHandler(new InputFieldKeyDownHandler());
        add(flowbinderFramePanel);
        
        flowbinderContentFormatPanel.addKeyDownHandler(new InputFieldKeyDownHandler());
        add(flowbinderContentFormatPanel);
        
        flowbinderCharacterSetPanel.addKeyDownHandler(new InputFieldKeyDownHandler());
        add(flowbinderCharacterSetPanel);
        
        flowbinderSinkPanel.addKeyDownHandler(new InputFieldKeyDownHandler());
        add(flowbinderSinkPanel);
        
        flowbinderRecordSplitterPanel.addKeyDownHandler(new InputFieldKeyDownHandler());
        flowbinderRecordSplitterPanel.setText(FLOWBINDER_CREATION_DEFAULT_RECORD_SPLITTER_LABEL);
        flowbinderRecordSplitterPanel.setEnabled(false);
        add(flowbinderRecordSplitterPanel);
        
        add(flowbinderSubmittersPanel);
        add(flowbinderFlowPanel);
        add(flowbinderSavePanel);
    }

    
    /*
     * Implementation of interface methods
     */

    @Override
    public void setPresenter(FlowbinderCreatePresenter presenter) {
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
        flowbinderSavePanel.setStatusText(message);
    }

    @Override
    public void setAvailableFlows(Map<String, String> availableFlows) {
        flowbinderFlowPanel.clearFlows();
        for (String key: availableFlows.keySet()) {
           flowbinderFlowPanel.setAvailableFlow(key, availableFlows.get(key));
        }
    }

    @Override
    public void setAvailableSubmitters(Map<String, String> availableSubmitters) {
        flowbinderSubmittersPanel.clearAvailableSubmitters();
        flowbinderSubmittersPanel.clearItems();
        for (String key: availableSubmitters.keySet()) {
            flowbinderSubmittersPanel.addAvailableSubmitter(key, availableSubmitters.get(key));
        }
    }

    
    /*
     * Private methods
     */
    private void changeDetected() {
        flowbinderSavePanel.setStatusText("");  // If the user makes changes after a save, the status field shall be cleared
    }
    

   /*
    * Private classes
    */
    private class SaveButtonHandler implements ClickHandler {
        @Override
        public void onClick(ClickEvent event) {
            final String name = flowbinderNamePanel.getText();
            final String description = flowbinderDescriptionPanel.getText();
            final String packaging = flowbinderFramePanel.getText();
            final String format = flowbinderContentFormatPanel.getText();
            final String charset = flowbinderCharacterSetPanel.getText();
            final String destination = flowbinderSinkPanel.getText();
            final String recordSplitter = flowbinderRecordSplitterPanel.getText();
            final Collection<Map.Entry<String, String>> submittersFromView = flowbinderSubmittersPanel.getSelectedSubmitters();
            final List<String> submitters = new ArrayList<String>();
            final String flow = flowbinderFlowPanel.getSelectedFlow();
            final String validationError = validateFields(name, description, packaging, format, charset, destination, recordSplitter, submittersFromView, flow);
            if (!validationError.isEmpty()) {
                Window.alert(validationError);
            } else {
                for (Map.Entry<String, String> submitter: submittersFromView) {
                    submitters.add(submitter.getKey());
                }
                presenter.saveFlowbinder(name, description, packaging, format, charset, destination, recordSplitter, flow, submitters);
            }
        }
        private String validateFields(final String name, final String description, final String frameFormat, final String contentFormat, final String characterSet, final String sink, final String recordSplitter,
                final Collection<Map.Entry<String, String>> submitters, final String flow) {
            if (name.isEmpty() || description.isEmpty() || frameFormat.isEmpty() || contentFormat.isEmpty() || characterSet.isEmpty() || sink.isEmpty() || recordSplitter.isEmpty()
                    || submitters.isEmpty() || flow.isEmpty()) {
                return FLOWBINDER_CREATION_INPUT_FIELD_VALIDATION_ERROR;
            }
            return "";
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
    
    private class FlowbinderDescriptionPanel extends HorizontalPanel {
        private final FlowDescriptionTextArea flowDescriptionTextArea = new FlowDescriptionTextArea();
        public FlowbinderDescriptionPanel() {
            add(new Label(FLOWBINDER_CREATION_FLOWBINDER_DESCRIPTION_LABEL));
            getElement().setId(GUIID_FLOWBINDER_CREATION_DESCRIPTION_PANEL);
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
                getElement().setAttribute("Maxlength", String.valueOf(FLOWBINDER_CREATION_DESCRIPTION_MAX_LENGTH));
                getElement().setId(GUIID_FLOWBINDER_CREATION_DESCRIPTION_TEXT_AREA);
                addKeyDownHandler(new InputFieldKeyDownHandler());
            }
        }
    }

    private class FlowbinderSubmittersPanel extends HorizontalPanel {
        private final DualList submittersSelectionLists = new DualList();
        public FlowbinderSubmittersPanel() {
            add(new Label(FLOWBINDER_CREATION_SUBMITTERS_LABEL));
            getElement().setId(GUIID_FLOWBINDER_CREATION_SUBMITTERS_SELECTION_PANEL);
            add(submittersSelectionLists);
            submittersSelectionLists.addChangeHandler(new ChangeHandler() {
                @Override
                public void onChange(ChangeEvent event) {
                    changeDetected();
                }
            });
        }
        private void clearItems() {
            submittersSelectionLists.clear();
        }
        private void addAvailableSubmitter(String key, String value) {
            submittersSelectionLists.addAvailableItem(key, value);
        }
        private Collection<Map.Entry<String, String>> getSelectedSubmitters() {
            return submittersSelectionLists.getSelectedItems();
        }
        private void clearAvailableSubmitters() {
            submittersSelectionLists.clearAvailableItems();
        }
    }

    private class FlowbinderFlowPanel extends HorizontalPanel {
        private final ListBox flow = new ListBox();
        public FlowbinderFlowPanel() {
            super();
            add(new Label(FLOWBINDER_CREATION_FLOW_LABEL));
            getElement().setId(GUIID_FLOWBINDER_CREATION_FLOW_PANEL);
            flow.getElement().setId(GUIID_FLOWBINDER_CREATION_FLOW_LIST_BOX);
            add(flow);
            flow.addChangeHandler(new ChangeHandler() {
                @Override
                public void onChange(ChangeEvent event) {
                    changeDetected();
                }
            });
        }
        private void setAvailableFlow(String key, String flowName) {
            flow.addItem(flowName, key);
        }
        private String getSelectedFlow() {
            int selectedItemIndex = flow.getSelectedIndex();
            if (selectedItemIndex < 0) {
                return "";
            }
            return flow.getValue(selectedItemIndex);
        }
        private void clearFlows() {
            flow.clear();
        }
    }

    private class FlowbinderSavePanel extends HorizontalPanel {
        private final Button flowbinderSaveButton = new Button("Gem");
        private final Label flowbinderSaveResultLabel = new Label("");
        public FlowbinderSavePanel() {
            flowbinderSaveResultLabel.getElement().setId(GUIID_FLOWBINDER_CREATION_SAVE_RESULT_LABEL);
            add(flowbinderSaveResultLabel);
            getElement().setId(GUIID_FLOWBINDER_CREATION_SAVE_PANEL);
            flowbinderSaveButton.getElement().setId(GUIID_FLOWBINDER_CREATION_SAVE_BUTTON);
            flowbinderSaveButton.addClickHandler(new SaveButtonHandler());
            add(flowbinderSaveButton);
        }
        public void setStatusText(String statusText) {
            flowbinderSaveResultLabel.setText(statusText);
        }
    }

}
