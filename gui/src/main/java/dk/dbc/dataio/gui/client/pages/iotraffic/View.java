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

package dk.dbc.dataio.gui.client.pages.iotraffic;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.IsWidget;
import dk.dbc.dataio.commons.types.GatekeeperDestination;
import dk.dbc.dataio.gui.client.components.EnterButton;
import dk.dbc.dataio.gui.client.components.PromptedCheckBox;
import dk.dbc.dataio.gui.client.components.PromptedTextBox;
import dk.dbc.dataio.gui.client.views.ContentPanel;

import java.util.List;


public class View extends ContentPanel<Presenter> implements IsWidget {
    interface UiTrafficBinder extends UiBinder<HTMLPanel, View> {
    }

    private static UiTrafficBinder uiBinder = GWT.create(UiTrafficBinder.class);

    @UiField PromptedTextBox submitter;
    @UiField PromptedTextBox packaging;
    @UiField PromptedTextBox format;
    @UiField PromptedTextBox destination;
    @UiField PromptedCheckBox copy;
    @UiField PromptedCheckBox notify;
    @UiField EnterButton addButton;
    @UiField(provided=true) GatekeepersTable gatekeepersTable;


    public View() {
        super("");
        gatekeepersTable = new GatekeepersTable();
        add(uiBinder.createAndBindUi(this));
    }

    @UiHandler("submitter")
    void submitterChanged(ValueChangeEvent<String> event) {
        presenter.submitterChanged(submitter.getText());
    }

    @UiHandler("packaging")
    void packaginChanged(ValueChangeEvent<String> event) {
        presenter.packagingChanged(packaging.getText());
    }

    @UiHandler("format")
    void formatChanged(ValueChangeEvent<String> event) {
        presenter.formatChanged(format.getText());
    }

    @UiHandler("destination")
    void destinationChanged(ValueChangeEvent<String> event) {
        presenter.destinationChanged(destination.getText());
    }

    @UiHandler("copy")
    void copyChanged(ValueChangeEvent<Boolean> event) {
        presenter.copyChanged(copy.getValue());
    }

    @UiHandler("notify")
    void notifyChanged(ValueChangeEvent<Boolean> event) {
        presenter.notifyChanged(notify.getValue());
    }

    @UiHandler("addButton")
    void addButtonPressed(ClickEvent event) {
        presenter.addButtonPressed();
    }

    /**
     * Displays a warning to the user
     * @param warning The warning to display
     */
    public void displayWarning(String warning) {
        Window.alert(warning);
    }

    /**
     * This method is used to put data into the view
     *
     * @param gatekeepers The list of gatekeepers to put into the view
     */
    public void setGatekeepers(List<GatekeeperDestination> gatekeepers) {
        gatekeepersTable.setGatekeepers(gatekeepers);
    }

}

