package dk.dbc.dataio.gui.client.pages.flowbinder.modify;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.user.client.Window;
import dk.dbc.dataio.gui.client.components.DualListEntry;
import dk.dbc.dataio.gui.client.components.ListEntry;
import dk.dbc.dataio.gui.client.components.SaveButton;
import dk.dbc.dataio.gui.client.components.TextAreaEntry;
import dk.dbc.dataio.gui.client.components.TextEntry;
import dk.dbc.dataio.gui.client.views.ContentPanel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *
 * This is the implementation of the Flow Binder Creation View
 *
 */
public class FlowbinderCreateViewImpl extends ContentPanel<FlowbinderCreatePresenter> implements FlowbinderCreateView {
    public static final int    FLOWBINDER_CREATION_NAME_MAX_LENGTH = 160;
    public static final int    FLOWBINDER_CREATION_DESCRIPTION_MAX_LENGTH = 160;
    public static final String GUIID_FLOWBINDER_CREATION_WIDGET = "flowbindercreationwidget";
    public static final String GUIID_FLOWBINDER_CREATION_NAME_PANEL = "flowbindercreationnamepanel";
    public static final String GUIID_FLOWBINDER_CREATION_DESCRIPTION_PANEL = "flowbindercreationdescriptionpanel";
    public static final String GUIID_FLOWBINDER_CREATION_FRAME_PANEL = "flowbindercreationframepanel";
    public static final String GUIID_FLOWBINDER_CREATION_CONTENTFORMAT_PANEL = "flowbindercreationcontentformatpanel";
    public static final String GUIID_FLOWBINDER_CREATION_CHARACTER_SET_PANEL = "flowbindercreationcharactersetpanel";
    public static final String GUIID_FLOWBINDER_CREATION_DESTINATION_PANEL = "flowbindercreationdestinationpanel";
    public static final String GUIID_FLOWBINDER_CREATION_RECORD_SPLITTER_PANEL = "flowbindercreationrecordsplitterpanel";
    public static final String GUIID_FLOWBINDER_CREATION_SUBMITTERS_SELECTION_PANEL = "flowbindercreationsubmittersduallist";
    public static final String GUIID_FLOWBINDER_CREATION_FLOW_PANEL = "flowbindercreationflowpanel";
    public static final String GUIID_FLOWBINDER_CREATION_SINK_PANEL = "flowbindercreationsinkpanel";
    public static final String GUIID_FLOWBINDER_CREATION_SAVE_PANEL = "flowbindercreationsavepanel";

    // Local variables
    private final static FlowbinderCreateTexts constants = GWT.create(FlowbinderCreateTexts.class);
    private final TextEntry flowbinderNamePanel = new TextEntry(GUIID_FLOWBINDER_CREATION_NAME_PANEL, constants.label_FlowBinderName(), FLOWBINDER_CREATION_NAME_MAX_LENGTH);
    private final TextAreaEntry flowbinderDescriptionPanel = new TextAreaEntry(GUIID_FLOWBINDER_CREATION_DESCRIPTION_PANEL, constants.label_FlowBinderDescription(), FLOWBINDER_CREATION_DESCRIPTION_MAX_LENGTH);
    private final TextEntry flowbinderFramePanel = new TextEntry(GUIID_FLOWBINDER_CREATION_FRAME_PANEL, constants.label_FrameFormat());
    private final TextEntry flowbinderContentFormatPanel = new TextEntry(GUIID_FLOWBINDER_CREATION_CONTENTFORMAT_PANEL, constants.label_ContentFormat());
    private final TextEntry flowbinderCharacterSetPanel = new TextEntry(GUIID_FLOWBINDER_CREATION_CHARACTER_SET_PANEL, constants.label_CharacterSet());
    private final TextEntry flowbinderDestinationPanel = new TextEntry(GUIID_FLOWBINDER_CREATION_DESTINATION_PANEL, constants.label_Destination());
    private final TextEntry flowbinderRecordSplitterPanel = new TextEntry(GUIID_FLOWBINDER_CREATION_RECORD_SPLITTER_PANEL, constants.label_RecordSplitter());
    private final DualListEntry flowbinderSubmittersPanel = new DualListEntry(GUIID_FLOWBINDER_CREATION_SUBMITTERS_SELECTION_PANEL, constants.label_Submitters());
    private final ListEntry flowbinderFlowPanel = new ListEntry(GUIID_FLOWBINDER_CREATION_FLOW_PANEL, constants.label_Flow());
    private final ListEntry flowbinderSinkPanel = new ListEntry(GUIID_FLOWBINDER_CREATION_SINK_PANEL, constants.label_Sink());
    private final SaveButton saveButton = new SaveButton(GUIID_FLOWBINDER_CREATION_SAVE_PANEL, constants.button_Save(), new SaveButtonEvent());


