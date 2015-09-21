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

package dk.dbc.dataio.gui.client.components.multilist;

import com.google.gwt.event.dom.client.ClickEvent;

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

    /**
     * Tests whether the event argument is a result of a click on the Add button
     * Please note, that the preferred way to implement this would be to supply this information in the event itself,
     * but this is currently not possible.
     * @param event Event, containing the click event to test for
     * @return True if the event was an Add button click, False if not
     */
    public boolean isAddEvent(ClickEvent event) {
        return event.getSource().equals(addButton);
    }

    /**
     * Tests whether the event argument is a result of a click on the Remove button
     * Please note, that the preferred way to implement this would be to supply this information in the event itself,
     * but this is currently not possible.
     * @param event Event, containing the click event to test for
     * @return True if the event was an Remove button click, False if not
     */
    public boolean isRemoveEvent(ClickEvent event) {
        return event.getSource().equals(removeButton);
    }

    /**
     * Gets the selected item in the list as a key value
     * @return The key value of the selected item
     */
    public String getSelectedItem() {
        return list.getSelectedItem();
    }
}
