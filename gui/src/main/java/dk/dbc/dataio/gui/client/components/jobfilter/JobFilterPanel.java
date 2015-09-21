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

package dk.dbc.dataio.gui.client.components.jobfilter;

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
import com.google.gwt.user.client.ui.Widget;
import dk.dbc.dataio.gui.client.components.SimplePanelWithButton;
import dk.dbc.dataio.gui.client.components.TitledDecoratorPanel;

import java.util.Iterator;

/**
 * This class combines the TitledDecoratorPanel and the SimplePanelWithButton panels
 *
 * <pre>
 * {@code
 * +- Panel Title --------------------------------------+
 * | Panel Content                              +-----+ |
 * |                                            | btn | |
 * |                                            +-----+ |
 * +----------------------------------------------------+
}
 *
 * In UiBinder, the component is used as follows:
 *
 * {@code
 * <ui:with field="img" type="dk.dbc.dataio.gui.client.resources.Resources"/>
 * ...
 * <dio:JobFilterPanel title="Panel Title" buttonImage="{img.deleteButton}">
 *    <g:Label>Panel content...</g:Label>
 * </dio:JobFilterPanel>
 * }</pre>
 */
public class JobFilterPanel extends Composite implements HasWidgets, HasClickHandlers/*, HasChangeHandlers*/ {
    interface TitledJobFilterPanelUiBinder extends UiBinder<HTMLPanel, JobFilterPanel> {
    }

    private static TitledJobFilterPanelUiBinder ourUiBinder = GWT.create(TitledJobFilterPanelUiBinder.class);

    public JobFilterPanel() {
        initWidget(ourUiBinder.createAndBindUi(this));
    }

    @UiField TitledDecoratorPanel decorator;
    @UiField SimplePanelWithButton panel;

    /**
     * Constructor taking the title of the panel and the button image as parameters (mandatory in UI Binder)
     * @param title The title of the Decorated panel
     * @param buttonImage the button image
     */
    @UiConstructor
    public JobFilterPanel(String title, ImageResource buttonImage) {
        initWidget(ourUiBinder.createAndBindUi(this));
        setTitle(title);
        this.panel.setButtonImage(buttonImage);
    }

    /**
     * Adds a widget to the TitledDecoratorPanel
     * @param widget The widget to add
     */
    @Override
    public void add(Widget widget) {
        this.panel.add(widget);
    }

    /**
     * Clears all widgets from the TitledDecoratorPanel
     */
    @Override
    public void clear() {
        this.panel.clear();
    }

    /**
     * Gets an iterator for the contained widgets.
     * @return The iterator for the contained widgets.
     */
    @Override
    public Iterator<Widget> iterator() {
        return this.panel.iterator();
    }

    /**
     * Removes a widget in the TitledDecoratorPanel
     * @param widget The widget to be removed
     * @return True if the widget was present
     */
    @Override
    public boolean remove(Widget widget) {
        return this.panel.remove(widget);
    }

    /**
     * Adds a click handler
     * @param clickHandler The click handler to add
     * @return The Handler Registration object
     */
    @Override
    public HandlerRegistration addClickHandler(ClickHandler clickHandler) {
        return panel.addClickHandler(clickHandler);
    }

    /**
     * Sets the title of the TitledDecoratorPanel
     * @param title The title of the TitledDecoratorPanel
     */
    public void setTitle(String title) {
        this.decorator.setTitle(title);
    }

    /**
     * Gets the title of the TitledDecoratorPanel
     * @return The title of the TitledDecoratorPanel
     */
    public String getTitle() {
        return this.decorator.getTitle();
    }


}