package dk.dbc.dataio.gui.client.pages.harvester.corepo.modify;

import dk.dbc.dataio.gui.client.presenters.GenericPresenter;


public interface Presenter extends GenericPresenter {
    void nameChanged(String name);

    void descriptionChanged(String description);

    void resourceChanged(String resource);

    void rrHarvesterChanged(String rrHarvester);

    void enabledChanged(Boolean value);

    void keyPressed();

    void deleteButtonPressed();

    void saveButtonPressed();
}
