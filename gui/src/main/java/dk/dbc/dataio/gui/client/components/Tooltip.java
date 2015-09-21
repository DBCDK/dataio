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

import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.user.client.ui.FocusWidget;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PopupPanel;

public class Tooltip extends PopupPanel implements MouseOverHandler, MouseOutHandler {

    private final FocusWidget widget;

    public Tooltip(FocusWidget widget, String text) {
        super(true);
        Label l = new Label(text);
        l.setWidth("300px"); // move to css
        l.setWordWrap(true); // move to css
        setWidget(l);
        this.widget = widget;
        widget.addMouseOverHandler(this);
        widget.addMouseOutHandler(this);
    }

    @Override
    public void onMouseOver(MouseOverEvent event) {
        int x = widget.getElement().getAbsoluteRight();
        int y = widget.getElement().getAbsoluteTop();
        setPopupPosition(x, y);
        show();
    }

    @Override
    public void onMouseOut(MouseOutEvent event) {
        hide();
    }
}