    /**
     * Constructor
     */
    public FlowbinderCreateViewImpl() {
        super(constants.menu_FlowbinderCreation());
    }


    /**
     * Initializations of the view
     */
    public void init() {
        getElement().setId(GUIID_FLOWBINDER_CREATION_WIDGET);

        flowbinderNamePanel.addKeyDownHandler(new InputFieldKeyDownHandler());
        add(flowbinderNamePanel);

        flowbinderDescriptionPanel.addKeyDownHandler(new InputFieldKeyDownHandler());
        add(flowbinderDescriptionPanel);

        flowbinderFramePanel.addKeyDownHandler(new InputFieldKeyDownHandler());
        flowbinderFramePanel.addToolTip(constants.tooltip_FrameFormat());
        add(flowbinderFramePanel);

        flowbinderContentFormatPanel.addKeyDownHandler(new InputFieldKeyDownHandler());
        flowbinderContentFormatPanel.addToolTip(constants.tooltip_ContentFormat());
        add(flowbinderContentFormatPanel);

        flowbinderCharacterSetPanel.addKeyDownHandler(new InputFieldKeyDownHandler());
        flowbinderCharacterSetPanel.addToolTip(constants.tooltip_CharacterSet());
        add(flowbinderCharacterSetPanel);

        flowbinderDestinationPanel.addKeyDownHandler(new InputFieldKeyDownHandler());
        add(flowbinderDestinationPanel);

        flowbinderRecordSplitterPanel.addKeyDownHandler(new InputFieldKeyDownHandler());
        flowbinderRecordSplitterPanel.setText(constants.label_DefaultRecordSplitter());
        flowbinderRecordSplitterPanel.setEnabled(false);
        add(flowbinderRecordSplitterPanel);

        flowbinderSubmittersPanel.addChangeHandler(new SomethingHasChanged());
        add(flowbinderSubmittersPanel);

        flowbinderFlowPanel.setEnabled(false);
        flowbinderFlowPanel.addChangeHandler(new SomethingHasChanged());
        add(flowbinderFlowPanel);

        flowbinderSinkPanel.setEnabled(false);
        flowbinderSinkPanel.addChangeHandler(new SomethingHasChanged());
        add(flowbinderSinkPanel);

        add(saveButton);
    }


    /*
     * Implementation of interface methods
     */

    /**
     * Clear all fields in this view
     */
    @Override
    public void clearFields() {
        flowbinderNamePanel.clearText();
        flowbinderDescriptionPanel.clearText();
        flowbinderFramePanel.clearText();
        flowbinderContentFormatPanel.clearText();
        flowbinderCharacterSetPanel.clearText();
        flowbinderDestinationPanel.clearText();
//        flowbinderRecordSplitterPanel.clearText();
        flowbinderSubmittersPanel.clear();
        flowbinderFlowPanel.clear();
        flowbinderSinkPanel.clear();
    }

    /**
     * OnSuccess
     * @param message The message to display to the user
     */
    @Override
    public void setStatusText(String message) {
        saveButton.setStatusText(message);
    }

