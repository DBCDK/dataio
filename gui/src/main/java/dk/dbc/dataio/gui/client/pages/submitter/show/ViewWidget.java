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

package dk.dbc.dataio.gui.client.pages.submitter.show;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Widget;
import dk.dbc.dataio.gui.client.components.popup.PopupListBox;
import dk.dbc.dataio.gui.client.events.DialogEvent;
import dk.dbc.dataio.gui.client.util.CommonGinjector;
import dk.dbc.dataio.gui.client.views.ContentPanel;

public abstract class ViewWidget extends ContentPanel<Presenter> implements IsWidget {

    CommonGinjector commonInjector = GWT.create(CommonGinjector.class);
    ViewGinjector viewInjector = GWT.create(ViewGinjector.class);

    // Instantiate UI Binder
    interface MyUiBinder extends UiBinder<Widget, ViewWidget> {}
    private static MyUiBinder uiBinder = GWT.create(MyUiBinder.class);

    // UI Fields
    @UiField Button createButton;
    @UiField CellTable submittersTable;
    @UiField PopupListBox popupList;

    /**
     * Default constructor
     * @param header Header text
     */
    public ViewWidget(String header) {
        super(header);
        setHeader(commonInjector.getMenuTexts().menu_Submitters());
        add(uiBinder.createAndBindUi(this));
    }

    /**
     * Ui Handler to catch click events on the create button
     * @param event Clicked event
     */
    @UiHandler("createButton")
    void backButtonPressed(ClickEvent event) {
        presenter.createSubmitter();
    }

    @UiHandler("popupList")
    void setPopupListButtonPressed(DialogEvent event) {
        if (event != null && event.getDialogButton() == DialogEvent.DialogButton.EXTRA_BUTTON) {
            // Assure, that all items in listBox are selected - only selected are returned in the call to getValue()
            ListBox listBox = popupList.getContentWidget();
            listBox.setMultipleSelect(true);
            int listBoxItems = listBox.getItemCount();
            for (int index=0; index<listBoxItems; index++) {
                listBox.setItemSelected(index, true);
            }
            presenter.copyFlowBinderListToClipboard(popupList.getValue());
        }
    }

    protected Texts getTexts() {
        return this.viewInjector.getTexts();
    }
}
