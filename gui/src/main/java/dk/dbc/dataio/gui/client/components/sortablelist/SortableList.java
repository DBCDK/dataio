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
     * @param enabled True: Enable the widget, False: Disable the widget
     */
    public void setEnabled(boolean enabled) {
        model.setEnabled(enabled);
    }

    /**
     * Adds one item to the bottom of the list
     * It triggers a ValueChangedEvent
     * @param text The text as it is displayed in the list
     * @param key The key for the list item
     */
    public void add(String text, String key) {
        model.add(text, key);
    }

    /**
     * Gets the key value of the selected item in the model
     * @return The key value of the selected item
     */
    public String getSelectedItem() {
        return model.getSelectedItem();
    }

    /**
     * Sets the sorting in the list to be Manual or Automatic
     * @param manualSorting Manual sorting if true, Automatic if false
     */
    public void setManualSorting(Boolean manualSorting) {
        model.setManualSorting(manualSorting);
    }


}
