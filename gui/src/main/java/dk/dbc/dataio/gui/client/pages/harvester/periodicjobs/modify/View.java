/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.dataio.gui.client.pages.harvester.periodicjobs.modify;

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
import dk.dbc.dataio.gui.client.components.prompted.PromptedDateTimeBox;
import dk.dbc.dataio.gui.client.components.prompted.PromptedList;
import dk.dbc.dataio.gui.client.components.prompted.PromptedTextArea;
import dk.dbc.dataio.gui.client.components.prompted.PromptedTextBox;
import dk.dbc.dataio.gui.client.events.DialogEvent;
import dk.dbc.dataio.gui.client.views.ContentPanel;
import dk.dbc.dataio.harvester.types.PeriodicJobsHarvesterConfig;

public class View extends ContentPanel<Presenter> implements IsWidget {
    interface HarvesterBinder extends UiBinder<HTMLPanel, View> {}
    private static HarvesterBinder uiBinder = GWT.create(HarvesterBinder.class);

    private ViewGinjector viewInjector = GWT.create(ViewGinjector.class);

    public View() {
        super("");
        add(uiBinder.createAndBindUi(this));
        final Texts texts = getTexts();
        contact.setTitle(texts.help_Contact());
        collection.setTitle(texts.help_Collection());
        description.setTitle(texts.help_Description());
        query.setTitle(texts.help_Query());
        resource.setTitle(texts.help_Resource());
        schedule.setTitle(texts.help_Schedule());
        httpReceivingAgency.setTitle(texts.help_HttpReceivingAgency());
        mailRecipient.setTitle(texts.help_Recipients());
        mailSubject.setTitle(texts.help_Subject());
        ftpAddress.setTitle(texts.help_FtpAddress());
        ftpUser.setTitle(texts.help_FtpUser());
        ftpPassword.setTitle(texts.help_FtpPassword());
        ftpSubdir.setTitle(texts.help_FtpSubdir());
        sftpAddress.setTitle(texts.help_SFtpAddress());
        sFtpUser.setTitle(texts.help_SFtpUser());
        sftpPassword.setTitle(texts.help_SFtpPassword());
        sftpSubdir.setTitle(texts.help_SFtpSubdir());
    }

    @UiFactory
    PopupBox<Label> getPopupBox() {
        return new PopupBox<>(new Label(viewInjector.getTexts().label_AreYouSureAboutDeleting()), "", "");
    }

    @UiField HTMLPanel httpSection;
    @UiField HTMLPanel mailSection;
    @UiField HTMLPanel ftpSection;
    @UiField HTMLPanel sftpSection;

    @UiField PromptedList pickupTypeSelection;
    @UiField PromptedList harvesterTypeSelection;
    @UiField PromptedTextBox name;
    @UiField PromptedTextBox schedule;
    @UiField PromptedTextArea description;
    @UiField PromptedTextBox resource;
    @UiField PromptedTextArea query;
    @UiField PromptedTextBox collection;
    @UiField PromptedTextBox destination;
    @UiField PromptedTextBox format;
    @UiField PromptedTextBox submitter;
    @UiField PromptedTextBox contact;
    @UiField PromptedDateTimeBox timeOfLastHarvest;
    @UiField PromptedCheckBox enabled;
    @UiField PromptedTextBox httpReceivingAgency;
    @UiField PromptedTextBox mailRecipient;
    @UiField PromptedTextBox mailSubject;
    @UiField PromptedTextBox ftpAddress;
    @UiField PromptedTextBox ftpUser;
    @UiField PromptedTextBox ftpPassword;
    @UiField PromptedTextBox ftpSubdir;
    @UiField PromptedTextBox sftpAddress;
    @UiField PromptedTextBox sFtpUser;
    @UiField PromptedTextBox sftpPassword;
    @UiField PromptedTextBox sftpSubdir;

    @UiField Button saveButton;
    @UiField Button deleteButton;
    @UiField Button runButton;
    @UiField Label status;
    @UiField PopupBox<Label> confirmation;

    @SuppressWarnings("unused")
    @UiHandler("pickupTypeSelection")
    void pickupTypeSelectionChanged(ValueChangeEvent<String> event) {
        presenter.pickupTypeChanged(PeriodicJobsHarvesterConfig.PickupType.valueOf(
                pickupTypeSelection.getSelectedKey()));
        presenter.keyPressed();
    }

    @SuppressWarnings("unused")
    @UiHandler("harvesterTypeSelection")
    void harvesterTypeSelectionChanged(ValueChangeEvent<String> event) {
        presenter.harvesterTypeChanged(PeriodicJobsHarvesterConfig.HarvesterType.valueOf(
                harvesterTypeSelection.getSelectedKey()));
        presenter.keyPressed();
    }

    @SuppressWarnings("unused")
    @UiHandler("name")
    void nameChanged(ValueChangeEvent<String> event) {
        presenter.nameChanged(name.getText());
        presenter.keyPressed();
    }

