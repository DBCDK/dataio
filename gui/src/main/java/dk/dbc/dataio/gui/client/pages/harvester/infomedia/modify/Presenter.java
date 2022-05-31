package dk.dbc.dataio.gui.client.pages.harvester.infomedia.modify;

import dk.dbc.dataio.gui.client.presenters.GenericPresenter;

public interface Presenter extends GenericPresenter {
    void idChanged(String id);

    void scheduleChanged(String name);

    void descriptionChanged(String description);

    void destinationChanged(String destination);

    void formatChanged(String format);

    void nextPublicationDateChanged(String date);

    void enabledChanged(Boolean value);

    void keyPressed();

    void deleteButtonPressed();

    void saveButtonPressed();
}
