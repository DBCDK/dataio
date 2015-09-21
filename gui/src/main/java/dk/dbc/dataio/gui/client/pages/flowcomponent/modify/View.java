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

package dk.dbc.dataio.gui.client.pages.flowcomponent.modify;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Label;
import dk.dbc.dataio.gui.client.components.PromptedList;
import dk.dbc.dataio.gui.client.components.PromptedTextArea;
import dk.dbc.dataio.gui.client.components.PromptedTextBox;
import dk.dbc.dataio.gui.client.views.ContentPanel;

public class View extends ContentPanel<Presenter> implements IsWidget {

    interface FlowComponentBinder extends UiBinder<HTMLPanel, View> {}
    private static FlowComponentBinder uiBinder = GWT.create(FlowComponentBinder.class);

    public View(String header) {
        super(header);
        add(uiBinder.createAndBindUi(this));
    }

    @UiField PromptedTextBox name;
    @UiField PromptedTextArea description;
    @UiField PromptedTextBox project;
    @UiField PromptedList revision;
    @UiField PromptedList next;
    @UiField PromptedList script;
    @UiField PromptedList method;
    @UiField Label status;
    @UiField Label busy;

    @UiHandler("name")
    void keyPressedInNameField(KeyDownEvent event) {
        presenter.keyPressed();
    }

    @UiHandler("name")
    void nameChanged(ValueChangeEvent<String> event) {
        presenter.nameChanged(name.getText());
    }

    @UiHandler("description")
    void descriptionChanged(ValueChangeEvent<String> event) {
        presenter.descriptionChanged(description.getText());
        presenter.keyPressed();
    }

    @UiHandler("project")
    void keyPressedInProjectField(KeyDownEvent event) {
        presenter.keyPressed();
    }

    @UiHandler("project")
    void projectChanged(ValueChangeEvent<String> event) {
        presenter.projectChanged(project.getText());
    }

    @UiHandler("revision")
     void revisionChanged(ValueChangeEvent<String> event) {
        presenter.revisionChanged(revision.getSelectedKey());
        presenter.keyPressed();
    }

    @UiHandler("next")
    void nextChanged(ValueChangeEvent<String> event) {
        presenter.nextChanged(next.getSelectedKey());
        presenter.keyPressed();
    }

    @UiHandler("script")
    void scriptNameChanged(ValueChangeEvent<String> event) {
        presenter.scriptNameChanged(script.getSelectedKey());
        presenter.keyPressed();
    }

    @UiHandler("method")
    void invocationMethodChanged(ValueChangeEvent<String> event) {
        presenter.invocationMethodChanged(method.getSelectedKey());
        presenter.keyPressed();
    }

    @UiHandler("saveButton")
    void saveButtonPressed(ClickEvent event) {
        presenter.saveButtonPressed();
    }

}
