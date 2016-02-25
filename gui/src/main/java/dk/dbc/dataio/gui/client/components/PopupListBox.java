package dk.dbc.dataio.gui.client.components;

import com.google.gwt.uibinder.client.UiConstructor;

/**
 * Popup box for entering a list in a popup window
 */
public class PopupListBox extends PopupBox<ListBoxHasValue, String> {
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
     * @param item The item to add to the list
     */
    public void add(String item) {
        widget.addItem(item);
        widget.setVisibleItemCount(widget.getItemCount() > MAX_ITEMS_IN_LIST ? MAX_ITEMS_IN_LIST : widget.getItemCount());
    }

}