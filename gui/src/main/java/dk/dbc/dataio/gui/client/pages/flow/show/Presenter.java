package dk.dbc.dataio.gui.client.pages.flow.show;

import dk.dbc.dataio.gui.client.model.FlowModel;
import dk.dbc.dataio.gui.client.presenters.GenericPresenter;

public interface Presenter extends GenericPresenter {
    void refreshFlowComponents(FlowModel model);

    void editFlow(FlowModel flow);

    void createFlow();
}
