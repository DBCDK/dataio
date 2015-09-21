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

package dk.dbc.dataio.gui.client.components;

import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;


public class DualPanesPanel extends FlowPanel {

    public static final String DUAL_PANES_PANEL_CLASS = "dio-DualPanesPanel";
    public static final String DUAL_PANES_PANEL_WIDGET_LEFT_CLASS = "dio-DualPanesPanel-WidgetLeftClass";
    public static final String DUAL_PANES_PANEL_WIDGET_RIGHT_CLASS = "dio-DualPanesPanel-WidgetRightClass";

    public DualPanesPanel(String guiId){
        getElement().setId(guiId);
        setStylePrimaryName(DUAL_PANES_PANEL_CLASS);
    }

    public void setDualPanesPanelWidgets (Widget widgetLeft, Widget widgetRight) {
        setStylePrimaryName(DUAL_PANES_PANEL_CLASS);
        widgetLeft.setStylePrimaryName(DUAL_PANES_PANEL_WIDGET_LEFT_CLASS);
        add(widgetLeft);
        widgetRight.setStylePrimaryName(DUAL_PANES_PANEL_WIDGET_RIGHT_CLASS);
        add(widgetRight);
    }
}
