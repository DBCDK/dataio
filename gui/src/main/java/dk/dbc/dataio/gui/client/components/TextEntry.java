package dk.dbc.dataio.gui.client.components;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.TextBox;

public class TextEntry extends DataEntry {

    private final TextBox textBox = new TextBox();

    
    public TextEntry(String guiId, String prompt, int maxLength) {
        this(guiId, prompt);
        textBox.getElement().setAttribute("Maxlength", String.valueOf(maxLength));
    }
    
    public TextEntry(String guiId, String prompt) {
        super(guiId, prompt);
        textBox.addStyleName(DataEntry.DATA_ENTRY_INPUT_BOX_CLASS);
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

    public void setEnabled(boolean enabled) {
        textBox.setEnabled(enabled);
    }

    public void addToolTip(String toolTipText) {
        new Tooltip(textBox, toolTipText);
    }
  
    public HandlerRegistration addKeyDownHandler(KeyDownHandler handler) {
        return textBox.addKeyDownHandler(handler);
    }

    public void fireChangeEvent() {
        class TextBoxChangedEvent extends ChangeEvent {}
        textBox.fireEvent(new TextBoxChangedEvent());
    }
    
    public HandlerRegistration addChangeHandler(ChangeHandler handler) {
        return textBox.addChangeHandler(handler);
    }
}
