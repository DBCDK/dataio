package dk.dbc.dataio.gui.client.components;

import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.HasValue;
import com.google.gwt.user.client.ui.ListBox;

import java.util.Map;
import java.util.TreeMap;

/**
 * <p>A list box (using the standard ListBox component), implementing the interface HasValue</p>
 * <p>The value in question is the selected value as a Map&lt;text, key&gt;</p>
 */
public class ListBoxHasValue extends ListBox implements HasValue<Map<String, String>> {
    ValueChangeHandler<Map<String, String>> valueChangeHandler = null;  // This is package private because of test - should be private

    /**
     * Constructor
     */
    public ListBoxHasValue() {
        addChangeHandler(event -> triggerValueChangeEvent());
    }


    /*
     * Implementation of HasValue methods
     */

    /**
     * Gets the value of the selected item
     *
     * @return The value of the selected item
     */
    @Override
    public Map<String, String> getValue() {
        int selectedIndex = super.getSelectedIndex();
        if (selectedIndex < 0) {
            return null;
        }
        TreeMap<String, String> selectedItems = new TreeMap<>();
        for (int i = 0; i < getItemCount(); i++) {
            if (isItemSelected(i)) {
                selectedItems.put(getValue(i), getItemText(i));
            }
        }
        return selectedItems;
    }

    /**
     * Sets the selection to the item, matching the input parameter
     * If no match is found within the list, the selection will not be changed
     * No Value Change event is fired
     *
     * @param value The item to be the future selected item
     */
    @Override
    public void setValue(Map<String, String> value) {
        setValue(value, false);
    }

    /**
     * Sets the selection to the item, matching the input parameter
     * If no match is found within the list, the selection will not be changed
     *
     * @param value      The item to be the future selected item
     * @param fireEvents If true, a Value Change event will be fired if a match is found
     */
    @Override
    public void setValue(Map<String, String> value, boolean fireEvents) {
        if (value != null) {
            for (int i = 0; i < getItemCount(); i++) {
                setItemSelected(i, value.containsKey(getValue(i)));
            }
        }
        if (fireEvents) {
            triggerValueChangeEvent();
        }
    }

    /**
     * Adds a Value Change Handler
     *
     * @param changeHandler The Value Change Handler
     * @return A Remove Handler to be used, if the event handler needs to be removed
     */
    @Override
    public HandlerRegistration addValueChangeHandler(ValueChangeHandler<Map<String, String>> changeHandler) {
        valueChangeHandler = changeHandler;
        return () -> valueChangeHandler = null;
    }


    /**
     * Private methods
     */

    /**
     * Triggers a Value Change Event - but only if there is an eventhandler registered
     */
    private void triggerValueChangeEvent() {
        if (valueChangeHandler != null) {
            valueChangeHandler.onValueChange(new ValueChangeEvent<Map<String, String>>(getValue()) {
            });
        }
    }

}
