package dk.dbc.dataio.gui.client.pages.flow.oldshow;

import com.google.gwt.user.client.ui.IsWidget;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.gui.client.views.GenericView;
import java.util.List;

public interface FlowsShowView extends IsWidget, GenericView<FlowsShowPresenter> {
    void clearFields();
    void setFlows(List<Flow> flow);
}
