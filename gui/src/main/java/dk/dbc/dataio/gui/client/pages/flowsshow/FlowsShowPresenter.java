package dk.dbc.dataio.gui.client.pages.flowsshow;

import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.gui.client.presenters.Presenter;

public interface FlowsShowPresenter extends Presenter {
    void updateFlow(Flow flow);
}
