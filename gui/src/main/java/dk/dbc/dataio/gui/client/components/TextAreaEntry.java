package dk.dbc.dataio.gui.client.components;

import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.TextArea;

public class TextAreaEntry extends DataEntry {
    public static final String TEXT_AREA_ENTRY_TEXT_BOX_CLASS = "dio-TextAreaEntry-TextAreaClass";
    public static final int DEFAULT_CHARACTER_WIDTH = 40;
    public static final int DEFAULT_VISIBLE_LINES = 4;
    
    private final TextArea textArea = new TextArea();

    
    public TextAreaEntry(String guiId, String prompt, int maxLength) {
        this(guiId, prompt);
        textArea.getElement().setAttribute("Maxlength", String.valueOf(maxLength));
    }
    
    public TextAreaEntry(String guiId, String prompt) {
        super(guiId, prompt);
        setCharacterWidth(DEFAULT_CHARACTER_WIDTH);
        setVisibleLines(DEFAULT_VISIBLE_LINES);
        textArea.setStylePrimaryName(TEXT_AREA_ENTRY_TEXT_BOX_CLASS);
        add(textArea);
    }
    
    public void clearText() {
        textArea.setText("");
    }
    
    public void setText(String value) {
        textArea.setText(value);
    }
    
    public String getText() {
        return textArea.getText();
    }

    public void setCharacterWidth(int characterWidth) {
        textArea.setCharacterWidth(characterWidth);
    }
    
    public void setVisibleLines(int visibleLines) {
        textArea.setVisibleLines(visibleLines);
    }
    
    public void setEnabled(boolean enabled) {
        textArea.setEnabled(enabled);
    }
  
    public HandlerRegistration addKeyDownHandler(KeyDownHandler handler) {
        return textArea.addKeyDownHandler(handler);
    }
    
    public HandlerRegistration addChangeHandler(ChangeHandler handler) {
        return textArea.addChangeHandler(handler);
    }

}
