package dk.dbc.dataio.gui.client.components;

import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;

public class TextEntry extends HorizontalPanel {
    public static final String TEXT_ENTRY_COMPONENT_CLASS = "dio-TextEntry";
    public static final String TEXT_ENTRY_PROMPT_LABEL_CLASS = "dio-TextEntry-PromptLabelClass";
    public static final String TEXT_ENTRY_TEXT_BOX_CLASS = "dio-TextEntry-TextBoxClass";

    private final TextBox textBox = new TextBox();

    
    public TextEntry(String prompt, int maxLength) {
        this(prompt);
        textBox.getElement().setAttribute("Maxlength", String.valueOf(maxLength));
    }
    
    public TextEntry(String prompt) {
        super();
        Label promptLabel = new Label(prompt);
        setStyleName(TEXT_ENTRY_COMPONENT_CLASS);
        promptLabel.setStyleName(TEXT_ENTRY_PROMPT_LABEL_CLASS);
        add(promptLabel);
        textBox.setStyleName(TEXT_ENTRY_TEXT_BOX_CLASS);
        add(textBox);
    }
    
    public void clearText() {
        textBox.setText("");
    }
    
    public void setText(String value) {
        textBox.setText(value);
    }
    
    public String getText() {
        return textBox.getText();
    }

    public HandlerRegistration addKeyDownHandler(KeyDownHandler handler) {
        return textBox.addKeyDownHandler(handler);
    }
    
    public HandlerRegistration addChangeHandler(ChangeHandler handler) {
        return textBox.addChangeHandler(handler);
    }
  
}
