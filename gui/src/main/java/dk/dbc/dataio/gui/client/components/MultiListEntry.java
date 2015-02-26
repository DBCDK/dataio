package dk.dbc.dataio.gui.client.components;

import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.uibinder.client.UiConstructor;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.HasValue;
import dk.dbc.dataio.gui.client.components.MultiList.MultiList;

import java.util.Map;

public class MultiListEntry extends DataEntry implements HasValue<Map<String, String>> {

    @UiField final MultiList multiList = new MultiList();

    @UiConstructor
    public MultiListEntry(String guiId, String prompt) {
        super(guiId, prompt);
        multiList.addStyleName(DataEntry.DATA_ENTRY_INPUT_BOX_CLASS);
        setEnabled(false);  // When empty, disable multiList box
        add(multiList);
    }

    public void setEnabled(boolean enabled) {
        multiList.setEnabled(enabled);
    }

    /**
     * This methods removes all items from the Multi List
     */
    @Override
    public void clear() {
        multiList.clear();
    }

    /**
     * getValue fetches the all items in the list as a map of Key/Value pairs
     * @return All items in the list
     */
    @Override
    public Map<String, String> getValue() {
        return multiList.getValue();
    }

    /**
     * addValue adds another item to the bottom of the list
     * @param text The text for the item
     * @param key The key for the item
     */
    public void addValue(String text, String key) {
        multiList.addValue(text, key);
    }

    /**
     * addValue adds another item to the bottom of the list
     * @param text The text for the item
     * @param key The key for the item
     * @param fireEvent A boolean to determine, if an event is being fired upon change
     */
    public void addValue(String text, String key, boolean fireEvent) {
        multiList.addValue(text, key, fireEvent);
    }

    /**
     * setValue replaces all items in the list with the supplied map
     * @param items The new map of list items
     */
    @Override
    public void setValue(Map<String, String> items) {
        setValue(items, true);
    }

    /**
     * setValue replaces all items in the list with the supplied map
     * @param items The new map of list items
     * @param fireEvent A boolean to determine, if an event is being fired upon change
     */
    @Override
    public void setValue(Map<String, String> items, boolean fireEvent) {
        multiList.setValue(items, fireEvent);
    }

    /**
     * This method adds a Value Change Handler to the list
     * @param valueChangeHandler The Change Handler
     * @return A HandlerRegistration
     */
    @Override
    public HandlerRegistration addValueChangeHandler(ValueChangeHandler<Map<String, String>> valueChangeHandler) {
        return multiList.addValueChangeHandler(valueChangeHandler);
    }
}
