package dk.dbc.dataio.gui.client.components.prompted;

import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.uibinder.client.UiConstructor;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.HasValue;
import com.google.gwt.user.client.ui.TextBox;
import dk.dbc.dataio.gui.client.components.Tooltip;

public class PromptedTextBox extends PromptedData implements HasValue<String> {

    @UiField
    final TextBox textBox = new TextBox();


    /**
     * Default empty constructor
     */
    public PromptedTextBox() {
        this("");
    }

    /**
     * Constructor to be used in UI Binder
     *
     * @param prompt The prompt text
     */
    public @UiConstructor PromptedTextBox(String prompt) {
        super(prompt);
        textBox.addStyleName(PromptedData.PROMPTED_DATA_DATA_CLASS);
        add(textBox);
    }

    /**
     * Sets the Tool Tip to be used. Tool Tips will be shown, whenever the user hovers over the component
     *
     * @param toolTip The Tool Tip to be shown to the user
     */
    public void setToolTip(String toolTip) {
        if (!toolTip.isEmpty()) {
            new Tooltip(textBox, toolTip);
        }

    }

    /**
     * Sets the Max Length of the text box in number of characters that can be entered
     *
     * @param maxLength The Max Length of the text box
     */
    public void setMaxLength(String maxLength) {
        textBox.getElement().setAttribute("Maxlength", String.valueOf(maxLength));
    }

    /**
     * Gets the value of the entered text as a String
     *
     * @return The value of the entered text
     */
    @Override
    public String getValue() {
        return textBox.getValue();
    }

    /**
     * Sets the text in the Text box in the component
     *
     * @param value The text to be set in the textbox
     */
    @Override
    public void setValue(String value) {
        textBox.setValue(value, false);
    }

    /**
     * Sets the text in the Text box in the component
     *
     * @param value      The text to be set in the textbox
     * @param fireEvents If true, an event is fired to signal that the value has changed
     */
    @Override
    public void setValue(String value, boolean fireEvents) {
        textBox.setValue(value, fireEvents);
    }

    /**
     * Adds a {@link com.google.gwt.event.logical.shared.ValueChangeHandler} handler.
     *
     * @param handler the handler
     * @return the registration for the event
     */
    @Override
    public HandlerRegistration addValueChangeHandler(ValueChangeHandler<String> handler) {
        return textBox.addValueChangeHandler(handler);
    }

    /**
     * Clears the text in the textbox
     */
    public void clearText() {
        textBox.setText("");
    }

    /**
     * Sets the text in the textbox. No event will be fired.
     *
     * @param value The text to be set in the textbox
     */
    public void setText(String value) {
        textBox.setText(value);
    }

    /**
     * Gets the value of the entered text as a String
     *
     * @return The value of the entered text
     */
    public String getText() {
        return textBox.getText();
    }

    /**
     * Enables or disables the component
     *
     * @param enabled True: Enables the component, False: Disables the component
     */
    public void setEnabled(boolean enabled) {
        textBox.setEnabled(enabled);
    }

    /**
     * Tells, whether the component is enabled or disabled
     *
     * @return True: Enabled, False: Disabled
     */
    public boolean isEnabled() {
        return textBox.isEnabled();
    }

    /**
     * Sets the focus on this component
     *
     * @param focused If true, the component is focused, if false, the component is de-focused (blurred)
     */
    public void setFocus(boolean focused) {
        textBox.setFocus(focused);
    }

    /**
     * Adds a KeyDownHandler
     *
     * @param handler The KeyDownHandler
     * @return The HandlerRegistration object to be used to remove the handler
     */
    public HandlerRegistration addKeyDownHandler(KeyDownHandler handler) {
        return textBox.addKeyDownHandler(handler);
    }

    /**
     * Fires a Change event
     */
    public void fireChangeEvent() {
        class TextBoxChangedEvent extends ChangeEvent {
        }
        textBox.fireEvent(new TextBoxChangedEvent());
    }

    /**
     * Adds a ChangeHandler to this component
     *
     * @param handler The ChangeHandler
     * @return The HandlerRegistration object to be used to remove the handler
     */
    public HandlerRegistration addChangeHandler(ChangeHandler handler) {
        return textBox.addChangeHandler(handler);
    }

    /**
     * Adds a BlurHandler to this component
     *
     * @param handler The BlurHandler
     * @return The HandlerRegistration object to be used to remove the handler
     */
    public HandlerRegistration addBlurHandler(BlurHandler handler) {
        return textBox.addBlurHandler(handler);
    }
}
