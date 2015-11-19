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

package dk.dbc.dataio.gui.client.pages.item.show;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.DecoratedTabPanel;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import dk.dbc.dataio.gui.client.views.ContentPanel;

public class ViewWidget extends ContentPanel<Presenter> implements IsWidget {
    // Please do note, that this list of sequential numbers are maintained manually
    // They must follow the order, given in the UI Binder file ViewWidget.ui.xml
    static final int ALL_ITEMS_TAB_INDEX = 0;
    static final int FAILED_ITEMS_TAB_INDEX = 1;
    static final int IGNORED_ITEMS_TAB_INDEX = 2;
    static final int JOB_INFO_TAB_CONTENT = 3;
    static final int JOB_DIAGNOSTIC_TAB_CONTENT = 4;
    static final int JOB_NOTIFICATION_TAB_CONTENT = 5;

    interface ViewUiBinder extends UiBinder<Widget, ViewWidget> {}

    private static ViewUiBinder uiBinder = GWT.create(ViewUiBinder.class);

    @UiField Label jobHeader;
    @UiField DecoratedTabPanel tabPanel;
    @UiField ItemsListView allItemsList;
    @UiField ItemsListView failedItemsList;
    @UiField ItemsListView ignoredItemsList;
    @UiField JobInfoTabContent jobInfoTabContent;
    @UiField JobDiagnosticTabContent jobDiagnosticTabContent;
    @UiField JobNotificationsTabContent jobNotificationsTabContent;


    /**
     * Constructor with header and text
     * @param header    Breadcrumb header text
     */
    public ViewWidget(String header) {
        super(header);
        add(uiBinder.createAndBindUi(this));
        allItemsList.itemsPager.firstPage();
        failedItemsList.itemsPager.firstPage();
        ignoredItemsList.itemsPager.firstPage();
    }

    /**
     * Ui Handler to catch selection events on the tabs in the tab panel
     * @param event Selected event
     */
    @UiHandler("tabPanel")
    void tabPanelSelection(SelectionEvent<Integer> event) {
        switch(event.getSelectedItem()) {
            case ALL_ITEMS_TAB_INDEX:
                presenter.allItemsTabSelected();
                break;
            case FAILED_ITEMS_TAB_INDEX:
                presenter.failedItemsTabSelected();
                break;
            case IGNORED_ITEMS_TAB_INDEX:
                presenter.ignoredItemsTabSelected();
                break;
        }
    }

}

