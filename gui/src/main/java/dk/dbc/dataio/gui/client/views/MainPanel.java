package dk.dbc.dataio.gui.client.views;

import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.SimpleLayoutPanel;
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

    private final NavigationPanel navigationPanel;
    public final ApplicationPanel applicationPanel = new ApplicationPanel(GUIID_APPLICATION_PANEL);

    /**
     * Constructor for Main Panel
     *
     * @param clientFactory
     */
    public MainPanel(ClientFactory clientFactory) {
        super(Style.Unit.PX);
        getElement().setId(GUIID_MAIN_PANEL);
        navigationPanel = new NavigationPanel(clientFactory, GUIID_NAVIGATION_PANEL);
        addWest(navigationPanel, 220);
        add(applicationPanel);
    }

    /**
     * Content Panel Class
     */
    public static class ApplicationPanel extends SimpleLayoutPanel {
        /**
         * Constructor for ContentPanel
         * @param guiId The GUI Id for Content Panel
         */
        public ApplicationPanel(String guiId) {
            getElement().setId(guiId);
        }
    }

}
