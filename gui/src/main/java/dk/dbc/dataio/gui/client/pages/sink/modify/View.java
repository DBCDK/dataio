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

package dk.dbc.dataio.gui.client.pages.sink.modify;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Label;
import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.gui.client.components.PromptedList;
import dk.dbc.dataio.gui.client.components.PromptedMultiList;
import dk.dbc.dataio.gui.client.components.PromptedPasswordTextBox;
import dk.dbc.dataio.gui.client.components.PromptedTextArea;
import dk.dbc.dataio.gui.client.components.PromptedTextBox;
import dk.dbc.dataio.gui.client.views.ContentPanel;

import java.util.ArrayList;
import java.util.Map;

public class View extends ContentPanel<Presenter> implements IsWidget {
    interface SinkBinder extends UiBinder<HTMLPanel, View> {}
    private static SinkBinder uiBinder = GWT.create(SinkBinder.class);

    public View() {
        super("");
        add(uiBinder.createAndBindUi(this));
    }

    @UiField PromptedList sinkTypeSelection;
    @UiField PromptedTextBox name;
    @UiField PromptedTextBox resource;
    @UiField PromptedTextArea description;
    @UiField HTMLPanel updateSinkSection;
    @UiField PromptedTextBox url;
    @UiField PromptedTextBox userid;
    @UiField PromptedPasswordTextBox password;
    @UiField PromptedMultiList queueProviders;
    @UiField Button deleteButton;
    @UiField Label status;

    @UiHandler("sinkTypeSelection")
    void sinkTypeSelectionChanged(ChangeEvent event) {
        switch (SinkContent.SinkType.valueOf(sinkTypeSelection.getSelectedKey())) {
            case OPENUPDATE:
                updateSinkSection.setVisible(true);
                break;
            default:
                updateSinkSection.setVisible(false);
                break;
        }
    }

    @UiHandler("sinkTypeSelection")
    @SuppressWarnings("unused")
    void setSinkTypeSelectionChanged(ValueChangeEvent<String> event) {
        presenter.sinkTypeChanged(sinkTypeSelection.getSelectedKey());
        presenter.keyPressed();
    }

    @UiHandler("name")
    @SuppressWarnings("unused")
    void nameChanged(ValueChangeEvent<String> event) {
        presenter.nameChanged(name.getText());
        presenter.keyPressed();
    }

    @UiHandler("resource")
    @SuppressWarnings("unused")
    void resourceChanged(ValueChangeEvent<String> event) {
        presenter.resourceChanged(resource.getText());
        presenter.keyPressed();
    }

    @UiHandler("description")
    @SuppressWarnings("unused")
    void descriptionChanged(ValueChangeEvent<String> event) {
        presenter.descriptionChanged(description.getText());
        presenter.keyPressed();
    }

    @UiHandler("url")
    @SuppressWarnings("unused")
    void urlChanged(ValueChangeEvent<String> event) {
        presenter.endpointChanged(url.getText());
        presenter.keyPressed();
    }

    @UiHandler("userid")
    @SuppressWarnings("unused")
    void useridChanged(ValueChangeEvent<String> event) {
        presenter.userIdChanged(userid.getText());
        presenter.keyPressed();
    }

    @UiHandler("password")
    @SuppressWarnings("unused")
    void passwordChanged(ValueChangeEvent<String> event) {
        presenter.passwordChanged(password.getText());
        presenter.keyPressed();
    }

    @UiHandler("queueProviders")
    void availableQueueProvidersChanged(ValueChangeEvent<Map<String, String>> event) {
        if (presenter != null) {
            presenter.queueProvidersChanged(new ArrayList<>(queueProviders.getValue().values()));
            presenter.keyPressed();
        }
    }

    @UiHandler("saveButton")
    @SuppressWarnings("unused")
    void saveButtonPressed(ClickEvent event) {
        presenter.saveButtonPressed();
    }

    @UiHandler("deleteButton")
    @SuppressWarnings("unused")
    void deleteButtonPressed(ClickEvent event) {
        presenter.deleteButtonPressed();
    }

    @UiHandler("queueProviders")
    void availableQueueProvidersButtonsClicked(ClickEvent event) {
        if (queueProviders.isAddEvent(event)) {
            presenter.queueProvidersAddButtonPressed();
        }
    }
}
