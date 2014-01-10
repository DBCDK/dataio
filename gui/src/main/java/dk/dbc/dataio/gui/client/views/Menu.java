
package dk.dbc.dataio.gui.client.views;

import com.google.gwt.core.client.GWT;
import com.google.gwt.place.shared.Place;
import dk.dbc.dataio.gui.client.i18n.MainConstants;
import dk.dbc.dataio.gui.client.places.FlowComponentCreatePlace;
import dk.dbc.dataio.gui.client.places.FlowComponentsShowPlace;
import dk.dbc.dataio.gui.client.places.FlowCreatePlace;
import dk.dbc.dataio.gui.client.places.FlowbinderCreatePlace;
import dk.dbc.dataio.gui.client.places.FlowsShowPlace;
import dk.dbc.dataio.gui.client.places.SinkCreatePlace;
import dk.dbc.dataio.gui.client.places.SubmitterCreatePlace;
import dk.dbc.dataio.gui.client.places.SubmittersShowPlace;
import dk.dbc.dataio.gui.client.views.Menu.MenuItem;
import java.util.Arrays;
import java.util.List;

/**
 *
 * This class contains the menuData structure for the GUI
 * It contains pure static data for defining the menuData structure for the Data IO User Interface
 *
 */
public final class Menu {
    // Main Menu GUI Id's
    public final static String GUIID_MAIN_MENU_ITEM_SUBMITTERS = "mainmenuitemsubmitters";
    public final static String GUIID_MAIN_MENU_ITEM_FLOWS = "mainmenuitemflows";
    public final static String GUIID_MAIN_MENU_ITEM_SINKS = "mainmenuitemsinks";
    // Sub Menu GUI Id's
    public final static String GUIID_SUB_MENU_ITEM_SUBMITTER_CREATION = "submenuitemsubmittercreation";
    public final static String GUIID_SUB_MENU_ITEM_FLOW_CREATION = "submenuitemflowcreation";
    public final static String GUIID_SUB_MENU_ITEM_FLOW_COMPONENT_CREATION = "submenuitemflowcomponentcreation";
    public final static String GUIID_SUB_MENU_ITEM_FLOW_COMPONENTS_SHOW = "submenuitemflowcomponentsshow";
    public final static String GUIID_SUB_MENU_ITEM_FLOWBINDER_CREATION = "submenuitemflowbindercreation";
    public final static String GUIID_SUB_MENU_ITEM_SINK_CREATION = "submenuitemsinkcreation";

    public final static Place NOWHERE = null;

    private final static MainConstants constants = GWT.create(MainConstants.class);

    public final MenuItem menuData;


    /**
     * Constructor - defines the static data for the menu structure
     */
    public Menu() {
        // Submitters Main Menu
        MenuItem createSubmitter = new MenuItem(GUIID_SUB_MENU_ITEM_SUBMITTER_CREATION, constants.subMenu_SubmitterCreation(), new SubmitterCreatePlace());
        MenuItem submittersMenu = new MenuItem(GUIID_MAIN_MENU_ITEM_SUBMITTERS, constants.mainMenu_Submitters(), new SubmittersShowPlace(),
            createSubmitter);

        // Flows Main Menu
        MenuItem createFlow = new MenuItem(GUIID_SUB_MENU_ITEM_FLOW_CREATION, constants.subMenu_FlowCreation(), new FlowCreatePlace());
        MenuItem createFlowComponent = new MenuItem(GUIID_SUB_MENU_ITEM_FLOW_COMPONENT_CREATION, constants.subMenu_FlowComponentCreation(), new FlowComponentCreatePlace());
        MenuItem showFlowComponents = new MenuItem(GUIID_SUB_MENU_ITEM_FLOW_COMPONENTS_SHOW, constants.subMenu_FlowComponentsShow(), new FlowComponentsShowPlace());
        MenuItem createFlowBinder = new MenuItem(GUIID_SUB_MENU_ITEM_FLOWBINDER_CREATION, constants.subMenu_FlowbinderCreation(), new FlowbinderCreatePlace());
        MenuItem flowsMenu = new MenuItem(GUIID_MAIN_MENU_ITEM_FLOWS, constants.mainMenu_Flows(), new FlowsShowPlace(),
            createFlow,
            createFlowComponent,
            showFlowComponents,
            createFlowBinder);

        // Sink Main Menu
        MenuItem createSink = new MenuItem(GUIID_SUB_MENU_ITEM_SINK_CREATION, constants.subMenu_SinkCreation(), new SinkCreatePlace());
        MenuItem sinkMenu = new MenuItem(GUIID_MAIN_MENU_ITEM_SINKS, constants.mainMenu_Sinks(), NOWHERE,
            createSink);

        // Toplevel Main Menu Container
        menuData = new MenuItem("toplevelmainmenu", "Toplevel Main Menu", NOWHERE,
            submittersMenu,
            flowsMenu,
            sinkMenu);
    }


    /**
     * Menu Item class
     */
    public final static class MenuItem {
        public String guiId;
        String label;
        Place place;
        List<MenuItem> subMenuItem;

        public MenuItem(String guiId, String label, Place place, MenuItem ... subMenuItems) {
            this.guiId = guiId;
            this.label = label;
            this.place = place;
            this.subMenuItem = Arrays.asList(subMenuItems);
        }

    }

}
