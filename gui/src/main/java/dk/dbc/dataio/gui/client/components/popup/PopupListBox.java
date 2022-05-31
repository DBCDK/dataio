package dk.dbc.dataio.gui.client.components.popup;

import com.google.gwt.uibinder.client.UiConstructor;
import dk.dbc.dataio.gui.client.components.ListBoxHasValue;

import java.util.Map;

/**
 * Popup box for entering a list in a popup window
 */
public class PopupListBox extends PopupValueBox<ListBoxHasValue, Map<String, String>> {
    private final int MAX_ITEMS_IN_LIST = 20;
    private final int MIN_ITEMS_IN_LIST = 2;
    private final String DEFAULT_WIDTH = "16em";


    /**
     * Constructor
     *
     * @param dialogTitle  The title text to display on the Dialog Box (mandatory)
     * @param okButtonText The text to be displayed in the OK Button (mandatory)
     */
    @UiConstructor
    public PopupListBox(String dialogTitle, String okButtonText) {
        super(new ListBoxHasValue(), dialogTitle, okButtonText);
        widget.setVisibleItemCount(MIN_ITEMS_IN_LIST);  // Default initial value to be used, before any items are added
        widget.setWidth(DEFAULT_WIDTH);  // Still a default value only to be used before any items are added
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
     *
     * @param item  The text of the item to add to the list
     * @param value The item's value, to be submitted if it is part of a FormPanel
     */
    public void addItem(String item, String value) {
        widget.addItem(item, value);
    }

    /**
     * Sets the possibility of setting more than one list item in the list
     *
     * @param multipleSelect If true, it is possible to select more than one item in the list.
     */
    public void setMultipleSelect(Boolean multipleSelect) {
        widget.setMultipleSelect(multipleSelect);
    }


    /*
     * Override PopupValueBox methods
     */

    /**
     * Shows the popup and attach it to the page. It must have a child widget before this method is called.
     */
    @Override
    public void show() {
        super.show();
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
     *
     * @param count Number of items in the listbox
     */
    private void setListBoxSize(int count) {
        if (count < MIN_ITEMS_IN_LIST) {
            count = MIN_ITEMS_IN_LIST;
        } else if (count > MAX_ITEMS_IN_LIST) {
            count = MAX_ITEMS_IN_LIST;
        }
        widget.setVisibleItemCount(count);
        widget.setWidth("");  // Let the ListBox adjust it's size according to its content (see constructor)
    }
}
