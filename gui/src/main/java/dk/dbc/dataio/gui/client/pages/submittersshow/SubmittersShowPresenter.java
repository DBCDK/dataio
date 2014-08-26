package dk.dbc.dataio.gui.client.pages.submittersshow;

import dk.dbc.dataio.commons.types.Submitter;
import dk.dbc.dataio.gui.client.presenters.GenericPresenter;

public interface SubmittersShowPresenter extends GenericPresenter {
    void editSubmitter(Submitter submitter);
}
