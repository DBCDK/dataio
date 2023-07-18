package dk.dbc.dataio.gui.client.pages.harvester.periodicjobs.modify;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiFactory;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextArea;
import dk.dbc.dataio.gui.client.components.popup.PopupBox;
import dk.dbc.dataio.gui.client.components.prompted.PromptedAnchorWithButton;
import dk.dbc.dataio.gui.client.components.prompted.PromptedCheckBox;
import dk.dbc.dataio.gui.client.components.prompted.PromptedDateTimeBox;
import dk.dbc.dataio.gui.client.components.prompted.PromptedFileStoreUpload;
import dk.dbc.dataio.gui.client.components.prompted.PromptedList;
import dk.dbc.dataio.gui.client.components.prompted.PromptedTextArea;
import dk.dbc.dataio.gui.client.components.prompted.PromptedTextBox;
import dk.dbc.dataio.gui.client.events.DialogEvent;
import dk.dbc.dataio.gui.client.views.ContentPanel;
import dk.dbc.dataio.harvester.types.PeriodicJobsHarvesterConfig;

public class View extends ContentPanel<Presenter> implements IsWidget {
    interface HarvesterBinder extends UiBinder<HTMLPanel, View> {
    }

    private static HarvesterBinder uiBinder = GWT.create(HarvesterBinder.class);

    private ViewGinjector viewInjector = GWT.create(ViewGinjector.class);

