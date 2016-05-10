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
import dk.dbc.dataio.gui.client.components.PopupTextBox;
import dk.dbc.dataio.gui.client.components.PromptedList;
import dk.dbc.dataio.gui.client.components.PromptedMultiList;
import dk.dbc.dataio.gui.client.components.PromptedPasswordTextBox;
import dk.dbc.dataio.gui.client.components.PromptedRadioButtons;
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
    @UiField PopupTextBox popupTextBox;
    @UiField PromptedRadioButtons sequenceAnalysisSelection;

    @UiHandler("sinkTypeSelection")
    void sinkTypeSelectionChanged(ValueChangeEvent<String> event) {
        String selectedKey = sinkTypeSelection.getSelectedKey();
        switch (SinkContent.SinkType.valueOf(selectedKey)) {
            case OPENUPDATE:
                updateSinkSection.setVisible(true);
                break;
            default:
                updateSinkSection.setVisible(false);
                break;
        }
        presenter.sinkTypeChanged(selectedKey);
        presenter.keyPressed();
    }

    @UiHandler("name")
    void nameChanged(ValueChangeEvent<String> event) {
        presenter.nameChanged(name.getText());
        presenter.keyPressed();
    }

    @UiHandler("resource")
    void resourceChanged(ValueChangeEvent<String> event) {
        presenter.resourceChanged(resource.getText());
        presenter.keyPressed();
    }

    @UiHandler("description")
    void descriptionChanged(ValueChangeEvent<String> event) {
        presenter.descriptionChanged(description.getText());
        presenter.keyPressed();
    }

    @UiHandler("url")
    void urlChanged(ValueChangeEvent<String> event) {
        presenter.endpointChanged(url.getText());
        presenter.keyPressed();
    }

    @UiHandler("userid")
    void useridChanged(ValueChangeEvent<String> event) {
        presenter.userIdChanged(userid.getText());
        presenter.keyPressed();
    }

    @UiHandler("password")
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
    void saveButtonPressed(ClickEvent event) {
        presenter.saveButtonPressed();
    }

    @UiHandler("deleteButton")
    void deleteButtonPressed(ClickEvent event) {
        presenter.deleteButtonPressed();
    }

    @UiHandler("queueProviders")
    void availableQueueProvidersButtonClicked(ClickEvent event) {
        if (queueProviders.isAddEvent(event)) {
            presenter.queueProvidersAddButtonPressed();
        }
    }

    @UiHandler("sequenceAnalysisSelection")
    void sequenceAnalysisSelectionChanged(ValueChangeEvent<String> event) {
        presenter.sequenceAnalysisSelectionChanged(event.getValue());
    }

    @UiHandler("popupTextBox")
    void popupTextBoxChanged(ValueChangeEvent<String> event) {
        Map<String, String> list = queueProviders.getValue();
        list.put(event.getValue(), event.getValue());
        queueProviders.setValue(list, true);
    }
}
