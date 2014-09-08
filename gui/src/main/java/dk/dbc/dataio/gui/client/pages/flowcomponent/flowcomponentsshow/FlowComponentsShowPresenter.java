package dk.dbc.dataio.gui.client.pages.flowcomponent.flowcomponentsshow;

import dk.dbc.dataio.commons.types.FlowComponent;
import dk.dbc.dataio.gui.client.presenters.GenericPresenter;

public interface FlowComponentsShowPresenter extends GenericPresenter {
    void editFlowComponent(FlowComponent flowComponent);
}
