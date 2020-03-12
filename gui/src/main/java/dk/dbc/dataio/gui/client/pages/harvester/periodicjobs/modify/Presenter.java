/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.dataio.gui.client.pages.harvester.periodicjobs.modify;

import dk.dbc.dataio.gui.client.presenters.GenericPresenter;
import dk.dbc.dataio.harvester.types.PeriodicJobsHarvesterConfig;

public interface Presenter extends GenericPresenter {
    void pickupTypeChanged(PeriodicJobsHarvesterConfig.PickupType pickupType);
    void nameChanged(String name);
    void scheduleChanged(String schedule);
    void descriptionChanged(String description);
    void resourceChanged(String resource);
    void queryChanged(String query);
    void collectionChanged(String collection);
    void destinationChanged(String destination);
    void formatChanged(String format);
    void submitterChanged(String submitter);
    void contactChanged(String contact);
    void timeOfLastHarvestChanged(String timeOfLastHarvest);
    void enabledChanged(Boolean value);
    void httpReceivingAgencyChanged(String agency);
    void mailRecipientsChanged(String mailRecipient);
    void mailSubjectChanged(String subject);
    void ftpAddressChanged(String subject);
    void ftpUserChanged(String subject);
    void ftpPasswordChanged(String subject);
    void ftpSubdirChanged(String subject);
    void keyPressed();
    void saveButtonPressed();
    void runButtonPressed();
}
