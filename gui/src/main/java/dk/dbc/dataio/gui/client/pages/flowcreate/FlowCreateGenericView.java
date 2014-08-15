package dk.dbc.dataio.gui.client.pages.flowcreate;

import com.google.gwt.user.client.ui.IsWidget;
import dk.dbc.dataio.gui.client.views.GenericView;
import java.util.Map;

public interface FlowCreateGenericView extends IsWidget, GenericView<FlowCreateGenericPresenter> {
    void setAvailableFlowComponents(Map<String, String> availableFlowComponents);
}
