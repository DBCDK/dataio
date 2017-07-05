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
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiConstructor;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import dk.dbc.dataio.gui.client.resources.Resources;

import java.util.Iterator;

/**
 * This class implements a SimplePanel, with a deleteButton added to the right of the panel:
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
 * <dio:JobFilterPanel title="Panel Title">
 *    <g:Label>Panel content...</g:Label>
 * </dio:JobFilterPanel>
 * }</pre>
 *
 */
public class JobFilterPanel extends Composite implements HasWidgets, HasClickHandlers {
    interface TitledJobFilterPanelUiBinder extends UiBinder<HTMLPanel, JobFilterPanel> {
    }
    protected Resources resources;

    private static TitledJobFilterPanelUiBinder ourUiBinder = GWT.create(TitledJobFilterPanelUiBinder.class);

    @UiField PushButton includeExcludeButton;
    @UiField PushButton deleteButton;
    @UiField SimplePanel content;

    private Boolean includeFilter = true;

    /**
     * Constructor taking the title of the panel and the deleteButton image as parameters (mandatory in UI Binder)
     * @param title The title of the panel
     * @param resources the resource for the panel
     * @param includeFilter True if filter is an Include filter, false if the filter is an Exclude filter
     */
    @UiConstructor
    JobFilterPanel(String title, Resources resources, boolean includeFilter) {
        initWidget(ourUiBinder.createAndBindUi(this));
        setTitle(title);
        this.resources = resources;
        this.includeFilter = includeFilter;
        setDeleteButtonImage(resources);
        setIncludeExcludeButtonImage(resources, this.includeFilter);
    }

    @UiHandler("includeExcludeButton")
    void setIncludeExcludeButtonClicked(ClickEvent event) {
        includeFilter = ! includeFilter;
        setIncludeExcludeButtonImage(resources, includeFilter);
    }

    /**
     * Test whether this is an Include or an Exclude filter.
     * @return True if the filter is an Include filter, false if it is an Exclude filter
     */
    public boolean isIncludeFilter() {
        return includeFilter;
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
        return deleteButton.addClickHandler(clickHandler);
    }

    /**
     * Sets the delete deleteButton image
     * @param resources The resources to be used for fetching the deleteButton images
     */
    private void setDeleteButtonImage(Resources resources) {
        this.deleteButton.getUpFace().setImage(new Image(resources.deleteUpButton()));
        this.deleteButton.getDownFace().setImage(new Image(resources.deleteDownButton()));
    }

    /**
     * Sets the include/exclude deleteButton image
     * @param resources The resources to be used for fetching the deleteButton images
     * @param include Determines whether to show an include deleteButton (true) or exclude deleteButton (false)
     */
    private void setIncludeExcludeButtonImage(Resources resources, boolean include) {
        if (include) {
            this.includeExcludeButton.getUpFace().setImage(new Image(resources.plusUpButton()));
            this.includeExcludeButton.getDownFace().setImage(new Image(resources.plusDownButton()));
        } else {
            this.includeExcludeButton.getUpFace().setImage(new Image(resources.minusUpButton()));
            this.includeExcludeButton.getDownFace().setImage(new Image(resources.minusDownButton()));
        }
    }

}