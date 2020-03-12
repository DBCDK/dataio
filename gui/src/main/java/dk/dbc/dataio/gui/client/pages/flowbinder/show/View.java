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

package dk.dbc.dataio.gui.client.pages.flowbinder.show;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Widget;
import dk.dbc.dataio.gui.client.components.flowbinderfilter.FlowBinderFilter;
import dk.dbc.dataio.gui.client.components.popup.PopupListBox;
import dk.dbc.dataio.gui.client.views.ContentPanel;

public class View extends ContentPanel<Presenter> implements IsWidget {
    interface MyUiBinder extends UiBinder<Widget, View> {}
    private static MyUiBinder uiBinder = GWT.create(MyUiBinder.class);

    // UI Fields
    @UiField(provided=true) FlowBindersTable flowBindersTable;
    @UiField PopupListBox popupList;
    @UiField FlowBinderFilter flowBinderFilter;

    /**
     * Default constructor
     */
    public View() {
        this("");
    }

    /**
     * Constructor
     * @param header    Breadcrumb header text
     */
    public View(String header) {
        super(header);
        flowBindersTable = new FlowBindersTable(this);
        add(uiBinder.createAndBindUi(this));
    }

    @Override
    public void init() {
        flowBindersTable.setPresenter(presenter);
    }

    /**
     * Ui Handler to catch click events on the create button
     * @param event Clicked event
     */
    @UiHandler("createButton")
    void createButtonPressed(ClickEvent event) {
        presenter.createFlowBinder();
    }

    @UiHandler("flowBinderFilter")
    @SuppressWarnings("unused")
    void flowBinderFilterChanged(ChangeEvent event) {
    }
}