    @SuppressWarnings("unused")
    @UiHandler("schedule")
    void scheduleChanged(ValueChangeEvent<String> event) {
        presenter.scheduleChanged(schedule.getText());
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
    @UiHandler("query")
    void queryChanged(ValueChangeEvent<String> event) {
        presenter.queryChanged(query.getText());
        presenter.keyPressed();
    }

    @SuppressWarnings("unused")
    @UiHandler("collection")
    void collectionChanged(ValueChangeEvent<String> event) {
        presenter.collectionChanged(collection.getText());
        presenter.keyPressed();
    }

    @SuppressWarnings("unused")
    @UiHandler("destination")
    void destinationChanged(ValueChangeEvent<String> event) {
        presenter.destinationChanged(destination.getText());
        presenter.keyPressed();
    }

    @SuppressWarnings("unused")
    @UiHandler("format")
    void formatChanged(ValueChangeEvent<String> event) {
        presenter.formatChanged(format.getText());
        presenter.keyPressed();
    }

    @SuppressWarnings("unused")
    @UiHandler("submitter")
    void submitterChanged(ValueChangeEvent<String> event) {
        presenter.submitterChanged(submitter.getText());
        presenter.keyPressed();
    }

    @SuppressWarnings("unused")
    @UiHandler("contact")
    void contactChanged(ValueChangeEvent<String> event) {
        presenter.contactChanged(contact.getText());
        presenter.keyPressed();
    }

    @SuppressWarnings("unused")
    @UiHandler("timeOfLastHarvest")
    void timeOfLastHarvestChanged(ValueChangeEvent<String> event) {
        presenter.timeOfLastHarvestChanged(timeOfLastHarvest.getValue());
        presenter.keyPressed();
    }

    @SuppressWarnings("unused")
    @UiHandler("enabled")
    void enabledChanged(ValueChangeEvent<Boolean> event) {
        presenter.enabledChanged(enabled.getValue());
        presenter.keyPressed();
    }

    @SuppressWarnings("unused")
    @UiHandler("httpReceivingAgency")
    void receivingAgencyChanged(ValueChangeEvent<String> event) {
        presenter.httpReceivingAgencyChanged(httpReceivingAgency.getText());
        presenter.keyPressed();
    }

    @UiHandler("mailRecipient")
    void mailRecipientsChanged(ValueChangeEvent<String> event) {
        presenter.mailRecipientsChanged(mailRecipient.getText());
        presenter.keyPressed();
    }

    @UiHandler("mailSubject")
    void mailSubjectChanged(ValueChangeEvent<String> event) {
        presenter.mailSubjectChanged(mailSubject.getText());
        presenter.keyPressed();
    }

    @UiHandler("ftpAddress")
    void ftpAddressChanged(ValueChangeEvent<String> event) {
        presenter.ftpAddressChanged(ftpAddress.getText());
        presenter.keyPressed();
    }

    @UiHandler("ftpUser")
    void ftpUserChanged(ValueChangeEvent<String> event) {
        presenter.ftpUserChanged(ftpUser.getText());
    }

    @UiHandler("ftpPassword")
    void ftpPasswordChanged(ValueChangeEvent<String> event) {
        presenter.ftpPasswordChanged(ftpPassword.getText());
    }

    @UiHandler("ftpSubdir")
    void ftpSubdirChanged(ValueChangeEvent<String> event) {
        presenter.ftpSubdirChanged(ftpSubdir.getText());
    }





    @UiHandler("sftpAddress")
    void sftpAddressChanged(ValueChangeEvent<String> event) {
        presenter.sftpAddressChanged(sftpAddress.getText());
        presenter.keyPressed();
    }

    @UiHandler("sFtpUser")
    void sFtpUserChanged(ValueChangeEvent<String> event) {
        presenter.sFtpUserChanged(sFtpUser.getText());
    }

    @UiHandler("sftpPassword")
    void sftpPasswordChanged(ValueChangeEvent<String> event) {
        presenter.sftpPasswordChanged(sftpPassword.getText());
    }

    @UiHandler("sftpSubdir")
    void sftpSubdirChanged(ValueChangeEvent<String> event) {
        presenter.sftpSubdirChanged(sftpSubdir.getText());
    }


    @SuppressWarnings("unused")
    @UiHandler("saveButton")
    void saveButtonPressed(ClickEvent event) {
        presenter.saveButtonPressed();
    }

    @SuppressWarnings("unused")
    @UiHandler("runButton")
    void runButtonPressed(ClickEvent event) {
        presenter.runButtonPressed();
    }

    @SuppressWarnings("unused")
    @UiHandler("deleteButton")
    void deleteButtonPressed(ClickEvent event) {
        confirmation.show();
    }

    @UiHandler("confirmation")
    void confirmationButtonClicked(DialogEvent event) {
        if (event.getDialogButton() == DialogEvent.DialogButton.OK_BUTTON) {
            ((PresenterEditImpl) presenter).deleteButtonPressed();
        }
    }

    protected Texts getTexts() {
        return this.viewInjector.getTexts();
    }
}
