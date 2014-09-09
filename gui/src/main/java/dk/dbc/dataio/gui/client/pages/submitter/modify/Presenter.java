package dk.dbc.dataio.gui.client.pages.submitter.modify;

import dk.dbc.dataio.gui.client.presenters.GenericPresenter;

public interface Presenter extends GenericPresenter {
    void numberChanged(String number);
    void nameChanged(String name);
    void descriptionChanged(String description);
    void keyPressed();
    void saveButtonPressed();
}
