/*
 * DataIO - Data IO
 * Copyright (C) 2018 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
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

package dk.dbc.dataio.gui.client.pages.failedftps.show;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.view.client.ListDataProvider;
import dk.dbc.dataio.gui.client.util.Format;
import dk.dbc.dataio.jobstore.types.InvalidTransfileNotificationContext;
import dk.dbc.dataio.jobstore.types.Notification;

import java.util.List;

/**
 * Gatekeepers Table for the Failed Ftps View
 */
public class FailedFtpsTable extends CellTable {
    ViewGinjector viewGinjector = GWT.create(ViewGinjector.class);
    Texts texts = viewGinjector.getTexts();
    View view;
    Presenter presenter = null;
    ListDataProvider<Notification> dataProvider;

    /**
     * Constructor
     *
     * @param view The owner view for this Gatekeeper Table
     */
    public FailedFtpsTable(View view) {
        this.view = view;
        dataProvider = new ListDataProvider<>();
        dataProvider.addDataDisplay(this);

        addColumn(constructDateColumn(), texts.label_HeaderDate());
        addColumn(constructTransFileColumn(), texts.label_HeaderTransFile());
        addColumn(constructMailColumn(), texts.label_HeaderMail());
    }


    /**
     * Sets the presenter to allow communication back to the presenter
     * @param presenter The presenter to set
     */
    public void setPresenter(Presenter presenter) {
        this.presenter = presenter;
    }


    /**
     * Puts data into the view
     *
     * @param notifications The list of Failed FTP Notifications to put into the view
     */
    public void setNotifications(List<Notification> notifications) {
        dataProvider.getList().clear();
        dataProvider.getList().addAll(notifications);
    }


    /*
     * Local methods
     */

    private Column constructDateColumn() {
        return new TextColumn<Notification>() {
            @Override
            public String getValue(Notification notification) {
                return Format.formatLongDate(notification.getTimeOfCreation());
            }
        };
    }

    private Column constructTransFileColumn() {
        return new TextColumn<Notification>() {
            @Override
            public String getValue(Notification notification) {
                InvalidTransfileNotificationContext context = (InvalidTransfileNotificationContext) notification.getContext();
                return context.getTransfileName();
            }
        };
    }

    private Column constructMailColumn() {
        return new TextColumn<Notification>() {
            @Override
            public String getValue(Notification notification) {
                return Format.capitalize(notification.getStatus().toString());
            }
        };
    }

}
