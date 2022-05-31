package dk.dbc.dataio.gui.client.views;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import dk.dbc.dataio.gui.client.components.log.LogPanel;
import dk.dbc.dataio.gui.client.i18n.MainConstants;
import dk.dbc.dataio.gui.client.presenters.GenericPresenter;

/**
 * This is a container panel, for holding a Header panel together with
 * a content panel
 *
 * @param <T> The presenter to use with this view
 */
public abstract class ContentPanel<T extends GenericPresenter> extends FlowPanel {
    protected final static MainConstants mainConstants = GWT.create(MainConstants.class);
    private static final String GUIID_HEADER_PANEL = "header-panel";
    public static final String GUIID_CONTENT_PANEL = "content-panel";

    public T presenter;
    private boolean initialized = false;
    private final HeaderPanel headerPanel = new HeaderPanel(mainConstants.header_DataIO(), GUIID_HEADER_PANEL);
    private final ContentContainerPanel contentPanel = new ContentContainerPanel(GUIID_CONTENT_PANEL);
    private LogPanel logPanel = new LogPanel();

    /**
     * Constructor
     *
     * @param header The text to use as the header test
     */
    public ContentPanel(String header) {
        getElement().setId(GUIID_CONTENT_PANEL);
        getElement().setPropertyObject(GUIID_CONTENT_PANEL, this);
        super.add(headerPanel);
        setHeader(header);
        super.add(contentPanel);
        super.add(logPanel);
    }

    /**
     * Generic initialization
     */

    /**
     * Set the presenter for the concrete view
     *
     * @param presenter the presenter for the instance of the view to use
     */
    public void setPresenter(T presenter) {
        this.presenter = presenter;
        if (!initialized) {
            initialized = true;
            init();
        }
    }

    /**
     * Overridable Initialization for the View
     */
    public void init() {
        // Intentionally left empty - to be overriden by derived classes
    }

    /**
     * Set the header text for the HeaderPanel
     *
     * @param text The header text
     */
    public void setHeader(String text) {
        if (text.isEmpty()) {
            headerPanel.setText(mainConstants.header_DataIO());
        } else {
            headerPanel.setText(mainConstants.header_DataIO() + " >> " + text);
        }
    }

    /**
     * Adds a child to the content panel
     *
     * @param child The child widget to add to the content panel
     */
    @Override
    public void add(Widget child) {
        contentPanel.add(child);
    }

    /**
     * Generic method to signal a failure to the user
     *
     * @param message the message to display in the view
     */
    public void setErrorText(String message) {
        Window.alert("Error: " + message);
    }

    public LogPanel getLogPanel() {
        return this.logPanel;
    }

    /**
     * Header Panel Class
     */
    private static class HeaderPanel extends Label {
        /**
         * Constructor for Header Panel
         *
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
    private static class ContentContainerPanel extends FlowPanel {
        /**
         * Constructor for Content Panel
         *
         * @param guiId The GUI Id for Content Panel
         */
        public ContentContainerPanel(String guiId) {
            super();
            getElement().setId(guiId);
        }
    }

}
