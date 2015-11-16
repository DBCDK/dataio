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

package dk.dbc.dataio.gui.client.pages.flow.modify;


import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Label;
import dk.dbc.dataio.gui.client.components.PromptedMultiList;
import dk.dbc.dataio.gui.client.components.PromptedTextArea;
import dk.dbc.dataio.gui.client.components.PromptedTextBox;
import dk.dbc.dataio.gui.client.model.FlowModel;
import dk.dbc.dataio.gui.client.views.ContentPanel;

import java.util.Map;

public class ViewWidget extends ContentPanel<Presenter> {
    interface FlowUiBinder extends UiBinder<HTMLPanel, ViewWidget> {}
    private static FlowUiBinder uiBinder = GWT.create(FlowUiBinder.class);
    protected FlowModel model;
    protected boolean showAvailableFlowComponents;

    public ViewWidget() {
        super("");
        add(uiBinder.createAndBindUi(this));
        this.model = new FlowModel();
        this.showAvailableFlowComponents = false;
    }

    @UiField PromptedTextBox name;
    @UiField PromptedTextArea description;
    @UiField PromptedMultiList flowComponents;
    @UiField Button deleteButton;
    @UiField Label status;


    @UiHandler("name")
    void nameChanged(BlurEvent event) {
        presenter.nameChanged(name.getText());
    }

    @UiHandler("description")
    void descriptionChanged(BlurEvent event) {
        presenter.descriptionChanged(description.getText());
    }

    @UiHandler("flowComponents")
    void flowComponentsChanged(ValueChangeEvent<Map<String, String>> event) {
        if (presenter != null) {
            presenter.flowComponentsChanged(flowComponents.getValue());
            presenter.keyPressed();
        }
    }

    @UiHandler(value={"name", "description"})
    void keyPressed(KeyDownEvent event) {
        presenter.keyPressed();
    }

    @UiHandler("saveButton")
    void saveButtonPressed(ClickEvent event) {
        presenter.saveButtonPressed();
    }

    @UiHandler("deleteButton")
    @SuppressWarnings("unused")
    void deleteButtonPressed(ClickEvent event) {
        presenter.deleteButtonPressed();
    }

    @UiHandler("flowComponents")
    void flowComponentButtonsClicked(ClickEvent event) {
        if (flowComponents.isAddEvent(event)) {
            presenter.addButtonPressed();
        } else if (flowComponents.isRemoveEvent(event)) {
            presenter.removeButtonPressed();
        }
    }

}
