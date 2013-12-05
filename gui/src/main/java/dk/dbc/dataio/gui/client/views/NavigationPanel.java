
package dk.dbc.dataio.gui.client.views;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.StackPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import dk.dbc.dataio.gui.util.ClientFactory;
import java.util.HashMap;
import java.util.Map;


/**
 *
 * Navigation Panel for the left menu.
 * Contains the Placecontroller, and the logic to control activation of the
 * different views, using the placeController
 * 
 * TODO: Clicking a Main Menu items opens up the Sub Menu's, but the 
 * place activation, as defined in MenuData has not yet been implemented.
 * 
 */
public class NavigationPanel extends FlowPanel {
    public static final String GUIID_NAVIGATION_MENU_PANEL = "navigationmenupanel";
    private static PlaceNavigator placeNavigator = null;
    private static NavigationFeedbackHandler feedbackHandler;


    /**
     * Constructor 
     * @param clientFactory Client Factory to be injected
     * @param guiId The GUI Id for the Navigation panel
     */
    NavigationPanel(ClientFactory clientFactory, String guiId) {
        NavigationPanel.placeNavigator = new PlaceNavigator(clientFactory.getPlaceController());
        getElement().setId(guiId);
        add(new Image("images/dbclogo.gif"));
        add(new MenuPanel(GUIID_NAVIGATION_MENU_PANEL));
    }
    
    /**
     * The caller of this class can inject a callback class, to be activated
     * upon activation of a menu. The Menu Text will be passed as a parameter
     * in the callback.
     * @param handler Callback class (NavigationFeedbackHandler)
     */
    void injectNavigationFeedback(NavigationFeedbackHandler handler) {
        NavigationPanel.feedbackHandler = handler;
    }
    
    /**
     * 
     * Menu Panel
     * 
     * This panel holds the Main Menu and all the Sub Menu items
     * 
     */
    private static class MenuPanel extends StackPanel {

        /**
         * Constructor
         * 
         * The MenuPanel constructor builds up the menu tree, by iterating
         * through the static MenuData structure, and instantiating the 
         * appropriate GUI components accordingly.
         * Furthermore, a menuIndex is also maintained, incrementing from 0 and
         * upwards. In order to be able to map a menuIndex to a Place (that 
         * activates the correct View), the constructor also feeds the 
         * menuIndex/Place pairs to the PlaceNavigator
         * 
         * @param guiId 
         */
        public MenuPanel(String guiId) {
            super();
            getElement().setId(guiId);
            int counter = 0;
            for (MenuData.MainMenuData mainMenuItem: MenuData.structure) {
                VerticalPanel panel = new VerticalPanel();
                for (MenuData.SubMenuData subMenuItem: mainMenuItem.subMenu) {
                    panel.add(new MenuButton(counter, subMenuItem.label, subMenuItem.guiId));
                    placeNavigator.add(counter, subMenuItem.place);
                    counter++;
                }
                add(panel, mainMenuItem.label);
                placeNavigator.add(counter, mainMenuItem.place);
                counter++;
            }
        }
    }

    /**
     * NavigationFeedbackHandler
     * A callback class to be used to signal a menu change to the caller of 
     * the Navigation Class
     */
    interface NavigationFeedbackHandler {
        /**
         * Callback method - transfers the menu text to the caller
         * @param text Menu text
         */
        void navigationChanged(String text);
    }
    
    /**
     * 
     * PlaceNavigator
     * 
     * The purpose of the PlaceNavigator is to maintain a relation between
     * menuIndex'es and Place's.
     * MenuIndex'es are sent from the buttons, when clicking, and Place's 
     * are the object to invoke, when instantiating the appropriate views.
     * 
     */
    private static class PlaceNavigator {
        PlaceController placeController = null;
        Map<Integer, Place> places = new HashMap<Integer, Place>();

        /**
         * Constructor
         * 
         * @param placeController The PlaceController, that controls
         *                        instantiation of Views
         */
        public PlaceNavigator(PlaceController placeController) {
            this.placeController = placeController;
        }

        /**
         * Add a menuIndex/Place pair to the PlaceNavigator
         * 
         * @param menuIndex Menu index
         * @param place Place
         */
        void add(Integer menuIndex, Place place) {
            places.put(menuIndex, place);
        }
        
        /**
         * Navigates to the View, pointed out by the menuIndex given as a parameter
         * 
         * @param menuIndex The menuIndex for the View to activate
         */
        void navigateTo(Integer menuIndex) {
            if (placeController != null && places.containsKey(menuIndex)) {
                placeController.goTo(places.get(menuIndex));
            }
        }
    }
    
    /**
     * 
     * MenuButton
     * 
     * The panel for a Sub Menu button
     * Upon instantiation, a menu index is passed to it, and whenever
     * activated, the button sends this menu index to the PlaceNavigator
     * 
     */
    private static class MenuButton extends Button {
        private String caption = null;
       
        /**
         * Constructor
         * 
         * @param menuIndex The Menu Index for this button
         * @param caption The caption text for the menu button
         * @param guiId A unique GUI Id for identification in the DOM tree
         */
        public MenuButton(int menuIndex, String caption, String guiId) {
            super(caption);
            this.caption = caption;
            addClickHandler(new ButtonHandler(menuIndex));
            getElement().setId(guiId);
        }

        /**
         * The ClickHandler for the MenuButton class
         * Sends the menuIndex to the PlaceNavigator for further processing
         */
        private class ButtonHandler implements ClickHandler {
            private int subPanelIndex;
            public ButtonHandler(int subPanelIndex) {
                this.subPanelIndex = subPanelIndex;
            }
            @Override
            public void onClick(ClickEvent event) {
                if (feedbackHandler != null) {
                    feedbackHandler.navigationChanged(caption);
                }
                placeNavigator.navigateTo(subPanelIndex);
            }
        }
    }
    
}
