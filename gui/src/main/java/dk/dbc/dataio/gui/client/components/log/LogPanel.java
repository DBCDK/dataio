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

package dk.dbc.dataio.gui.client.components.log;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.Label;

import java.util.Date;

public class LogPanel extends Label {
    private StringBuilder logMessageBuilder;
    private int index = 0;
    private static final String GUID_LOG_PANEL = "log-panel";

    public LogPanel() {
        super();
        setup();
        logMessageBuilder = new StringBuilder();
    }

    public void show(String message) {
        logMessageBuilder.append(message);
        setLogMessage();
    }

    public void clear() {
        setText("");
        logMessageBuilder = new StringBuilder();
        index = 0;
    }

    private void setLogMessage() {
        setText(logMessageBuilder.insert(index, new Date() + ": ").append("\n").toString());
        index = logMessageBuilder.length();

    }

    /* private methods */
    private void setup() {
        Element element = getElement();
        element.setId(GUID_LOG_PANEL);
        element.setPropertyObject(GUID_LOG_PANEL, this);
        setSize("100%", "100%");
        element.getStyle().setWhiteSpace(Style.WhiteSpace.PRE);
        element.getStyle().setMargin(4, Style.Unit.PX);
        element.getStyle().setColor("darkslategray");
        setVisible(true);
    }
}
