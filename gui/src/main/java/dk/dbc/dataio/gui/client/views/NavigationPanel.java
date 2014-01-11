
package dk.dbc.dataio.gui.client.views;

import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Tree;
import com.google.gwt.user.client.ui.TreeItem;
import dk.dbc.dataio.gui.client.views.Menu.MenuItem;
import dk.dbc.dataio.gui.util.ClientFactory;


/**
 *
 * Navigation Panel for the left menuData.
 * Contains the Placecontroller, and the logic to control activation of the
 * different views, using the placeController
 *
 * TODO: Clicking a Main Menu items opens up the Sub Menu's, but the
 * place activation, as defined in Menu has not yet been implemented.
 *
 */
public class NavigationPanel extends FlowPanel {
    public static final String GUIID_NAVIGATION_MENU_PANEL = "navigationmenupanel";
    private final PlaceController placeController;
    private final Menu menuStructure = new Menu();


    /**
     * Constructor
     * @param clientFactory Client Factory to be injected
     * @param guiId The GUI Id for the Navigation panel
     */
    NavigationPanel(ClientFactory clientFactory, String guiId) {
        placeController = clientFactory.getPlaceController();
        getElement().setId(guiId);
        add(new Image("images/dbclogo.gif"));
        add(new MenuPanel(GUIID_NAVIGATION_MENU_PANEL));
    }

    /**
     *
     * Menu Panel
     *
     * This panel holds the Main Menu and all the Sub Menu items
     *
     */
    private class MenuPanel extends Tree {
        /**
         * Constructor
         * The MenuPanel constructor builds up the menuData tree, by iterating
         * through the Menu structure, and instantiating the
         * appropriate GUI components accordingly.
         *
         * @param guiId
         */
        public MenuPanel(String guiId) {
            super();
            getElement().setId(guiId);
            for (MenuItem mainMenuItem: menuStructure.menuData.subMenuItem) {
                TreeItem root;
                root = new TreeItem();
                root.getElement().setId(mainMenuItem.guiId);
                root.setText(mainMenuItem.label);
                root.setUserObject(mainMenuItem.place);
                for (MenuItem subMenuItem: mainMenuItem.subMenuItem) {
                    TreeItem item = new TreeItem(new Label(subMenuItem.label));
                    item.getElement().setId(subMenuItem.guiId);
                    item.setUserObject(subMenuItem.place);
                    root.addItem(item);
                }
                addItem(root);
            }
            addSelectionHandler(new MenuSelectionHandler());
        }



        private class MenuSelectionHandler implements SelectionHandler<TreeItem> {
            @Override
            public void onSelection(SelectionEvent<TreeItem> event) {
                Place place = (Place) event.getSelectedItem().getUserObject();
                if (placeController != null && place != null) {
                    placeController.goTo(place);
                }
            }

        }
    }

}
