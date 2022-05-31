package dk.dbc.dataio.gui.client.components.prompted;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.uibinder.client.UiConstructor;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.HasValue;
import dk.dbc.dataio.gui.client.components.DateTimeBox;


public class PromptedDateTimeBox extends PromptedData implements HasValue<String> {

    @UiField
    final DateTimeBox dateTimeBox = new DateTimeBox();


    /**
     * UiBinder Constructor
     *
     * @param guiId  The GUI Id to be set in the DOM
     * @param prompt The prompt text
     */
    public @UiConstructor PromptedDateTimeBox(String guiId, String prompt) {
        super(guiId, prompt);
        dateTimeBox.addStyleName(PromptedData.PROMPTED_DATA_DATA_CLASS);
        add(dateTimeBox);
    }

    /**
     * Gets the current boolean value for the CheckBox
     *
     * @return The current value: True if selected, False if not
     */
    @Override
    public String getValue() {
        return dateTimeBox.getValue();
    }

    /**
     * Sets the value of the CheckBox
     *
     * @param value The value to set for the CheckBox: True if selected, False if not
     */
    @Override
    public void setValue(String value) {
        dateTimeBox.setValue(value, false);
    }

    /**
     * Sets the value of the CheckBox, and fires an event, if the fireEvents parameter is set
     *
     * @param value      The value to set for the CheckBox: True if selected, False if not
     * @param fireEvents Fires an event, if set
     */
    @Override
    public void setValue(String value, boolean fireEvents) {
        dateTimeBox.setValue(value, fireEvents);
    }

    /**
     * Adds a {@link ValueChangeHandler} handler.
     *
     * @param handler the handler
     * @return the registration for the event
     */
    @Override
    public HandlerRegistration addValueChangeHandler(ValueChangeHandler<String> handler) {
        return dateTimeBox.addValueChangeHandler(handler);
    }

    /**
     * Fires a change event
     */
    public void fireChangeEvent() {
        class TextBoxChangedEvent extends ChangeEvent {
        }
        dateTimeBox.fireEvent(new TextBoxChangedEvent());
    }

}
