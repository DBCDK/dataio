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

package dk.dbc.dataio.gui.client.components.multilist;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiConstructor;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.HasValue;
import com.google.gwt.user.client.ui.PushButton;
import dk.dbc.dataio.gui.client.components.sortablelist.SortableList;

import java.util.Map;

public class MultiListWidget extends Composite implements HasValue<Map<String, String>>, HasClickHandlers {
    interface MultiListWidgetUiBinder extends UiBinder<HTMLPanel, MultiListWidget> {
    }

    private static MultiListWidgetUiBinder uiBinder = GWT.create(MultiListWidgetUiBinder.class);

    ClickHandler buttonClickHandler = null;

    @UiField SortableList list;
    @UiField PushButton removeButton;
    @UiField PushButton addButton;


    @UiConstructor
    public MultiListWidget() {
        initWidget(uiBinder.createAndBindUi(this));
    }

    /**
     * getValue fetches the all items in the list as a map of Key/Value pairs
     *
     * @return All items in the list
     */
    @Override
    public Map<String, String> getValue() {
        return list.getValue();
    }

    /**
     * setValue replaces all items in the list with the supplied map
     *
     * @param items The new map of list items
     */
    @Override
    public void setValue(Map<String, String> items) {
        list.setValue(items);
    }

    /**
     * setValue replaces all items in the list with the supplied map
     *
     * @param items     The new map of list items
     * @param fireEvent A boolean to determine, if an event is being fired upon change
     */
    @Override
    public void setValue(Map<String, String> items, boolean fireEvent) {
        list.setValue(items, fireEvent);
    }

    /**
     * Adds a ValueChangeHandler to the MultiList component, in order to be notified on changes
     *
     * @param valueChangeHandler The Value Change Handler
     * @return A HandlerRegistration object to be used, when removing the ValueChangeHandler
     */
    @Override
    public HandlerRegistration addValueChangeHandler(ValueChangeHandler<Map<String, String>> valueChangeHandler) {
        return list.addValueChangeHandler(valueChangeHandler);
    }

    /**
     * This method implements a Click Handler, to signal clicks on one of the buttons
     *
     * @param clickHandler The Click Handler
     * @return A HandlerRegistration
     */
    @Override
    public HandlerRegistration addClickHandler(final ClickHandler clickHandler) {
        buttonClickHandler = clickHandler;
        return new HandlerRegistration() {
            @Override
            public void removeHandler() {
                buttonClickHandler = null;
            }
        };
    }

    /**
     * Fire a ValueChange event
     *
     * @param gwtEvent The value of the ValueChange Event
     */
    @Override
    public void fireEvent(GwtEvent<?> gwtEvent) {
        list.fireEvent(gwtEvent);
    }


    /*
     * Ui Handlers
     */

    /**
     * UiHandler for Click Events
     *
     * @param event The associated click event
     */
    @UiHandler(value = {"addButton", "removeButton"})
    void handleButtonClickEvents(ClickEvent event) {
        if (buttonClickHandler != null) {
            buttonClickHandler.onClick(event);
        }
    }

}
