package dk.dbc.dataio.gui.client.components;

import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;

/**
 *
 */
public class DataEntry extends HorizontalPanel {
    public static final String DATA_ENTRY_COMPONENT_CLASS = "dio-DataEntry";
    public static final String DATA_ENTRY_PROMPT_LABEL_CLASS = "dio-DataEntry-PromptLabelClass";

    public DataEntry(String guiId, String prompt) {
        super();
        getElement().setId(guiId);
        setStyleName(DATA_ENTRY_COMPONENT_CLASS);
        Label promptLabel = new Label(prompt);
        promptLabel.setStyleName(DATA_ENTRY_PROMPT_LABEL_CLASS);
        add(promptLabel);
    }

}
