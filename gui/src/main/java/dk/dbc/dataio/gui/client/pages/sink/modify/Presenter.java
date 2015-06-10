package dk.dbc.dataio.gui.client.pages.sink.modify;

import dk.dbc.dataio.gui.client.presenters.GenericPresenter;

public interface Presenter extends GenericPresenter {
    void nameChanged(String name);
    void resourceChanged(String resource);
    void descriptionChanged(String description);
    void keyPressed();
    void saveButtonPressed();
}
