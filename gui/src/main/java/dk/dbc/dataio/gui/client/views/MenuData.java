
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
    final static class MainMenuData {
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
    
}
