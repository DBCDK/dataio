package dk.dbc.dataio.gui.client.pages.submittermodify;

import dk.dbc.dataio.gui.client.presenters.GenericPresenter;

public interface Presenter extends GenericPresenter {
    public void numberChanged(String number);
    public void nameChanged(String name);
    public void descriptionChanged(String description);
    public void keyPressed();
    public void saveButtonPressed();
}
