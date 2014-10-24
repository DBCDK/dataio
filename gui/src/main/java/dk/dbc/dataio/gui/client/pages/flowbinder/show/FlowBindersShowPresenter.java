package dk.dbc.dataio.gui.client.pages.flowbinder.show;

import dk.dbc.dataio.gui.client.presenters.GenericPresenter;
import dk.dbc.dataio.gui.types.FlowBinderContentViewData;

public interface FlowBindersShowPresenter extends GenericPresenter {
    void updateFlowBinder(FlowBinderContentViewData content);
}
