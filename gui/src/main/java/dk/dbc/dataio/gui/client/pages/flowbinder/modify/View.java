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

package dk.dbc.dataio.gui.client.pages.flowbinder.modify;


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
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Label;
import dk.dbc.dataio.gui.client.components.PopupListBox;
import dk.dbc.dataio.gui.client.components.PromptedList;
import dk.dbc.dataio.gui.client.components.PromptedMultiList;
import dk.dbc.dataio.gui.client.components.PromptedTextArea;
import dk.dbc.dataio.gui.client.components.PromptedTextBox;
import dk.dbc.dataio.gui.client.events.DialogEvent;
import dk.dbc.dataio.gui.client.views.ContentPanel;

import java.util.Map;

public class View extends ContentPanel<Presenter> implements IsWidget {
    interface FlowbinderBinder extends UiBinder<HTMLPanel, View> {}
    private static FlowbinderBinder uiBinder = GWT.create(FlowbinderBinder.class);

    public View() {
        super("");
        add(uiBinder.createAndBindUi(this));
    }

    @UiField PromptedTextBox name;
    @UiField PromptedTextArea description;
    @UiField PromptedTextBox frame;
    @UiField PromptedTextBox format;
    @UiField PromptedTextBox charset;
    @UiField PromptedTextBox destination;
    @UiField PromptedList recordSplitter;
    @UiField PromptedMultiList submitters;
    @UiField PromptedList flow;
    @UiField PromptedList sink;
    @UiField HTMLPanel updateSinkSection;
    @UiField PromptedList queueProvider;
    @UiField Button deleteButton;
    @UiField Label status;
    @UiField PopupListBox popupListBox;

    @UiHandler("name")
    void nameChanged(BlurEvent event) {
        presenter.nameChanged(name.getText());
    }

    @UiHandler("description")
    void descriptionChanged(BlurEvent event) {
        presenter.descriptionChanged(description.getText());
    }

    @UiHandler("frame")
    void frameChanged(BlurEvent event) {
        presenter.frameChanged(frame.getText());
    }

    @UiHandler("format")
    void formatChanged(BlurEvent event) {
        presenter.formatChanged(format.getText());
    }

    @UiHandler("charset")
    void charsetChanged(BlurEvent event) {
        presenter.charsetChanged(charset.getText());
    }

    @UiHandler("destination")
    void destinationChanged(BlurEvent event) {
        presenter.destinationChanged(destination.getText());
    }

    @UiHandler("recordSplitter")
    void recordSplitterChanged(ValueChangeEvent<String> event) {
        presenter.recordSplitterChanged(recordSplitter.getSelectedKey());
        presenter.keyPressed();
    }

    @UiHandler("submitters")
    void submittersClicked(ClickEvent event) {
        if (submitters.isAddEvent(event)) {
            popupListBox.show();
        }
        if (submitters.isRemoveEvent(event)) {
            presenter.removeSubmitter(submitters.getSelectedItem());
        }
    }

    @UiHandler("submitters")
    void submittersChanged(ValueChangeEvent<Map<String, String>> event) {
        presenter.submittersChanged(submitters.getValue());
        presenter.keyPressed();
    }

    @UiHandler("flow")
    void flowChanged(ValueChangeEvent<String> event) {
        presenter.flowChanged(flow.getSelectedKey());
        presenter.keyPressed();
    }

    @UiHandler("sink")
    void sinkChanged(ValueChangeEvent<String> event) {
        presenter.sinkChanged(sink.getSelectedKey());
        presenter.keyPressed();
    }

    @UiHandler("queueProvider")
    void queueProviderChanged(ValueChangeEvent<String> event) {
        presenter.queueProviderChanged(queueProvider.getSelectedKey());
        presenter.keyPressed();
    }

    @UiHandler({"name", "description", "frame", "format", "charset", "destination"})
    void keyPressedInField(KeyDownEvent event) {
        presenter.keyPressed();
    }

    @UiHandler("saveButton")
    void saveButtonPressed(ClickEvent event) {
        presenter.saveButtonPressed();
    }

    @UiHandler("deleteButton")
    void deleteButtonPressed(ClickEvent event) {
        presenter.deleteButtonPressed();
    }

    @UiHandler("popupListBox")
    void setPopupListBoxClicked(DialogEvent event) {
        if (event.getDialogButton() == DialogEvent.DialogButton.OK_BUTTON) {
            presenter.addSubmitter(popupListBox.getValue().getValue());
        }
    }

}
