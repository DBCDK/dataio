package dk.dbc.dataio.gui.client.pages.harvester.dmat.modify;

import dk.dbc.dataio.gui.client.presenters.GenericPresenter;

public interface Presenter extends GenericPresenter {
    void nameChanged(String name);

    void scheduleChanged(String schedule);

    void descriptionChanged(String description);

    void destinationChanged(String destination);


    void formatChanged(String format);

    void enabledChanged(Boolean value);

    void keyPressed();

    void saveButtonPressed();
}
