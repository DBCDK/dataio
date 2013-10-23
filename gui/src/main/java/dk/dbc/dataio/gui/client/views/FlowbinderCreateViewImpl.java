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
import com.google.gwt.user.client.ui.VerticalPanel;
import dk.dbc.dataio.gui.client.components.DualListEntry;
import dk.dbc.dataio.gui.client.components.ListEntry;
import dk.dbc.dataio.gui.client.components.TextAreaEntry;
import dk.dbc.dataio.gui.client.components.TextEntry;
import dk.dbc.dataio.gui.client.presenters.FlowbinderCreatePresenter;
import java.util.ArrayList;
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
    private final TextAreaEntry flowbinderDescriptionPanel = new TextAreaEntry(GUIID_FLOWBINDER_CREATION_DESCRIPTION_PANEL ,FLOWBINDER_CREATION_FLOWBINDER_DESCRIPTION_LABEL, FLOWBINDER_CREATION_DESCRIPTION_MAX_LENGTH);
    private final TextEntry flowbinderFramePanel = new TextEntry(GUIID_FLOWBINDER_CREATION_FRAME_PANEL, FLOWBINDER_CREATION_FRAMEFORMAT_LABEL);
    private final TextEntry flowbinderContentFormatPanel = new TextEntry(GUIID_FLOWBINDER_CREATION_CONTENTFORMAT_PANEL, FLOWBINDER_CREATION_CONTENTFORMAT_LABEL);
    private final TextEntry flowbinderCharacterSetPanel = new TextEntry(GUIID_FLOWBINDER_CREATION_CHARACTER_SET_PANEL, FLOWBINDER_CREATION_CHARACTERSET_LABEL);
    private final TextEntry flowbinderSinkPanel = new TextEntry(GUIID_FLOWBINDER_CREATION_SINK_PANEL, FLOWBINDER_CREATION_SINK_LABEL);
    private final TextEntry flowbinderRecordSplitterPanel = new TextEntry(GUIID_FLOWBINDER_CREATION_RECORD_SPLITTER_PANEL, FLOWBINDER_CREATION_RECORD_SPLITTER_LABEL);
    private final DualListEntry flowbinderSubmittersPanel = new DualListEntry(GUIID_FLOWBINDER_CREATION_SUBMITTERS_SELECTION_PANEL, FLOWBINDER_CREATION_SUBMITTERS_LABEL);
    private final ListEntry flowbinderFlowPanel = new ListEntry(GUIID_FLOWBINDER_CREATION_FLOW_PANEL, FLOWBINDER_CREATION_FLOW_LABEL);
    private final FlowbinderSavePanel flowbinderSavePanel = new FlowbinderSavePanel();

    public FlowbinderCreateViewImpl() {
        getElement().setId(GUIID_FLOWBINDER_CREATION_WIDGET);
        
        flowbinderNamePanel.addKeyDownHandler(new InputFieldKeyDownHandler());
        add(flowbinderNamePanel);
        
        flowbinderDescriptionPanel.addKeyDownHandler(new InputFieldKeyDownHandler());
        add(flowbinderDescriptionPanel);
        
        flowbinderFramePanel.addKeyDownHandler(new InputFieldKeyDownHandler());
        flowbinderFramePanel.addToolTip("Rammeformat: Teknisk formatprotokol til brug for udveksling af data. Eksempelvis dm2iso, dm2lin, xml, csv, m.v.");
        add(flowbinderFramePanel);
        
        flowbinderContentFormatPanel.addKeyDownHandler(new InputFieldKeyDownHandler());
        flowbinderContentFormatPanel.addToolTip("Indholdsformat: Bibliografisk format, f.eks. dbc, dfi, dkbilled, dsd, ebogsbib, ebrary, mv.");
        add(flowbinderContentFormatPanel);
        
        flowbinderCharacterSetPanel.addKeyDownHandler(new InputFieldKeyDownHandler());
        flowbinderCharacterSetPanel.addToolTip("Tegnsæt: F.eks. utf8, latin-1, samkat, m.v.");
        add(flowbinderCharacterSetPanel);
        
        flowbinderSinkPanel.addKeyDownHandler(new InputFieldKeyDownHandler());
        add(flowbinderSinkPanel);
        
        flowbinderRecordSplitterPanel.addKeyDownHandler(new InputFieldKeyDownHandler());
        flowbinderRecordSplitterPanel.setText(FLOWBINDER_CREATION_DEFAULT_RECORD_SPLITTER_LABEL);
        flowbinderRecordSplitterPanel.setEnabled(false);
        add(flowbinderRecordSplitterPanel);

        flowbinderSubmittersPanel.addChangeHandler(new ChangeHandler() {
            @Override
            public void onChange(ChangeEvent event) {
                changeDetected();
            }
        });
        add(flowbinderSubmittersPanel);

        flowbinderFlowPanel.setEnabled(false);
        flowbinderFlowPanel.addChangeHandler(new ChangeHandler() {
            @Override
            public void onChange(ChangeEvent event) {
                changeDetected();
            }
        });
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
        flowbinderFlowPanel.clear();
        if (!availableFlows.isEmpty()) {
            flowbinderFlowPanel.setEnabled(true);
            for (String key: availableFlows.keySet()) {
               flowbinderFlowPanel.setAvailableItem(availableFlows.get(key), key);
            }
        }
    }

    @Override
    public void setAvailableSubmitters(Map<String, String> availableSubmitters) {
        flowbinderSubmittersPanel.clear();
        if (!availableSubmitters.isEmpty()) {
            flowbinderSubmittersPanel.setEnabled(true);
            for (String key: availableSubmitters.keySet()) {
                flowbinderSubmittersPanel.addAvailableItem(availableSubmitters.get(key), key);
            }
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
            final Map<String, String> submittersFromView = flowbinderSubmittersPanel.getSelectedItems();
            final List<String> submitters = new ArrayList<String>();
            final String flow = flowbinderFlowPanel.getSelectedKey();
            final String validationError = validateFields(name, description, packaging, format, charset, destination, recordSplitter, submittersFromView, flow);
            if (!validationError.isEmpty()) {
                Window.alert(validationError);
            } else {
                for (String key: submittersFromView.keySet()) {
                    submitters.add(key);
                }
                presenter.saveFlowbinder(name, description, packaging, format, charset, destination, recordSplitter, flow, submitters);
            }
        }
        private String validateFields(final String name, final String description, final String frameFormat, final String contentFormat, final String characterSet, final String sink, final String recordSplitter,
                final Map<String, String> submitters, final String flow) {
            if (name.isEmpty() || description.isEmpty() || frameFormat.isEmpty() || contentFormat.isEmpty() || characterSet.isEmpty() || sink.isEmpty() || recordSplitter.isEmpty()
                    || (submitters == null) || submitters.isEmpty() || (flow == null) || flow.isEmpty()) {
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
