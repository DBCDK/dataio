package dk.dbc.dataio.gui.client.views;

import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import dk.dbc.dataio.gui.client.pages.navigation.NavigationPanel;
import dk.dbc.dataio.gui.util.ClientFactory;

/**
 * This is the main panel for the dataio user interface
 * The main panel is divided in two areas:
 * o The Navigation area (maintained by Navigation Panel)
 * o The Content area (maintained by Content Panel)
 */
public class MainPanel extends DockLayoutPanel {
    public static final String GUIID_MAIN_PANEL = "main-panel";
    private static final String GUIID_NAVIGATION_PANEL = "navigation-panel";
    private static final String GUIID_APPLICATION_PANEL = "application-panel";

    public final ScrollPanel applicationPanel;
    public NavigationPanel navigationPanel;

    /**
     * Constructor for Main Panel
     *
     * @param clientFactory The Clientfactory to be used by the Main Panel
     */
    public MainPanel(ClientFactory clientFactory) {
        super(Style.Unit.PX);

        getElement().setId(GUIID_MAIN_PANEL);
        getElement().setPropertyObject(GUIID_MAIN_PANEL, this);

        navigationPanel = new NavigationPanel(clientFactory.getPlaceController());
        navigationPanel.getElement().setId(GUIID_NAVIGATION_PANEL);
        addWest(navigationPanel, 220);

        applicationPanel = new ScrollPanel();
        applicationPanel.getElement().setId(GUIID_APPLICATION_PANEL);
        add(applicationPanel);
    }

    public String getBackgroundImage() {
        return getElement().getStyle().getBackgroundImage();
    }

    public void setBackgroundImage(String url) {
        getElement().getStyle().setBackgroundImage(url);
    }

    public void setNavigationPanelBackgroundColor(String color) {
        navigationPanel.getElement().getStyle().setBackgroundColor(color);
    }

    public void setApplicationPanelBackgroundColor(String color) {
        applicationPanel.getElement().getStyle().setBackgroundColor(color);
    }

}
