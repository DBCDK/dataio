package dk.dbc.dataio.gui.client.views;

import com.google.gwt.user.client.ui.IsWidget;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.gui.client.presenters.FlowsShowPresenter;
import java.util.List;

public interface FlowsShowView extends IsWidget, View<FlowsShowPresenter> {
    void setFlows(List<Flow> flow);
}
