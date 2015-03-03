package dk.dbc.dataio.gui.client.components.MultiList;

public class MultiList extends MultiListWidget {

    /**
     * Clears all values in the MultiList component
     */
    public void clear() {
        list.clear();
    }

    /**
     * addValue adds another item to the bottom of the list
     * @param text The text for the item
     * @param key The key for the item
     */
    public void addValue(String text, String key) {
        list.add(text, key);
    }

    /**
     * Enables the MultiList Component
     * @param enabled True: Enable the component, False: Disable the component
     */
    public void setEnabled(boolean enabled) {
        list.setEnabled(enabled);
        addButton.setEnabled(enabled);
        removeButton.setEnabled(enabled);
    }

}
