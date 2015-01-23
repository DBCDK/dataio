package dk.dbc.dataio.gui.client.views;

import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import dk.dbc.dataio.gui.client.pages.navigation.NavigationPanel;
import dk.dbc.dataio.gui.util.ClientFactory;

/**
 *
 * This is the main panel for the dataio user interface
 * The main panel is divided in two areas:
 *  o The Navigation area (maintained by Navigation Panel)
 *  o The Content area (maintained by Content Panel)
 *
 */
public class MainPanel extends DockLayoutPanel {
    private static final String GUIID_MAIN_PANEL = "main-panel";
    private static final String GUIID_NAVIGATION_PANEL = "navigation-panel";
    private static final String GUIID_APPLICATION_PANEL = "application-panel";

    public final ScrollPanel applicationPanel;


    /**
     * Constructor for Main Panel
     *
     * @param clientFactory The Clientfactory to be used by the Main Panel
     */
    public MainPanel(ClientFactory clientFactory) {
        super(Style.Unit.PX);
        getElement().setId(GUIID_MAIN_PANEL);

        NavigationPanel navigationPanel = new NavigationPanel(clientFactory.getPlaceController());
        navigationPanel.getElement().setId(GUIID_NAVIGATION_PANEL);
        addWest(navigationPanel, 220);

        applicationPanel = new ScrollPanel();
        applicationPanel.getElement().setId(GUIID_APPLICATION_PANEL);
        add(applicationPanel);
    }

}
