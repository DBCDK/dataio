/*
 * MenuItem
 * Declaration of one single menu item
 * Each item contains:
 *  o A gui ID
 *  o A label for the menu item
 *  o A GWT Place, which is the reference to the new gui page to activate
 *  o A list of sub menu's (of type MenuItem)
 *
 */

package dk.dbc.dataio.gui.client.views;

import com.google.gwt.place.shared.Place;
import java.util.Arrays;
import java.util.List;

/**
 *
 * Class MenuItem
 */
public class MenuItem {
    final String guiId;
    final String label;
    final Place place;
    final List<MenuItem> subMenuItem;

    public MenuItem(String guiId, String label, Place place, MenuItem ... subMenuItems) {
        this.guiId = guiId;
        this.label = label;
        this.place = place;
        this.subMenuItem = Arrays.asList(subMenuItems);
    }

}
