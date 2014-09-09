package dk.dbc.dataio.gui.client.pages.flow.modify;

import dk.dbc.dataio.gui.client.presenters.GenericPresenter;

public interface Presenter extends GenericPresenter {
    void nameChanged(String name);
    void descriptionChanged(String description);
    void flowComponentsChanged();
    void keyPressed();
    void saveButtonPressed();
}
