package dk.dbc.dataio.gui.client.pages.harvester.periodicjobs.modify;

import dk.dbc.dataio.gui.client.presenters.GenericPresenter;
import dk.dbc.dataio.harvester.types.PeriodicJobsHarvesterConfig;

public interface Presenter extends GenericPresenter {
    void pickupTypeChanged(PeriodicJobsHarvesterConfig.PickupType pickupType);

    void harvesterTypeChanged(PeriodicJobsHarvesterConfig.HarvesterType harvesterType);

    void nameChanged(String name);

    void scheduleChanged(String schedule);

    void descriptionChanged(String description);

    void resourceChanged(String resource);

    void queryChanged(String query);

    void queryFileIdClicked(String buttonType);

    void fileStoreUploadChanged(String fileId);

    void collectionChanged(String collection);

    void holdingsTypeSelectionChanged(PeriodicJobsHarvesterConfig.HoldingsFilter holdingsFilter);

    void holdingsSolrUrlChanged(String holdingsSolrUrl);

    void destinationChanged(String destination);

    void formatChanged(String format);

    void submitterChanged(String submitter);

    void contactChanged(String contact);

    void timeOfLastHarvestChanged(String timeOfLastHarvest);

    void overrideFilenameChanged(String overrideFilename) throws UnsupportedOperationException;

    void contentHeaderChanged(String contentHeader);

    void contentFooterChanged(String contentfooter);

    void enabledChanged(Boolean value);

    void httpReceivingAgencyChanged(String agency);

    void mailRecipientsChanged(String mailRecipient);

    void mailSubjectChanged(String subject);

    void mailMimetypeChanged(String mimetype);

    void mailBodyChanged(String body);

    void mailRecordLimitChanged(String recordLimit);

    void ftpAddressChanged(String subject);

    void ftpUserChanged(String subject);

    void ftpPasswordChanged(String subject);

    void ftpSubdirChanged(String subject);

    void sftpAddressChanged(String subject);

    void sFtpUserChanged(String subject);

    void sftpPasswordChanged(String subject);

    void sftpSubdirChanged(String subject);

    void keyPressed();

    void saveButtonPressed();

    void runButtonPressed();

    void refreshButtonPressed();

    void validateSolrButtonPressed();
}
