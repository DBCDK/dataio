package dk.dbc.dataio.gui.client.pages.submitter.show;

import dk.dbc.dataio.gui.client.model.SubmitterModel;
import dk.dbc.dataio.gui.client.presenters.GenericPresenter;

import java.util.Map;

public interface Presenter extends GenericPresenter {
    void showFlowBinders(SubmitterModel model);

    void editSubmitter(SubmitterModel model);

    void createSubmitter();

    void copyFlowBinderListToClipboard(Map<String, String> flowBinders);
}
