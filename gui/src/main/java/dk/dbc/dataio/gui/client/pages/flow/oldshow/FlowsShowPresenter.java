package dk.dbc.dataio.gui.client.pages.flow.oldshow;

import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.gui.client.presenters.GenericPresenter;

public interface FlowsShowPresenter extends GenericPresenter {
    void refreshFlowComponents(Flow flow);
    void updateFlow(Flow flow);
}
