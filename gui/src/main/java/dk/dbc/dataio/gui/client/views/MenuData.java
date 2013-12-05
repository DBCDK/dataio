
package dk.dbc.dataio.gui.client.views;

import com.google.gwt.core.client.GWT;
import com.google.gwt.place.shared.Place;
import dk.dbc.dataio.gui.client.i18n.MenuConstants;
import dk.dbc.dataio.gui.client.places.FlowComponentCreatePlace;
import dk.dbc.dataio.gui.client.places.FlowComponentsShowPlace;
import dk.dbc.dataio.gui.client.places.FlowCreatePlace;
import dk.dbc.dataio.gui.client.places.FlowbinderCreatePlace;
import dk.dbc.dataio.gui.client.places.SinkCreatePlace;
import dk.dbc.dataio.gui.client.places.SubmitterCreatePlace;
import java.util.ArrayList;
import java.util.List;

/**
 * 
 * This class contains the menu structure for the GUI
 * It contains pure static data for defining the menu structure for the Data IO User Interface
 * 
 */
public final class MenuData {
    // Main Menu GUI Id's
    public static final String GUIID_MAIN_MENU_ITEM_SUBMITTERS = "mainmenuitemsubmitters";
    public static final String GUIID_MAIN_MENU_ITEM_FLOWS = "mainmenuitemflows";
    public static final String GUIID_MAIN_MENU_ITEM_SINKS = "mainmenuitemsinks";
    // Sub Menu GUI Id's
    public static final String GUIID_SUB_MENU_ITEM_SUBMITTER_CREATION = "submenuitemsubmittercreation";
    public static final String GUIID_SUB_MENU_ITEM_FLOW_CREATION = "submenuitemflowcreation";
    public static final String GUIID_SUB_MENU_ITEM_FLOW_COMPONENT_CREATION = "submenuitemflowcomponentcreation";
    public static final String GUIID_SUB_MENU_ITEM_FLOW_COMPONENTS_SHOW = "submenuitemflowcomponentsshow";
    public static final String GUIID_SUB_MENU_ITEM_FLOWBINDER_CREATION = "submenuitemflowbindercreation";
    public static final String GUIID_SUB_MENU_ITEM_SINK_CREATION = "submenuitemsinkcreation";

    private static final MenuConstants constants = GWT.create(MenuConstants.class);

    /**
     * 
     * MainMenuData
     * This class is a static class for holding the static data to define a
     * Main Menu item
     * 
     */
    public final static class MainMenuData {
        final String label;
        final Place place;
        final String guiId;
        final SubMenuData[] subMenu;

        /**
         * Constructor
         * 
         * @param label The menu text for this main menu item
         * @param place A Place instance for activation of the menu item
         *              If 'place' is null, the menu cannot be activated
         * @param guiId The guiId for identification of the Menu Item in the DOM tree
         * @param subMenu An array of Sub Menu Items under this Main Menu Item
         */
        public MainMenuData(String label, Place place, String guiId, SubMenuData[] subMenu) {
            this.label = label;
            this.place = place;
            this.guiId = guiId;
            this.subMenu = subMenu;
        }
    }

    /**
     * 
     * SubMenuData
     * This class is a static class for holding the static data to define a
     * Sub Menu item
     * 
     */
    final static class SubMenuData {
        final String label;
        final Place place;
        final String guiId;

        /**
         * Constructor
         * 
         * @param Label The menu text for this sub menu item
         * @param place A Place instance for activation of the menu item
         *              If 'place' is null, the menu cannot be activated
         * @param guiId The guiId for identification of the Menu Item in the DOM tree
         */
        public SubMenuData(String Label, Place place, String guiId) {
            this.label = Label;
            this.place = place;
            this.guiId = guiId;
        }
    }
    
