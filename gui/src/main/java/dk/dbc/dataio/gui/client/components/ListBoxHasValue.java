package dk.dbc.dataio.gui.client.components;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.HasValue;
import com.google.gwt.user.client.ui.ListBox;

/**
 * <p>A list box (using the standard ListBox component), implementing the interface HasValue</p>
 * <p>The value in question is the selected value</p>
 */
public class ListBoxHasValue extends ListBox implements HasValue<String> {
    ValueChangeHandler<String> valueChangeHandler = null;  // This is package private because of test - should be private

    /**
     * Constructor
     */
    public ListBoxHasValue() {
        addChangeHandler(new ChangeHandler() {
            @Override
            public void onChange(ChangeEvent event) {
                triggerValueChangeEvent();
            }
        });
    }


    /*
     * Implementation of HasValue methods
     */

    /**
     * Gets the value of the selected item
     * @return The value of the selected item
     */
    @Override
    public String getValue() {
        return super.getValue(super.getSelectedIndex());
    }

    /**
     * Sets the selection to the item, matching the input parameter
     * If no match is found within the list, the selection will not be changed
     * No Value Change event is fired
     * @param value The string to be the future selected item
     */
    @Override
    public void setValue(String value) {
        setValue(value, false);
    }

    /**
     * Sets the selection to the item, matching the input parameter
     * If no match is found within the list, the selection will not be changed
     * @param value The string to be the future selected item
     * @param fireEvents If true, a Value Change event will be fired if a match is found
     */
    @Override
    public void setValue(String value, boolean fireEvents) {
        for (int i=0; i<getItemCount(); i++) {
            if (value.equals(getItemText(i))) {
                setSelectedIndex(i);
                break;
            }
        }
        if (fireEvents) {
            triggerValueChangeEvent();
        }
    }

    /**
     * Adds a Value Change Handler
     * @param changeHandler The Value Change Handler
     * @return A Remove Handler to be used, if the event handler needs to be removed
     */
    @Override
    public HandlerRegistration addValueChangeHandler(ValueChangeHandler<String> changeHandler) {
        valueChangeHandler = changeHandler;
        return new HandlerRegistration() {
            @Override
            public void removeHandler() {
                valueChangeHandler = null;
            }
        };
    }


    /**
     * Private methods
     */

    /**
     * Triggers a Value Change Event - but only if there is an eventhandler registered
     */
    private void triggerValueChangeEvent() {
        if (valueChangeHandler != null) {
            valueChangeHandler.onValueChange(new ValueChangeEvent<String>(getValue()) {});
        }
    }

}
