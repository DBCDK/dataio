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
     * @param keyPrompt The prompt for the text box for the key entry
     * @param valuePrompt The prompt for the text box for the value entry
     * @param dialogTitle The title text to display on the Dialog Box
     * @param okButtonText The text to be displayed in the OK Button
     */
    @UiConstructor
    public PopupMapEntry(String keyPrompt, String valuePrompt, String dialogTitle, String okButtonText) {
        super(new MapEntry(keyPrompt, valuePrompt), dialogTitle, okButtonText);
    }

}