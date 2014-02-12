package dk.dbc.dataio.gui.client.pages.submittercreate;

import dk.dbc.dataio.gui.client.presenters.Presenter;

public interface SubmitterCreatePresenter extends Presenter {
    void saveSubmitter(String name, String number, String description);
}
