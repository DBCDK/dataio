package dk.dbc.dataio.gui.client.components;

import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;

/**
 *
 */
public class DataEntry extends HorizontalPanel {
    public static final String DATA_ENTRY_PROMPT_LABEL_CLASS = "dio-DataEntry-PromptLabelClass";

    public DataEntry(String guiId, String prompt) {
        super();
        getElement().setId(guiId);
        Label promptLabel = new Label(prompt);
        promptLabel.setStylePrimaryName(DATA_ENTRY_PROMPT_LABEL_CLASS);
        add(promptLabel);
    }

}
