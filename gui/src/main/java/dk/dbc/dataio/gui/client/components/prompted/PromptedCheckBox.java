package dk.dbc.dataio.gui.client.components.prompted;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.uibinder.client.UiConstructor;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HasValue;

public class PromptedCheckBox extends PromptedData implements HasValue<Boolean> {

    @UiField
    final CheckBox checkBox = new CheckBox();


    /**
     * UiBinder Constructor
     *
     * @param guiId  The GUI Id to be set in the DOM
     * @param prompt The prompt text
     */
    public @UiConstructor PromptedCheckBox(String guiId, String prompt) {
        super(guiId, prompt);
        checkBox.addStyleName(PromptedData.PROMPTED_DATA_DATA_CLASS);
        add(checkBox);
    }

    /**
     * Gets the current boolean value for the CheckBox
     *
     * @return The current value: True if selected, False if not
     */
    @Override
    public Boolean getValue() {
        return checkBox.getValue();
    }

    /**
     * Sets the value of the CheckBox
     *
     * @param value The value to set for the CheckBox: True if selected, False if not
     */
    @Override
    public void setValue(Boolean value) {
        checkBox.setValue(value, false);
    }

    /**
     * Sets the value of the CheckBox, and fires an event, if the fireEvents parameter is set
     *
     * @param value      The value to set for the CheckBox: True if selected, False if not
     * @param fireEvents Fires an event, if set
     */
    @Override
    public void setValue(Boolean value, boolean fireEvents) {
        checkBox.setValue(value, fireEvents);
    }

    /**
     * Adds a {@link com.google.gwt.event.logical.shared.ValueChangeHandler} handler.
     *
     * @param handler the handler
     * @return the registration for the event
     */
    @Override
    public HandlerRegistration addValueChangeHandler(ValueChangeHandler<Boolean> handler) {
        return checkBox.addValueChangeHandler(handler);
    }

    /**
     * Enables or disables the CheckBox
     *
     * @param enabled If true, the CheckBox is enabled, if false, the CheckBox is disabled
     */
    public void setEnabled(boolean enabled) {
        checkBox.setEnabled(enabled);
    }

    /**
     * Fires a change event
     */
    public void fireChangeEvent() {
        class TextBoxChangedEvent extends ChangeEvent {
        }
        checkBox.fireEvent(new TextBoxChangedEvent());
    }

}