    /**
     * This method is called by the presenter, when pushing Flows to the view
     * @param availableFlows The flows to display
     */
    @Override
    public void setAvailableFlows(Map<String, String> availableFlows) {
        flowbinderFlowPanel.clear();
        if (!availableFlows.isEmpty()) {
            flowbinderFlowPanel.setEnabled(true);
            for(Map.Entry<String, String> entry : availableFlows.entrySet()) {
                flowbinderFlowPanel.setAvailableItem(entry.getValue(), entry.getKey());
            }
        }
    }

    /**
     * This method is called by the presenter, when pushing Submitters to the view
     * @param availableSubmitters The submitters to display
     */
    @Override
    public void setAvailableSubmitters(Map<String, String> availableSubmitters) {
        flowbinderSubmittersPanel.clear();
        if (!availableSubmitters.isEmpty()) {
            flowbinderSubmittersPanel.setEnabled(true);
            for(Map.Entry<String, String> entry : availableSubmitters.entrySet()) {
                flowbinderSubmittersPanel.addAvailableItem(entry.getValue(), entry.getKey());
            }
        }
    }

    /**
     * This method is called by the presenter, when pushing Sinks to the view
     * @param availableSinks The sinks to display
     */
    @Override
    public void setAvailableSinks(Map<String, String> availableSinks) {
        flowbinderSinkPanel.clear();
        if (!availableSinks.isEmpty()) {
            flowbinderSinkPanel.setEnabled(true);
            for(Map.Entry<String, String> entry : availableSinks.entrySet()) {
               flowbinderSinkPanel.setAvailableItem(entry.getValue(), entry.getKey());
            }
        }
    }


    /*
     * Private methods
     */
    private void changeDetected() {
        saveButton.setStatusText("");  // If the user makes changes after a save, the status field shall be cleared
    }


   /*
    * Private classes
    */
    private class SaveButtonEvent implements SaveButton.ButtonEvent {
        @Override
        public void buttonPressed() {
            final String name = flowbinderNamePanel.getText();
            final String description = flowbinderDescriptionPanel.getText();
            final String packaging = flowbinderFramePanel.getText();
            final String format = flowbinderContentFormatPanel.getText();
            final String charset = flowbinderCharacterSetPanel.getText();
            final String destination = flowbinderDestinationPanel.getText();
            final String recordSplitter = flowbinderRecordSplitterPanel.getText();
            final String sink = flowbinderSinkPanel.getSelectedKey();
            final Map<String, String> submittersFromView = flowbinderSubmittersPanel.getSelectedItems();
            final List<String> submitters = new ArrayList<String>();
            final String flow = flowbinderFlowPanel.getSelectedKey();
            final String validationError = validateFields(name, description, packaging, format, charset, destination, recordSplitter, submittersFromView, flow, sink);
            if (!validationError.isEmpty()) {
                Window.alert(validationError);
            } else {
                for (String key: submittersFromView.keySet()) {
                    submitters.add(key);
                }
                presenter.saveFlowbinder(name, description, packaging, format, charset, destination, recordSplitter, flow, submitters, sink);
            }
        }
        private String validateFields(final String name, final String description, final String frameFormat, final String contentFormat, final String characterSet, final String destination, final String recordSplitter,
                final Map<String, String> submitters, final String flow, final String sink) {
            if (name.isEmpty() ||
                description.isEmpty() ||
                frameFormat.isEmpty() ||
                contentFormat.isEmpty() ||
                characterSet.isEmpty() ||
                destination==null ||
                destination.isEmpty() ||
                recordSplitter.isEmpty() ||
                submitters == null ||
                submitters.isEmpty() ||
                flow == null ||
                flow.isEmpty() ||
                sink == null ||
                sink.isEmpty()) {
                return constants.error_InputFieldValidationError();
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

    private class SomethingHasChanged implements ChangeHandler {
        @Override
        public void onChange(ChangeEvent event) {
            changeDetected();
        }

    }

}
