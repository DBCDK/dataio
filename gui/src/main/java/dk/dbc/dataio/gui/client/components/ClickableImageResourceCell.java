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

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.cell.client.ValueUpdater;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.ui.ImageResourceRenderer;


public class ClickableImageResourceCell extends AbstractCell<ImageResource> {
    public static ImageResourceRenderer renderer;

    public ClickableImageResourceCell() {
        super("click");
        if (renderer == null) {
            renderer = new ImageResourceRenderer();
        }
    }

    @Override
    public void onBrowserEvent(Context context, final Element parent, ImageResource value, NativeEvent event, ValueUpdater<ImageResource> valueUpdater) {
        super.onBrowserEvent(context, parent, value, event, valueUpdater);
        if (event.getType().equals("click") && valueUpdater != null) {
            valueUpdater.update(value);
        }
    }

    @Override
    public void render(com.google.gwt.cell.client.Cell.Context context, ImageResource value, final SafeHtmlBuilder sb) {
        sb.append(renderer.render(value));
    }
}
