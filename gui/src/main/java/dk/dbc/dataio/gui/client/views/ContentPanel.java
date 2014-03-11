
package dk.dbc.dataio.gui.client.views;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import dk.dbc.dataio.gui.client.i18n.MainConstants;
import dk.dbc.dataio.gui.client.presenters.Presenter;

/**
 * This is a container panel, for holding a Header panel together with
 * a content panel
 * @param <T> The presenter to use with this view
 */
public abstract class ContentPanel<T extends Presenter> extends FlowPanel {
    protected final static MainConstants mainConstants = GWT.create(MainConstants.class);
    private static final String GUIID_HEADER_PANEL = "header-panel";
    private static final String GUIID_CONTENT_PANEL = "content-panel";

    protected T presenter;
    private boolean initialized = false;
    private final HeaderPanel headerPanel = new HeaderPanel(mainConstants.header_DataIO(), GUIID_HEADER_PANEL);
    private final ContentContainerPanel contentPanel = new ContentContainerPanel(GUIID_CONTENT_PANEL);

    /**
     * Constructor
     * @param header The text to use as the header test
     */
    public ContentPanel(String header) {
        super.add(headerPanel);
        setHeader(header);
        super.add(contentPanel);
    }

    /**
     * Generic initialization
     */

    /**
     * Set the presenter for the concrete view
     * @param presenter
     */
    public void setPresenter(T presenter) {
        if (!initialized) {
            initialized = true;
            init();
        }
        this.presenter = presenter;
    }

    /**
     * Abstract Initilaization for the View
     */
    public abstract void init();

    /**
     * Set the header text for the HeaderPanel
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
     * @param child The child widget to add to the content panel
     */
    @Override
    public void add(Widget child) {
        contentPanel.add(child);
    }

    /**
     * Generic method to signal a failure to the user
     * @param message
     */
    public void onFailure(String message) {
        Window.alert("Error: " + message);
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
    private static class ContentContainerPanel extends FlowPanel {
        /**
         * Constructor for Content Panel
         * @param guiId The GUI Id for Content Panel
         */
        public ContentContainerPanel(String guiId) {
            super();
            getElement().setId(guiId);
        }
    }


}
