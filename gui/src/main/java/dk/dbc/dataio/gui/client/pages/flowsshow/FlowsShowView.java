package dk.dbc.dataio.gui.client.pages.flowsshow;

import com.google.gwt.user.client.ui.IsWidget;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.gui.client.views.GenericView;
import java.util.List;

public interface FlowsShowView extends IsWidget, GenericView<FlowsShowPresenter> {
    void setFlows(List<Flow> flow);
}
