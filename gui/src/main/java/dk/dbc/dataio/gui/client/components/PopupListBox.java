package dk.dbc.dataio.gui.client.components;

import com.google.gwt.uibinder.client.UiConstructor;

import java.util.Map;

/**
 * Popup box for entering a list in a popup window
 */
public class PopupListBox extends PopupBox<ListBoxHasValue, Map.Entry<String, String>> {
    private final int MAX_ITEMS_IN_LIST = 20;

    /**
     * Constructor
     *
     * @param dialogTitle  The title text to display on the Dialog Box (mandatory)
     * @param okButtonText The text to be displayed in the OK Button (mandatory)
     */
    @UiConstructor
    public PopupListBox(String dialogTitle, String okButtonText) {
        super(new ListBoxHasValue(), dialogTitle, okButtonText);
    }


    /*
     * Additional public method - specific for handling List functionality
     */

    /**
     * Clears the list of available items in the list
     */
    public void clear() {
        widget.clear();
    }

    /**
     * Adds an item to the list of available items in the list
     * @param text The item to add to the list
     * @param value The key of the item added
     */
    public void add(String text, String value) {
        widget.addItem(text, value);
        setListBoxSize(widget.getItemCount());
    }


    /*
     * Private methods
     */

    /**
     * <p>Sets the size of the ListBox</p>
     * <p>Assures, that the size of the listbox is larger than 2 and smaller than MAX_ITEMS_IN_LIST
     * The reason for making it larger than 2 is, that ListBox uses the standard html tag SELECT to
     * construct the list - and if the size is being set to 1, a combo box is shown instead. And this
     * is task of the browser, so we cannot prevent that (unless using something different from a SELECT tag)</p>
     * @param count Number of items in the listbox
     */
    private void setListBoxSize(int count) {
        if (count < 2) {
            count = 2;
        } else if (count > MAX_ITEMS_IN_LIST) {
            count = MAX_ITEMS_IN_LIST;
        }
        widget.setVisibleItemCount(count);
    }
}
