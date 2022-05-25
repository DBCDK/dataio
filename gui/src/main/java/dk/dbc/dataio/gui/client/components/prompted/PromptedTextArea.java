package dk.dbc.dataio.gui.client.components.prompted;

import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.uibinder.client.UiConstructor;
import com.google.gwt.user.client.ui.HasValue;
import com.google.gwt.user.client.ui.TextArea;

public class PromptedTextArea extends PromptedData implements HasValue<String> {
    public static final int DEFAULT_CHARACTER_WIDTH = 40;
    public static final int DEFAULT_VISIBLE_LINES = 4;

    private final TextArea textArea = new TextArea();


    public PromptedTextArea(String guiId, String prompt, int maxLength) {
        this(guiId, prompt);
        textArea.getElement().setAttribute("Maxlength", String.valueOf(maxLength));
    }

    public PromptedTextArea(String guiId, String prompt) {
        super(guiId, prompt);
        setCharacterWidth(DEFAULT_CHARACTER_WIDTH);
        setVisibleLines(DEFAULT_VISIBLE_LINES);
        textArea.addStyleName(PromptedData.PROMPTED_DATA_DATA_CLASS);
        add(textArea);
    }

    public @UiConstructor PromptedTextArea(String guiId, String prompt, String maxLength) {
        this(guiId, prompt);
        if (!maxLength.isEmpty()) {
            textArea.getElement().setAttribute("Maxlength", maxLength);
        }
    }

    @Override
    public String getValue() {
        return textArea.getValue();
    }

    @Override
    public void setValue(String value) {
        textArea.setValue(value, false);
    }

    @Override
    public void setValue(String value, boolean fireEvents) {
        textArea.setValue(value, fireEvents);
    }

    /**
     * Adds a {@link com.google.gwt.event.logical.shared.ValueChangeHandler} handler.
     *
     * @param handler the handler
     * @return the registration for the event
     */
    @Override
    public HandlerRegistration addValueChangeHandler(ValueChangeHandler<String> handler) {
        return textArea.addValueChangeHandler(handler);
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

    public HandlerRegistration addBlurHandler(BlurHandler handler) {
        return textArea.addBlurHandler(handler);
    }

}
