package dk.dbc.dataio.gui.client;

import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.VerticalPanel;

class FlowCreationWidget extends VerticalPanel {

    // Constants (These are not private since we use them in the selenium tests)
    final static String CONTEXT_HEADER = "Flow - opsætning";
    final static String GUIID_FLOW_CREATION_WIDGET = "flowcreationwidget";
    final static String GUIID_FLOW_CREATION_NAME_TEXT_BOX = "flowcreationnametextbox";
    final static String GUIID_FLOW_CREATION_DESCRIPTION_TEXT_AREA = "flowcreationdescriptiontextarea";
    final static String GUIID_FLOW_CREATION_SAVE_BUTTON = "flowcreationsavebutton";
    final static String GUIID_FLOW_CREATION_SAVE_RESULT_LABEL = "flowcreationsaveresultlabel";
    final static String SAVE_RESULT_LABEL_SUCCES_MESSAGE = "Tillykke du har gemt";
    private static final int FLOW_CREATION_DESCRIPTION_MAX_LENGTH = 160;
    static final String FLOW_CREATION_INPUT_FIELD_VALIDATION_ERROR = "Alle felter skal udfyldes.";
    // Local variables
    private final HorizontalPanel flowDescriptionPanel = new HorizontalPanel();
    private final Label flowDescriptionLabel = new Label("Skriv noget tekst. Max " + FLOW_CREATION_DESCRIPTION_MAX_LENGTH + " tegn");
    private final TextArea flowDescriptionTextArea = new TextArea();

    public FlowCreationWidget() {
        getElement().setId(GUIID_FLOW_CREATION_WIDGET);

        // Example of how a flow description text area could be set up
        setUpFlowDescriptionPanel();
        // This class extends VerticalPanel and can therefore use 
        // methods in a VerticalPanel directly:
        add(flowDescriptionPanel);
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
    }
}
