/*
 * DataIO - Data IO
 * Copyright (C) 2015 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
 * Denmark. CVR: 15149043
 *
 * This file is part of DataIO.
 *
 * DataIO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DataIO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DataIO.  If not, see <http://www.gnu.org/licenses/>.
 */

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
            int keyCode = event.getNativeEvent ().getKeyCode();
            if (keyCode == KeyCodes.KEY_ENTER) {
                setFocus(true);  // Set focus to the button itself, in order to fire the ValueChangeEvent on the previously focus'ed widget
                setFocus(false);  // Blur focus in order to avoid double activations
                click();
            }
        }
    }
}
