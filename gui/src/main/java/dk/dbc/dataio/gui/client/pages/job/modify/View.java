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

package dk.dbc.dataio.gui.client.pages.job.modify;

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
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.gui.client.components.prompted.PromptedTextBox;
import dk.dbc.dataio.gui.client.views.ContentPanel;

public class View extends ContentPanel<Presenter> implements IsWidget {

    interface EditJobBinder extends UiBinder<HTMLPanel, View> {}
    private static EditJobBinder uiBinder = GWT.create(EditJobBinder.class);

    @UiField Label header;
    @UiField PromptedTextBox jobId;
    @UiField PromptedTextBox packaging;
    @UiField PromptedTextBox format;
    @UiField PromptedTextBox charset;
    @UiField PromptedTextBox destination;
    @UiField PromptedTextBox mailForNotificationAboutVerification;
    @UiField PromptedTextBox mailForNotificationAboutProcessing;
    @UiField PromptedTextBox resultMailInitials;
    @UiField PromptedTextBox type;
    @UiField PromptedTextBox jobcreationtime;
    @UiField PromptedTextBox jobcompletiontime;
    @UiField PromptedTextBox datafile;
    @UiField PromptedTextBox partnumber;
    @UiField Button rerunButton;

    @UiField Label status;

    public View() {
        super("");
        add(uiBinder.createAndBindUi(this));
    }

    @UiHandler("packaging")
    void numberChanged(ValueChangeEvent<String> event) {
        presenter.packagingChanged(packaging.getText());
        presenter.keyPressed();
    }

    @UiHandler("format")
    void nameChanged(ValueChangeEvent<String> event) {
        presenter.formatChanged(format.getText());
        presenter.keyPressed();
    }

    @UiHandler("charset")
    void descriptionChanged(ValueChangeEvent<String> event) {
        presenter.charsetChanged(charset.getText());
        presenter.keyPressed();
    }

    @UiHandler("destination")
    void destinationChanged(ValueChangeEvent<String> event) {
        presenter.destinationChanged(destination.getText());
        presenter.keyPressed();
    }

    @UiHandler("mailForNotificationAboutVerification")
    void mailForNotificationAboutVerificationChanged(ValueChangeEvent<String> event) {
        presenter.mailForNotificationAboutVerificationChanged(mailForNotificationAboutVerification.getText());
        presenter.keyPressed();
    }

    @UiHandler("mailForNotificationAboutProcessing")
    void mailForNotificationAboutProcessingChanged(ValueChangeEvent<String> event) {
        presenter.mailForNotificationAboutProcessingChanged(mailForNotificationAboutProcessing.getText());
        presenter.keyPressed();
    }

    @UiHandler("resultMailInitials")
    void resultMailInitialsChanged(ValueChangeEvent<String> event) {
        presenter.resultMailInitialsChanged(resultMailInitials.getText());
        presenter.keyPressed();
    }

    @UiHandler("type")
    void typeChanged(ValueChangeEvent<String> event) {
        presenter.typeChanged(JobSpecification.Type.valueOf(type.getText()));
        presenter.keyPressed();
    }

    @UiHandler("rerunButton")
    void rerunButtonPressed(ClickEvent event) {
        presenter.rerunButtonPressed();
    }
}
