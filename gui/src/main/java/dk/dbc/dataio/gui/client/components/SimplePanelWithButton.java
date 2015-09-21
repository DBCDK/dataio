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

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiConstructor;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

import java.util.Iterator;

/**
 * The SimplePanelWithButton component implements a SimplePanel, with a button added to the right of the panel:
 * <pre>
 * {@code
 * +----------------------------------------------------+
 * | Panel content...                           +-----+ |
 * |                                            | btn | |
 * |                                            +-----+ |
 * +----------------------------------------------------+
 * }
 *
 * In UiBinder, the component is used as follows:
 *
 * {@code
 * <ui:with field="img" type="dk.dbc.dataio.gui.client.resources.Resources"/>
 * ...
 * <dio:SimplePanelWithButton buttonImage="{img.deleteButton}">
 *    <g:Label>Panel content...</g:Label>
 * </dio:SimplePanelWithButton>
 * }</pre>
 */
public class SimplePanelWithButton extends Composite implements HasWidgets, HasClickHandlers {
    interface SimplePanelWithButtonUiBinder extends UiBinder<HTMLPanel, SimplePanelWithButton> {
    }

    private static SimplePanelWithButtonUiBinder ourUiBinder = GWT.create(SimplePanelWithButtonUiBinder.class);

    @UiField PushButton button;
    protected @UiField SimplePanel content;

    /**
     * Default constructor, taking the button image as a parameter
     * @param buttonImage The image to be shown on the button
     */
    @UiConstructor
    public SimplePanelWithButton(ImageResource buttonImage) {
        initWidget(ourUiBinder.createAndBindUi(this));
        this.button.getUpFace().setImage(new Image(buttonImage));
    }

    /**
     * Adds a widget to the panel
     * @param widget The widget to add to the panel
     */
    @Override
    public void add(Widget widget) {
        this.content.add(widget);
    }

    /**
     * Clears all widgets from the panel
     */
    @Override
    public void clear() {
        this.content.clear();
    }

    /**
     * Gets an iterator for the contained widgets.
     * @return The iterator for the contained widgets.
     */
    @Override
    public Iterator<Widget> iterator() {
        return this.content.iterator();
    }

    /**
     * Removes a widget from this panel
     * @param widget The widget to be removed
     * @return True if the widget was present
     */
    @Override
    public boolean remove(Widget widget) {
        return this.content.remove(widget);
    }

    /**
     * Adds a click handler
     * @param clickHandler The click handler to add
     * @return The Handler Registration object
     */
    @Override
    public HandlerRegistration addClickHandler(ClickHandler clickHandler) {
        return button.addClickHandler(clickHandler);
    }

    /**
     * Sets the button image
     * @param buttonImage The image to be shown on the button
     */
    public void setButtonImage(ImageResource buttonImage) {
        this.button.getUpFace().setImage(new Image(buttonImage));
    }

}