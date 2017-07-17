/*
 * DataIO - Data IO
 * Copyright (C) 2015 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
 * Denmark. CVR: 15149043
 *
 * This file is part of DataIO.
 *
 * DataIO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DataIO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DataIO.  If not, see <http://www.gnu.org/licenses/>.
 */


package dk.dbc.dataio.gui.client.views;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import dk.dbc.dataio.gui.client.i18n.MainConstants;
import dk.dbc.dataio.gui.client.presenters.GenericPresenter;

import java.util.Date;

/**
 * This is a container panel, for holding a Header panel together with
 * a content panel
 * @param <T> The presenter to use with this view
 */
public abstract class ContentPanel<T extends GenericPresenter> extends FlowPanel {
    protected final static MainConstants mainConstants = GWT.create(MainConstants.class);
    private static final String GUIID_HEADER_PANEL = "header-panel";
    private static final String GUIID_CONTENT_PANEL = "content-panel";
    public static final String GUID_LOG_PANEL = "log-panel";

    public T presenter;
    private boolean initialized = false;
    private final HeaderPanel headerPanel = new HeaderPanel(mainConstants.header_DataIO(), GUIID_HEADER_PANEL);
    private final ContentContainerPanel contentPanel = new ContentContainerPanel(GUIID_CONTENT_PANEL);
    public LogPanel logPanel = new LogPanel(GUID_LOG_PANEL);

    /**
     * Constructor
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
     * @param message the message to display in the view
     */
    public void setErrorText(String message) {
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
     * Log Panel Class
     */
    public class LogPanel extends Label {
        private StringBuilder logMessageBuilder = new StringBuilder();
        private int index = 0;

        /**
         * Constructor for Log Panel
         * @param guiId The GUI Id for Log Panel
         */
        public LogPanel(String guiId) {
            super();
            setupLogPanel(guiId);
        }

        public StringBuilder getLogMessageBuilder() {
            return logMessageBuilder;
        }

        public void setLogMessage() {
            setText(logMessageBuilder.insert(index, new Date() + ": ").append("\n").toString());
            index = logMessageBuilder.length();

        }

        public void clearLogMessage() {
            setText("");
            logMessageBuilder = new StringBuilder();
            index = 0;
        }

        /* private methods */
        private void setupLogPanel(String guiId) {
            Element element = getElement();
            element.setId(guiId);
            element.setPropertyObject(guiId, this);
            setSize("100%", "100%");
            element.getStyle().setWhiteSpace(Style.WhiteSpace.PRE);
            element.getStyle().setMargin(4, Style.Unit.PX);
            element.getStyle().setColor("darkslategray");
            setVisible(true);
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
