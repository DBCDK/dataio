/*
 *
 *  * DataIO - Data IO
 *  * Copyright (C) 2015 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
 *  * Denmark. CVR: 15149043
 *  *
 *  * This file is part of DataIO.
 *  *
 *  * DataIO is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * DataIO is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with DataIO.  If not, see <http://www.gnu.org/licenses/>.
 *
 *
 */

package dk.dbc.dataio.gui.client.pages.harvester.corepo.modify;

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
import dk.dbc.dataio.gui.client.components.popup.PopupBox;
import dk.dbc.dataio.gui.client.components.prompted.PromptedCheckBox;
import dk.dbc.dataio.gui.client.components.prompted.PromptedList;
import dk.dbc.dataio.gui.client.components.prompted.PromptedTextArea;
import dk.dbc.dataio.gui.client.components.prompted.PromptedTextBox;
import dk.dbc.dataio.gui.client.events.DialogEvent;
import dk.dbc.dataio.gui.client.views.ContentPanel;

public class View extends ContentPanel<Presenter> implements IsWidget {
    interface HarvesterBinder extends UiBinder<HTMLPanel, View> {}
    private static HarvesterBinder uiBinder = GWT.create(HarvesterBinder.class);
    private ViewGinjector viewInjector = GWT.create(ViewGinjector.class);

    public View() {
        super("");
        add(uiBinder.createAndBindUi(this));
    }


    @UiFactory
    PopupBox<Label> getPopupBox() {
        return new PopupBox<>(new Label(viewInjector.getTexts().label_AreYouSureAboutDeleting()), "", "");
    }


    @UiField PromptedTextBox name;
    @UiField PromptedTextArea description;
    @UiField PromptedTextBox resource;
    @UiField PromptedList rrHarvester;
    @UiField PromptedCheckBox enabled;
    @UiField Button saveButton;
    @UiField Button deleteButton;
    @UiField Label status;
    @UiField PopupBox<Label> confirmation;


    @SuppressWarnings("unused")
    @UiHandler("name")
    void nameChanged(ValueChangeEvent<String> event) {
        presenter.nameChanged(name.getText());
        presenter.keyPressed();
    }

    @SuppressWarnings("unused")
    @UiHandler("description")
    void descriptionChanged(ValueChangeEvent<String> event) {
        presenter.descriptionChanged(description.getText());
        presenter.keyPressed();
    }

    @SuppressWarnings("unused")
    @UiHandler("resource")
    void resourceChanged(ValueChangeEvent<String> event) {
        presenter.resourceChanged(resource.getText());
        presenter.keyPressed();
    }

    @SuppressWarnings("unused")
    @UiHandler("rrHarvester")
    void rrHarvesterChanged(ValueChangeEvent<String> event) {
        presenter.rrHarvesterChanged(rrHarvester.getSelectedKey());
        presenter.keyPressed();
    }

    @SuppressWarnings("unused")
    @UiHandler("enabled")
    void enabledChanged(ValueChangeEvent<Boolean> event) {
        presenter.enabledChanged(enabled.getValue());
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

    @UiHandler("confirmation")
    void confirmationButtonClicked(DialogEvent event) {
        if (event.getDialogButton() == DialogEvent.DialogButton.OK_BUTTON) {
            presenter.deleteButtonPressed();
        }
    }

}
