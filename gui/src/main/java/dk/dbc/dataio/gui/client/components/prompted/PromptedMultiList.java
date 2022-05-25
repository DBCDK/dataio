package dk.dbc.dataio.gui.client.components.prompted;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.uibinder.client.UiConstructor;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.HasValue;
import dk.dbc.dataio.gui.client.components.multilist.MultiList;

import java.util.Map;

public class PromptedMultiList extends PromptedData implements HasValue<Map<String, String>>, HasClickHandlers {

    @UiField
    final MultiList multiList = new MultiList();

    /**
     * Constructor for the PromptedMultiList component
     *
     * @param guiId  The Gui Id
     * @param prompt A prompt text, to be displayed for the Multi List component
     */
    @UiConstructor
    public PromptedMultiList(String guiId, String prompt) {
        super(guiId, prompt);
        multiList.addStyleName(PromptedData.PROMPTED_DATA_DATA_CLASS);
        setEnabled(false);  // When empty, disable multiList box
        add(multiList);
    }

    /**
     * Enables og disables the PromptedMultiList component
     *
     * @param enabled True: Enables the PromptedMultiList Component, False: Disables the PromptedMultiList Component
     */
    public void setEnabled(boolean enabled) {
        multiList.setEnabled(enabled);
    }

    /**
     * Tests whether the supplied parameter is an event, caused by a click on the Add button
     *
     * @param event The event to test
     * @return True: This was a click on the Add button, False: It was not a click on the Add button
     */
    public boolean isAddEvent(ClickEvent event) {
        return multiList.isAddEvent(event);
    }

    /**
     * Tests whether the supplied parameter is an event, caused by a click on the Remove button
     *
     * @param event The event to test
     * @return True: This was a click on the Remove button, False: It was not a click on the Remove button
     */
    public boolean isRemoveEvent(ClickEvent event) {
        return multiList.isRemoveEvent(event);
    }

    /**
     * Gets the key value of the selected item in the multi list
     *
     * @return The key value of the selected item
     */
    public String getSelectedItem() {
        return multiList.getSelectedItem();
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
     *
     * @return All items in the list
     */
    @Override
    public Map<String, String> getValue() {
        return multiList.getValue();
    }

    /**
     * addValue adds another item to the bottom of the list
     *
     * @param text The text for the item
     * @param key  The key for the item
     */
    public void addValue(String text, String key) {
        multiList.addValue(text, key);
    }

    /**
     * setValue replaces all items in the list with the supplied map
     *
     * @param items The new map of list items
     */
    @Override
    public void setValue(Map<String, String> items) {
        setValue(items, true);
    }

    /**
     * setValue replaces all items in the list with the supplied map
     *
     * @param items     The new map of list items
     * @param fireEvent A boolean to determine, if an event is being fired upon change
     */
    @Override
    public void setValue(Map<String, String> items, boolean fireEvent) {
        multiList.setValue(items, fireEvent);
    }

    /**
     * This method adds a Value Change Handler to the list
     *
     * @param valueChangeHandler The Change Handler
     * @return A HandlerRegistration
     */
    @Override
    public HandlerRegistration addValueChangeHandler(ValueChangeHandler<Map<String, String>> valueChangeHandler) {
        return multiList.addValueChangeHandler(valueChangeHandler);
    }

    /**
     * This method implements a Click Handler, to signal clicks on one of the buttons
     *
     * @param clickHandler The Click Handler
     * @return A HandlerRegistration
     */
    @Override
    public HandlerRegistration addClickHandler(final ClickHandler clickHandler) {
        return multiList.addClickHandler(clickHandler);
    }

    /**
     * Sets the sorting in the list to be Manual or Automatic
     *
     * @param manualSorting Manual sorting if true, Automatic if false
     */
    public void setManualSorting(Boolean manualSorting) {
        multiList.setManualSorting(manualSorting);
    }


}
