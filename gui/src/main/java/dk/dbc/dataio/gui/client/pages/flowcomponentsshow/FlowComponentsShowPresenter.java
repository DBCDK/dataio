package dk.dbc.dataio.gui.client.pages.flowcomponentsshow;

import dk.dbc.dataio.commons.types.FlowComponent;
import dk.dbc.dataio.gui.client.presenters.Presenter;

public interface FlowComponentsShowPresenter extends Presenter {
    void editFlowComponent(FlowComponent flowComponent);
}
