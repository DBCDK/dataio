package dk.dbc.dataio.gui.client.components;

import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Focusable;
import com.google.gwt.user.client.ui.RootPanel;


/**
 * EnterButton is like Button, with the added handling of the Enter Key, that fires a click on this button
 */

public class EnterButton extends Button implements Focusable {
    public EnterButton() {
        RootPanel.get().addDomHandler(new EnterKeyPressHandler(), KeyPressEvent.getType());
    }

    private class EnterKeyPressHandler implements KeyPressHandler {
        @Override
        public void onKeyPress(KeyPressEvent event) {
            int keyCode = event.getNativeEvent().getKeyCode();
            if (keyCode == KeyCodes.KEY_ENTER) {
                setFocus(true);  // Set focus to the button itself, in order to fire the ValueChangeEvent on the previously focus'ed widget
                setFocus(false);  // Blur focus in order to avoid double activations
                click();
            }
        }
    }
}
