package dk.dbc.dataio.gui.client.pages.flowcreate;

import com.google.gwt.user.client.ui.IsWidget;
import dk.dbc.dataio.gui.client.views.View;
import java.util.Map;

public interface FlowCreateView extends IsWidget, View<FlowCreatePresenter> {
    void setAvailableFlowComponents(Map<String, String> availableFlowComponents);
}
