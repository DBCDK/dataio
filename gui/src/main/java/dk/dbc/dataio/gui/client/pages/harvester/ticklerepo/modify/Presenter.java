package dk.dbc.dataio.gui.client.pages.harvester.ticklerepo.modify;

import dk.dbc.dataio.gui.client.presenters.GenericPresenter;


public interface Presenter extends GenericPresenter {
    void idChanged(String id);

    void nameChanged(String name);

    void descriptionChanged(String description);

    void destinationChanged(String destination);

    void formatChanged(String format);

    void typeChanged(String type);

    void enabledChanged(Boolean value);

    void notificationsEnabledChanged(Boolean value);

    void keyPressed();

    void deleteButtonPressed();

    void saveButtonPressed();

    void taskRecordHarvestButtonPressed();

    void deleteOutdatedRecordsButtonPressed();

    void deleteOutdatedRecords();

    void setRecordHarvestCount();
}
