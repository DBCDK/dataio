package dk.dbc.dataio.gui.client.pages.submitter.show;

import dk.dbc.dataio.gui.client.model.SubmitterModel;
import dk.dbc.dataio.gui.client.presenters.GenericPresenter;

public interface Presenter extends GenericPresenter {
    void editSubmitter(SubmitterModel model);
    void createSubmitter();
}
