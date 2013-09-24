package dk.dbc.dataio.gui.client.views;

import com.google.gwt.user.client.ui.IsWidget;
import dk.dbc.dataio.gui.client.presenters.FlowCreatePresenter;
import java.util.Map;

public interface FlowCreateView extends IsWidget, View<FlowCreatePresenter> {
    public void setAvailableFlowComponents(Map<String, String> availableFlowComponents);
}
