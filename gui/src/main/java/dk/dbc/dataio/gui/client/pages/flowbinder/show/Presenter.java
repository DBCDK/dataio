package dk.dbc.dataio.gui.client.pages.flowbinder.show;

import dk.dbc.dataio.gui.client.model.FlowBinderModel;
import dk.dbc.dataio.gui.client.presenters.GenericPresenter;

public interface Presenter extends GenericPresenter {
    void editFlowBinder(FlowBinderModel model);
}
