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
import com.google.gwt.uibinder.client.UiFactory;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.gui.client.components.PopupBox;
import dk.dbc.dataio.gui.client.components.PopupValueBox;
import dk.dbc.dataio.gui.client.components.PromptedList;
import dk.dbc.dataio.gui.client.components.PromptedMultiList;
import dk.dbc.dataio.gui.client.components.PromptedPasswordTextBox;
import dk.dbc.dataio.gui.client.components.PromptedRadioButtons;
import dk.dbc.dataio.gui.client.components.PromptedTextArea;
import dk.dbc.dataio.gui.client.components.PromptedTextBox;
import dk.dbc.dataio.gui.client.events.DialogEvent;
import dk.dbc.dataio.gui.client.views.ContentPanel;

import java.util.ArrayList;
import java.util.Map;

public class View extends ContentPanel<Presenter> implements IsWidget {
    interface SinkBinder extends UiBinder<HTMLPanel, View> {}
    private static SinkBinder uiBinder = GWT.create(SinkBinder.class);
    private ViewGinjector viewInjector = GWT.create(ViewGinjector.class);

    public View() {
        super("");
        add(uiBinder.createAndBindUi(this));
    }

    @UiFactory
    PopupBox<Label> getPopupBox() {
        return new PopupBox<>(new Label(viewInjector.getTexts().label_AreYouSureAboutDeleting()), "", "");
    }

    @UiFactory
    PopupValueBox<TextBox, String> getPopupValueBox() {
        return new PopupValueBox<>(new TextBox(), "", "");
    }

    @UiField PromptedList sinkTypeSelection;
    @UiField PromptedTextBox name;
    @UiField PromptedTextBox resource;
    @UiField PromptedTextArea description;
    @UiField HTMLPanel sequenceAnalysisSection;
    @UiField HTMLPanel updateSinkSection;
    @UiField HTMLPanel esSinkSection;
    @UiField HTMLPanel imsSinkSection;
    @UiField PromptedTextBox url;
    @UiField PromptedTextBox openupdateuserid;
    @UiField PromptedPasswordTextBox password;
    @UiField PromptedMultiList queueProviders;
    @UiField PromptedTextBox esUserId;
    @UiField PromptedTextBox esDatabase;
    @UiField PromptedTextBox imsEndpoint;
    @UiField Button deleteButton;
    @UiField Label status;
    @UiField PopupValueBox popupTextBox;
    @UiField PromptedRadioButtons sequenceAnalysisSelection;
    @UiField PopupBox<Label> confirmation;

    @SuppressWarnings("unused")
    @UiHandler("sinkTypeSelection")
    void sinkTypeSelectionChanged(ValueChangeEvent<String> event) {
        presenter.sinkTypeChanged(SinkContent.SinkType.valueOf(sinkTypeSelection.getSelectedKey()));
        presenter.keyPressed();
    }

    @SuppressWarnings("unused")
    @UiHandler("name")
    void nameChanged(ValueChangeEvent<String> event) {
        presenter.nameChanged(name.getText());
        presenter.keyPressed();
    }

    @SuppressWarnings("unused")
    @UiHandler("resource")
    void resourceChanged(ValueChangeEvent<String> event) {
        presenter.resourceChanged(resource.getText());
        presenter.keyPressed();
    }

    @SuppressWarnings("unused")
    @UiHandler("description")
    void descriptionChanged(ValueChangeEvent<String> event) {
        presenter.descriptionChanged(description.getText());
        presenter.keyPressed();
    }

    @SuppressWarnings("unused")
    @UiHandler("url")
    void urlChanged(ValueChangeEvent<String> event) {
        presenter.endpointChanged(url.getText());
        presenter.keyPressed();
    }

    @SuppressWarnings("unused")
    @UiHandler("openupdateuserid")
    void useridChanged(ValueChangeEvent<String> event) {
        presenter.openUpdateUserIdChanged(openupdateuserid.getText());
        presenter.keyPressed();
    }

    @SuppressWarnings("unused")
    @UiHandler("password")
    void passwordChanged(ValueChangeEvent<String> event) {
        presenter.passwordChanged(password.getText());
        presenter.keyPressed();
    }

    @SuppressWarnings("unused")
    @UiHandler("queueProviders")
    void availableQueueProvidersChanged(ValueChangeEvent<Map<String, String>> event) {
        if (presenter != null) {
            presenter.queueProvidersChanged(new ArrayList<>(queueProviders.getValue().values()));
            presenter.keyPressed();
        }
    }

    @SuppressWarnings("unused")
    @UiHandler("esUserId")
    void esUserIdChanged(ValueChangeEvent<String> event) {
        presenter.esUserIdChanged(esUserId.getText());
        presenter.keyPressed();
    }

    @SuppressWarnings("unused")
    @UiHandler("esDatabase")
    void esDatabaseChanged(ValueChangeEvent<String> event) {
        presenter.esDatabaseChanged(esDatabase.getText());
        presenter.keyPressed();
    }

    @SuppressWarnings("unused")
    @UiHandler("imsEndpoint")
    void imsEndpointChanged(ValueChangeEvent<String> event) {
        presenter.imsEndpointChanged(imsEndpoint.getText());
        presenter.keyPressed();
    }

    @SuppressWarnings("unused")
    @UiHandler("saveButton")
    void saveButtonPressed(ClickEvent event) {
        presenter.saveButtonPressed();
    }

    @SuppressWarnings("unused")
    @UiHandler("deleteButton")
    void deleteButtonPressed(ClickEvent event) {
        confirmation.show();
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
    void popupTextBoxChanged(DialogEvent event) {
        if (event.getDialogButton() == DialogEvent.DialogButton.OK_BUTTON) {
            Map<String, String> list = queueProviders.getValue();
            list.put((String)popupTextBox.getValue(), (String)popupTextBox.getValue());
            queueProviders.setValue(list, true);
        }
    }

    @UiHandler("confirmation")
    void confirmationButtonClicked(DialogEvent event) {
        if (event.getDialogButton() == DialogEvent.DialogButton.OK_BUTTON) {
            presenter.deleteButtonPressed();
        }
    }
}