    public View() {
        super("");
        add(uiBinder.createAndBindUi(this));
        final Texts texts = getTexts();
        contact.setTitle(texts.help_Contact());
        collection.setTitle(texts.help_Collection());
        holdingsSolrUrl.setTitle(texts.help_HoldingsSolrUrl());
        holdingsTypeSelection.setTitle(texts.help_HoldingsTypeSelection());
        description.setTitle(texts.help_Description());
        query.setTitle(texts.help_Query());
        queryFileId.setTitle(texts.help_QueryFileId());
        resource.setTitle(texts.help_Resource());
        schedule.setTitle(texts.help_Schedule());
        httpReceivingAgency.setTitle(texts.help_HttpReceivingAgency());
        mailRecipient.setTitle(texts.help_Recipients());
        mailSubject.setTitle(texts.help_Subject());
        mailBody.setTitle(texts.help_Body());
        mailRecordLimit.setTitle(texts.help_RecordLimit());
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

    @UiField
    HTMLPanel httpSection;
    @UiField
    HTMLPanel mailSection;
    @UiField
    HTMLPanel ftpSection;
    @UiField
    HTMLPanel sftpSection;
    @UiField
    HTMLPanel holdingsSection;
    @UiField
    PromptedList pickupTypeSelection;
    @UiField
    PromptedList harvesterTypeSelection;
    @UiField
    PromptedTextBox name;
    @UiField
    PromptedTextBox schedule;
    @UiField
    PromptedTextArea description;
    @UiField
    PromptedTextBox resource;
    @UiField
    PromptedTextArea query;
    @UiField
    PromptedAnchorWithButton queryFileId;
    @UiField
    PromptedFileStoreUpload fileStoreUpload;
    @UiField
    PromptedTextBox collection;
    @UiField
    PromptedList holdingsTypeSelection;
    @UiField
    PromptedTextBox holdingsSolrUrl;
    @UiField
    PromptedTextBox destination;
    @UiField
    PromptedTextBox format;
    @UiField
    PromptedTextBox submitter;
    @UiField
    PromptedTextBox contact;
    @UiField
    PromptedDateTimeBox timeOfLastHarvest;
    @UiField
    PromptedTextBox overrideFilename;
    @UiField
    PromptedCheckBox enabled;
    @UiField
    PromptedTextBox httpReceivingAgency;
    @UiField
    PromptedTextBox mailRecipient;
    @UiField
    PromptedTextBox mailSubject;
    @UiField
    PromptedTextBox mailMimetype;
    @UiField
    PromptedTextArea mailBody;
    @UiField
    PromptedTextBox mailRecordLimit;
    @UiField
    PromptedTextBox ftpAddress;
    @UiField
    PromptedTextBox ftpUser;
    @UiField
    PromptedTextBox ftpPassword;
    @UiField
    PromptedTextBox ftpSubdir;
    @UiField
    PromptedTextBox sftpAddress;
    @UiField
    PromptedTextBox sFtpUser;
    @UiField
    PromptedTextBox sftpPassword;
    @UiField
    PromptedTextBox sftpSubdir;
    @UiField
    PromptedTextArea contentFooter;
    @UiField
    PromptedTextArea contentHeader;
    @UiField
    Button saveButton;
    @UiField
    Button deleteButton;
    @UiField
    Button runButton;
    @UiField
    Button refreshButton;

    @UiField
    Label status;
    @UiField
    TextArea queryStatus;
    @UiField
    PopupBox<Label> confirmation;

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
    @UiHandler("queryFileId")
    void queryFileIdClicked(ValueChangeEvent<String> event) {
        presenter.queryFileIdClicked(event.getValue());
        presenter.keyPressed();
    }

    @SuppressWarnings("unused")
    @UiHandler("fileStoreUpload")
    void queryFileIdSubmitCompleted(FormPanel.SubmitCompleteEvent event) {
        String fileId = event.getResults();

        /*
         * This is a hack because the received value is something along
         * <pre style="word-wrap: break-word; white-space: pre-wrap;">42</pre>
         * When the returned value has been fixed this hack should be removed!
         */
        if (fileId.startsWith("<") && fileId.endsWith(">")) {
            int start = fileId.indexOf(">") + 1;
            int end = fileId.indexOf("<", 2);

            fileId = fileId.substring(start, end);
        }
        presenter.fileStoreUploadChanged(fileId);
        presenter.keyPressed();
    }

    @SuppressWarnings("unused")
    @UiHandler("collection")
    void collectionChanged(ValueChangeEvent<String> event) {
        presenter.collectionChanged(collection.getText());
        presenter.keyPressed();
    }

    @SuppressWarnings("unused")
    @UiHandler("holdingsTypeSelection")
    void holdingsTypeSelectionChanged(ValueChangeEvent<String> event) {
        presenter.holdingsTypeSelectionChanged(PeriodicJobsHarvesterConfig.HoldingsFilter.valueOf(
                holdingsTypeSelection.getSelectedKey()));
        presenter.keyPressed();
    }

    @SuppressWarnings("unused")
    @UiHandler("holdingsSolrUrl")
    void holdingsSolrUrlChanged(ValueChangeEvent<String> event) {
        presenter.holdingsSolrUrlChanged(holdingsSolrUrl.getText());
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
    @UiHandler("overrideFilename")
    void overrideFilenameChanged(ValueChangeEvent<String> event) throws UnsupportedOperationException {
        presenter.overrideFilenameChanged(overrideFilename.getValue());
        presenter.keyPressed();
    }

    @SuppressWarnings("unused")
    @UiHandler("contentHeader")
    void contentHeaderChanged(ValueChangeEvent<String> event) throws UnsupportedOperationException {
        presenter.contentHeaderChanged(contentHeader.getValue());
        presenter.keyPressed();
    }

    @SuppressWarnings("unused")
    @UiHandler("contentFooter")
    void contentFooterChanged(ValueChangeEvent<String> event) throws UnsupportedOperationException {
        presenter.contentFooterChanged(contentFooter.getValue());
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

    @SuppressWarnings("unused")
    @UiHandler("mailRecipient")
    void mailRecipientsChanged(ValueChangeEvent<String> event) {
        presenter.mailRecipientsChanged(mailRecipient.getText());
        presenter.keyPressed();
    }

    @SuppressWarnings("unused")
    @UiHandler("mailSubject")
    void mailSubjectChanged(ValueChangeEvent<String> event) {
        presenter.mailSubjectChanged(mailSubject.getText());
        presenter.keyPressed();
    }

    @SuppressWarnings("unused")
    @UiHandler("mailMimetype")
    void mailMimetypeChanged(ValueChangeEvent<String> event) {
        presenter.mailMimetypeChanged(mailMimetype.getText());
        presenter.keyPressed();
    }

    @SuppressWarnings("unused")
    @UiHandler("mailBody")
    void mailBodyChanged(ValueChangeEvent<String> event) {
        presenter.mailBodyChanged(mailBody.getValue());
        presenter.keyPressed();
    }

    @SuppressWarnings("unused")
    @UiHandler("mailRecordLimit")
    void maxMailRecordLimit(ValueChangeEvent<String> event) {
        presenter.mailRecordLimitChanged(mailRecordLimit.getText());
        presenter.keyPressed();
    }

    @SuppressWarnings("unused")
    @UiHandler("ftpAddress")
    void ftpAddressChanged(ValueChangeEvent<String> event) {
        presenter.ftpAddressChanged(ftpAddress.getText());
        presenter.keyPressed();
    }

    @SuppressWarnings("unused")
    @UiHandler("ftpUser")
    void ftpUserChanged(ValueChangeEvent<String> event) {
        presenter.ftpUserChanged(ftpUser.getText());
    }

    @SuppressWarnings("unused")
    @UiHandler("ftpPassword")
    void ftpPasswordChanged(ValueChangeEvent<String> event) {
        presenter.ftpPasswordChanged(ftpPassword.getText());
    }

    @SuppressWarnings("unused")
    @UiHandler("ftpSubdir")
    void ftpSubdirChanged(ValueChangeEvent<String> event) {
        presenter.ftpSubdirChanged(ftpSubdir.getText());
    }

    @SuppressWarnings("unused")
    @UiHandler("sftpAddress")
    void sftpAddressChanged(ValueChangeEvent<String> event) {
        presenter.sftpAddressChanged(sftpAddress.getText());
        presenter.keyPressed();
    }

    @SuppressWarnings("unused")
    @UiHandler("sFtpUser")
    void sFtpUserChanged(ValueChangeEvent<String> event) {
        presenter.sFtpUserChanged(sFtpUser.getText());
    }

    @SuppressWarnings("unused")
    @UiHandler("sftpPassword")
    void sftpPasswordChanged(ValueChangeEvent<String> event) {
        presenter.sftpPasswordChanged(sftpPassword.getText());
    }

    @SuppressWarnings("unused")
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
    @UiHandler("refreshButton")
    void RefreshButtonPressed(ClickEvent event) {
        presenter.refreshButtonPressed();
    }

    @SuppressWarnings("unused")
    @UiHandler("deleteButton")
    void deleteButtonPressed(ClickEvent event) {
        confirmation.show();
    }

    @SuppressWarnings("unused")
    @UiHandler("validateButton")
    void validateSolrButtonPressed(ClickEvent event) {
        presenter.validateSolrButtonPressed();
    }

    @SuppressWarnings("unused")
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
