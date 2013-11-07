package dk.dbc.dataio.gui.client.views;

import com.google.gwt.user.client.ui.IsWidget;
import dk.dbc.dataio.gui.client.presenters.FlowbinderCreatePresenter;
import java.util.Map;

public interface FlowbinderCreateView extends IsWidget, View<FlowbinderCreatePresenter> {
    public void setAvailableFlows(Map<String, String> availableFlows);
    public void setAvailableSubmitters(Map<String, String> availableSubmitters);
    public void setAvailableSinks(Map<String, String> availableSinks);
}

