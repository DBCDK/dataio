package dk.dbc.dataio.gui.client.components.sortablelist;

public class SortableList extends SortableListWidget {


    /**
     * Constructor
     */
    public SortableList() {
        super();
        setEnabled(false);
    }


    /*
     * Public Methods
     */

    /**
     * Removes all entries in the list
     * It triggers a ValueChangedEvent
     */
    public void clear() {
        model.clear();
    }

    /**
     * Enables or disables the SortableList widget
     * Meaning, that the widget is grayed out, and no interaction can be done on it
     *
     * @param enabled True: Enable the widget, False: Disable the widget
     */
    public void setEnabled(boolean enabled) {
        model.setEnabled(enabled);
    }

    /**
     * Adds one item to the bottom of the list
     * It triggers a ValueChangedEvent
     *
     * @param text The text as it is displayed in the list
     * @param key  The key for the list item
     */
    public void add(String text, String key) {
        model.add(text, key);
    }

    /**
     * Gets the key value of the selected item in the model
     *
     * @return The key value of the selected item
     */
    public String getSelectedItem() {
        return model.getSelectedItem();
    }

    /**
     * Sets the sorting in the list to be Manual or Automatic
     *
     * @param manualSorting Manual sorting if true, Automatic if false
     */
    public void setManualSorting(Boolean manualSorting) {
        model.setManualSorting(manualSorting);
    }


}
