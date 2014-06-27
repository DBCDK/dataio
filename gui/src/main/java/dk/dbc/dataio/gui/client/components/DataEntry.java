package dk.dbc.dataio.gui.client.components;

import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;

/**
 *
 */
public class DataEntry extends FlowPanel {
    public static final String DATA_ENTRY_CLASS = "dio-DataEntry";
    public static final String DATA_ENTRY_PROMPT_LABEL_CLASS = "dio-DataEntry-PromptLabelClass";
    public static final String DATA_ENTRY_INPUT_BOX_CLASS = "dio-DataEntry-InputBoxClass";

    public DataEntry(String guiId, String prompt) {
        super();
        getElement().setId(guiId);
        setStylePrimaryName(DATA_ENTRY_CLASS);
        Label promptLabel = new Label(prompt);
        promptLabel.setStylePrimaryName(DATA_ENTRY_PROMPT_LABEL_CLASS);
        add(promptLabel);
    }

}
