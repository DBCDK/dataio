package dk.dbc.dataio.gui.client.components;

import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;

/**
 *
 */
public class PromptedData extends FlowPanel {
    public static final String PROMPTED_DATA_CLASS = "dio-PromptedData";
    public static final String PROMPTED_DATA_PROMPT_LABEL_CLASS = "dio-PromptedData-PromptLabelClass";
    public static final String PROMPTED_DATA_INPUT_BOX_CLASS = "dio-PromptedData-InputBoxClass";

    public PromptedData(String guiId, String prompt) {
        super();
        getElement().setId(guiId);
        setStylePrimaryName(PROMPTED_DATA_CLASS);
        Label promptLabel = new Label(prompt);
        promptLabel.setStylePrimaryName(PROMPTED_DATA_PROMPT_LABEL_CLASS);
        add(promptLabel);
    }

}
