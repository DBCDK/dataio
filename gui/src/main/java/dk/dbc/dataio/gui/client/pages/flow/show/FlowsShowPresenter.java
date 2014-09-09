package dk.dbc.dataio.gui.client.pages.flow.show;

import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.gui.client.presenters.GenericPresenter;

public interface FlowsShowPresenter extends GenericPresenter {
    void updateFlowComponentsInFlowToLatestVersion(Flow flow);
}
