package dk.dbc.dataio.gui.client.pages.flowsshow;

import com.google.gwt.user.client.ui.IsWidget;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.gui.client.views.GenericView;
import java.util.List;

public interface FlowsShowGenericView extends IsWidget, GenericView<FlowsShowGenericPresenter> {
    void setFlows(List<Flow> flow);
}
