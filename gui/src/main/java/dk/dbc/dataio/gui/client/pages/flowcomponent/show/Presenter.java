package dk.dbc.dataio.gui.client.pages.flowcomponent.show;

import dk.dbc.dataio.gui.client.model.FlowComponentModel;
import dk.dbc.dataio.gui.client.presenters.GenericPresenter;

public interface Presenter extends GenericPresenter {
    void editFlowComponent(FlowComponentModel flowComponent);

    void createFlowComponent();
}
