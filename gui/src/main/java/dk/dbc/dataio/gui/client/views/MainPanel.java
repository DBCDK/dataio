package dk.dbc.dataio.gui.client.views;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimpleLayoutPanel;
import dk.dbc.dataio.gui.client.i18n.MainConstants;
import dk.dbc.dataio.gui.util.ClientFactory;

/**
 *
 * This is the main panel for the dataio user interface
 * The main panel is divided in three areas:
 *  o The Header area (maintained by Header Panel)
 *  o The Navigation area (maintained by Navigation Panel)
 *  o The Content area (maintained by Content Panel)
 *
 */
public class MainPanel extends DockLayoutPanel {
    private static final String GUIID_MAIN_PANEL = "main-panel";
    private static final String GUIID_HEADER_PANEL = "header-panel";
    private static final String GUIID_NAVIGATION_PANEL = "navigation-panel";
    private static final String GUIID_CONTENT_PANEL = "content-panel";

    private final MainConstants constants = GWT.create(MainConstants.class);

    public final ContentPanel contentPanel = new ContentPanel(GUIID_CONTENT_PANEL);
    private final HeaderPanel headerPanel = new HeaderPanel(constants.header_DataIO(), GUIID_HEADER_PANEL);
    private final NavigationPanel navigationPanel;

    /**
     * Constructor for Main Panel
     *
     * @param clientFactory
     */
    public MainPanel(ClientFactory clientFactory) {
        super(Style.Unit.PX);
        navigationPanel = new NavigationPanel(clientFactory, GUIID_NAVIGATION_PANEL);
        navigationPanel.injectNavigationFeedback(new NavigationPanel.NavigationFeedbackHandler() {
            @Override public void navigationChanged(String text) {
                headerPanel.setText(constants.header_DataIO() + " >> " + text);
            }
        });
        getElement().setId(GUIID_MAIN_PANEL);
        addWest(navigationPanel, 250);
        addNorth(headerPanel, 30);
        add(contentPanel);
    }

    /**
     * Header Panel Class
     */
    private static class HeaderPanel extends Label {
        /**
         * Constructor for Header Panel
         * @param label The Header Text
         * @param guiId The GUI Id for Header Panel
         */
        public HeaderPanel(String label, String guiId) {
            super();
            getElement().setId(guiId);
            setText(label);
        }
    }

    /**
     * Content Panel Class
     */
    private static class ContentPanel extends SimpleLayoutPanel {
        /**
         * Constructor for ContentPanel
         * @param guiId The GUI Id for Content Panel
         */
        public ContentPanel(String guiId) {
            getElement().setId(guiId);
        }
    }

}
