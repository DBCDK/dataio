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

package dk.dbc.dataio.gui.client.components.sortablelist;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.HasValue;

import java.util.Map;

public class SortableListWidget extends Composite implements HasValue<Map<String, String>> {
    interface SortableListWidgetUiBinder extends UiBinder<HTMLPanel, SortableListWidget> {
    }
    private static SortableListWidgetUiBinder ourUiBinder = GWT.create(SortableListWidgetUiBinder.class);

    protected SortableListModel model = null;

    @UiField FlowPanel list;


    /**
     * Constructor
     */
    public SortableListWidget() {
        initWidget(ourUiBinder.createAndBindUi(this));
        model = new SortableListModel(list);
    }


    /*
     * Implementations for the HasValue interface
     */

    /**
     * Fetches all items in the list in sorted order
     *
     * @return All items in the list in sorted order
     */
    @Override
    public Map<String, String> getValue() {
        return model.get();
    }

    /**
     * Clears all items from the list, and replaces it with the supplied items
     *
     * @param items     Items to set in the new list
     * @param fireEvent A boolean to determine, if an event is being fired upon change
     */
    @Override
    public void setValue(Map<String, String> items, boolean fireEvent) {
        model.put(items, fireEvent);
    }

    /**
     * Clears all items from the list, and replaces it with the supplied items
     *
     * @param items Items to set in the new list
     */
    @Override
    public void setValue(Map<String, String> items) {
        setValue(items, true);
    }

    /**
     * Adds a change handler to the list
     * Upon changes in the list, the associated handler will be activated
     *
     * @param changeHandler The Value Change Handler
     * @return A HandlerRegistration object
     */
    @Override
    public HandlerRegistration addValueChangeHandler(final ValueChangeHandler<Map<String, String>> changeHandler) {
        return model.addValueChangeHandler(changeHandler);
    }

}
