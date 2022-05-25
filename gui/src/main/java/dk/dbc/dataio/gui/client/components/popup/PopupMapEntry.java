package dk.dbc.dataio.gui.client.components.popup;

import com.google.gwt.uibinder.client.UiConstructor;
import dk.dbc.dataio.gui.client.components.MapEntry;

import java.util.Map;

/**
 * Popup box for entering a text string in a popup window
 */
public class PopupMapEntry extends PopupValueBox<MapEntry, Map.Entry<String, String>> {

    /**
     * Constructor
     *
     * @param keyPrompt    The prompt for the text box for the key entry
     * @param valuePrompt  The prompt for the text box for the value entry
     * @param dialogTitle  The title text to display on the Dialog Box
     * @param okButtonText The text to be displayed in the OK Button
     */
    @UiConstructor
    public PopupMapEntry(String keyPrompt, String valuePrompt, String dialogTitle, String okButtonText) {
        super(new MapEntry(keyPrompt, valuePrompt), dialogTitle, okButtonText);
    }

}
