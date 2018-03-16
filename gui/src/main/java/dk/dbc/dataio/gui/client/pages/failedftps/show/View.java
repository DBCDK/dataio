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
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.IsWidget;
import dk.dbc.dataio.gui.client.components.popup.PopupValueBox;
import dk.dbc.dataio.gui.client.events.DialogEvent;
import dk.dbc.dataio.gui.client.views.ContentPanel;
import dk.dbc.dataio.jobstore.types.Notification;

import java.util.List;


public class View extends ContentPanel<Presenter> implements IsWidget {
    interface UiTrafficBinder extends UiBinder<HTMLPanel, View> {
    }

    private static UiTrafficBinder uiBinder = GWT.create(UiTrafficBinder.class);

    @UiField(provided=true) FailedFtpsTable failedFtpsTable;

    @UiField(provided=true) PopupValueBox<EditTransFileView, EditTransFileView.EditTransFileData> editTransFilePopup;


    public View() {
        super("");
        failedFtpsTable = new FailedFtpsTable(this);
        editTransFilePopup = new PopupValueBox(new EditTransFileView());
        add(uiBinder.createAndBindUi(this));
    }

    @Override
    public void init() {
        failedFtpsTable.setPresenter(presenter);
    }


    /*
     * Ui Handlers
     */

    @UiHandler("editTransFilePopup")
    public void resendFtp(DialogEvent event) {
        if (event.getDialogButton() == DialogEvent.DialogButton.OK_BUTTON) {
            if (presenter != null) {
                presenter.resendFtp(editTransFilePopup.getValue().name, editTransFilePopup.getValue().content);
            }
            editTransFilePopup.hide();
        }
    }


    /*
     * Public methods
     */

    /**
     * Displays a warning to the user
     * @param warning The warning to display
     */
    void displayWarning(String warning) {
        Window.alert(warning);
    }

    /**
     * This method is used to put data into the view
     *
     * @param notifications The list of Failed Ftp notifications to put into the view
     */
    void setNotifications(List<Notification> notifications) {
        failedFtpsTable.setNotifications(notifications);
    }

    /**
     * This method show the Failed Ftp's Popup box with the values given in the parameter list
     * @param transfileName The name of the Transfile
     * @param transfileContent The content of the Transfile
     * @param mailContent The content of the Mail, sent to the user
     */
    void showFailedFtp(String transfileName, String transfileContent, String mailContent) {
        editTransFilePopup.show();
        editTransFilePopup.setValue(new EditTransFileView().new EditTransFileData(transfileName, transfileContent, mailContent));
    }
}

