package dk.dbc.dataio.gui.client.pages.submittercreate;

import dk.dbc.dataio.gui.client.presenters.GenericPresenter;

public interface SubmitterCreatePresenter extends GenericPresenter {
    void saveSubmitter(String name, String number, String description);
}
