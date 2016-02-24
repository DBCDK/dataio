package dk.dbc.dataio.gui.client.components;

import com.google.gwt.uibinder.client.UiConstructor;
import com.google.gwt.user.client.ui.TextBox;

/**
 * Popup box for entering a text string in a popup window
 */
public class PopupTextBox extends PopupBox<TextBox, String> {

    /**
     * Constructor
     *
     * @param dialogTitle  The title text to display on the Dialog Box (mandatory)
     * @param okButtonText The text to be displayed in the OK Button (mandatory)
     */
    @UiConstructor
    public PopupTextBox(String dialogTitle, String okButtonText) {
        super(new TextBox(), dialogTitle, okButtonText);
    }

}