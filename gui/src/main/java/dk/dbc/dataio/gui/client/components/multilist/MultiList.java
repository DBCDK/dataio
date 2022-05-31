package dk.dbc.dataio.gui.client.components.multilist;

import com.google.gwt.event.dom.client.ClickEvent;

public class MultiList extends MultiListWidget {

    /**
     * Clears all values in the MultiList component
     */
    public void clear() {
        list.clear();
    }

    /**
     * addValue adds another item to the bottom of the list
     *
     * @param text The text for the item
     * @param key  The key for the item
     */
    public void addValue(String text, String key) {
        list.add(text, key);
    }

    /**
     * Enables the MultiList Component
     *
     * @param enabled True: Enable the component, False: Disable the component
     */
    public void setEnabled(boolean enabled) {
        list.setEnabled(enabled);
        addButton.setEnabled(enabled);
        removeButton.setEnabled(enabled);
    }

    /**
     * Tests whether the event argument is a result of a click on the Add button
     * Please note, that the preferred way to implement this would be to supply this information in the event itself,
     * but this is currently not possible.
     *
     * @param event Event, containing the click event to test for
     * @return True if the event was an Add button click, False if not
     */
    public boolean isAddEvent(ClickEvent event) {
        return event.getSource().equals(addButton);
    }

    /**
     * Tests whether the event argument is a result of a click on the Remove button
     * Please note, that the preferred way to implement this would be to supply this information in the event itself,
     * but this is currently not possible.
     *
     * @param event Event, containing the click event to test for
     * @return True if the event was an Remove button click, False if not
     */
    public boolean isRemoveEvent(ClickEvent event) {
        return event.getSource().equals(removeButton);
    }

    /**
     * Gets the selected item in the list as a key value
     *
     * @return The key value of the selected item
     */
    public String getSelectedItem() {
        return list.getSelectedItem();
    }

    /**
     * Sets the sorting in the list to be Manual or Automatic
     *
     * @param manualSorting Manual sorting if true, Automatic if false
     */
    public void setManualSorting(Boolean manualSorting) {
        list.setManualSorting(manualSorting);
    }

}
