package dk.dbc.dataio.gui.client.pages.submittermodify;

import dk.dbc.dataio.gui.client.presenters.GenericPresenter;

public interface Presenter extends GenericPresenter {
    void keyPressed();
    void numberChanged(String number);
    void nameChanged(String name);
    void descriptionChanged(String description);
    void saveButtonPressed();
}