    /**
     * 
     * MainMenuData.structure contains the static definition of the complete
     * menu structure for the User Interface
     */
    static final MainMenuData structure[] = {
        new MainMenuData(constants.mainMenu_Submitters(), null, GUIID_MAIN_MENU_ITEM_SUBMITTERS, new SubMenuData[] {
            new SubMenuData(constants.subMenu_SubmitterCreation(), new SubmitterCreatePlace(), GUIID_SUB_MENU_ITEM_SUBMITTER_CREATION), 
        }),
        new MainMenuData(constants.mainMenu_Flows(), null, GUIID_MAIN_MENU_ITEM_FLOWS, new SubMenuData[] {
            new SubMenuData(constants.subMenu_FlowCreation(), new FlowCreatePlace(), GUIID_SUB_MENU_ITEM_FLOW_CREATION),
            new SubMenuData(constants.subMenu_FlowComponentCreation(), new FlowComponentCreatePlace(), GUIID_SUB_MENU_ITEM_FLOW_COMPONENT_CREATION),
            new SubMenuData(constants.subMenu_FlowComponentsShow(), new FlowComponentsShowPlace(), GUIID_SUB_MENU_ITEM_FLOW_COMPONENTS_SHOW),
            new SubMenuData(constants.subMenu_FlowbinderCreation(), new FlowbinderCreatePlace(), GUIID_SUB_MENU_ITEM_FLOWBINDER_CREATION),
        }),
        new MainMenuData(constants.mainMenu_Sinks(), null, GUIID_MAIN_MENU_ITEM_SINKS, new SubMenuData[]{
            new SubMenuData(constants.subMenu_SinkCreation(), new SinkCreatePlace(), GUIID_SUB_MENU_ITEM_SINK_CREATION),
        }),
    };
    
    /**
     * Test whether the guiId passed as a parameter is a Main Menu item
     * @param guiId The guiId to test
     * @return Returns true if the guiId is a Main Menu Item, false if not
     */
    public static boolean isMainMenuItem(String guiId) {
        for (MainMenuData mainMenu: MenuData.structure) {
            if (guiId.equals(mainMenu.guiId)) {
                return true;  // Yes - the guiId is a Main Menu item
            }
        }
        return false;  // The guiId was not identified as a Main Menu Item
    }
    
    /**
     * Test whether the guiId passed as a parameter is a Sub Menu item
     * @param guiId The guiId to test
     * @return Returns true if the guiId is a Sub Menu Item, false if not
     */
    public static boolean isSubMenuItem(String guiId) {
        for (MainMenuData mainMenu: MenuData.structure) {
            for (SubMenuData subMenu: mainMenu.subMenu) {
                if (guiId.equals(subMenu.guiId)) {
                    return true;  // Yes - the guiId is a Sub Menu item
                }
            }
        }
        return false;  // The guiId was not identified as a sub Menu Item
    }
    
    /**
     * Assumes, that the passed parameter is a Sub Menu Item, and returns
     * the corresponding Main Menu Item, where it belongs
     * @param subMenuItem The Sub Menu Item to test
     * @return The Main Menu Item, where the Sub Menu Item belongs to,
     *         null if no corresponding Main Menu was found
     */
    public static String findMainMenuItem(String subMenuItem) {
        for (MainMenuData mainMenu: MenuData.structure) {
            for (SubMenuData subMenu: mainMenu.subMenu) {
                if (subMenuItem.equals(subMenu.guiId)) {
                    return mainMenu.guiId;  // Gotcha...
                }
            }
        }
        return null;  // The subMenuItem was not found under a Main Menu
    }

    /**
     * Finds all Main Menu guiId's, and returns them to the caller
     * @return All MainMenu guiId's
     */
    public static List<String> findAllMainMenuIds() {
        List<String> mainMenus = new ArrayList<String>();
        for (MainMenuData mainMenu: MenuData.structure) {
            mainMenus.add(mainMenu.guiId);
        }
        return mainMenus;
    }

    /**
     * Finds all Sub Menu guiId's, that is a child of the mainMenuId as given in the call to the method.
     * It then returns the complete list of subMenuId's
     * @param mainMenuId The parent Main Menu Id
     * @return The list of found Sub Menu Id's, that belongs to the main menu item
     * @throws Exception If mainMenuId is not found, an exception is thrown
     */
    public static List<String> findAllSubMenuIds(String mainMenuId) throws Exception {
        List<String> subMenus = new ArrayList<String>();
        for (MainMenuData mainMenu: MenuData.structure) {
            if (mainMenuId.equals(mainMenu.guiId)) {
                for (SubMenuData subMenu: mainMenu.subMenu) {
                    subMenus.add(subMenu.guiId);
                }
            return subMenus;
            }
        }
        throw new Exception("That Main Menu Item Id was not found");
    }
    
}
