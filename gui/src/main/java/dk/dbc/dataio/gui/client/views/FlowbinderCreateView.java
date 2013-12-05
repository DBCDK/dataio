package dk.dbc.dataio.gui.client.views;

import com.google.gwt.user.client.ui.IsWidget;
import dk.dbc.dataio.gui.client.presenters.FlowbinderCreatePresenter;
import java.util.Map;

public interface FlowbinderCreateView extends IsWidget, View<FlowbinderCreatePresenter> {
    void setAvailableFlows(Map<String, String> availableFlows);
    void setAvailableSubmitters(Map<String, String> availableSubmitters);
    void setAvailableSinks(Map<String, String> availableSinks);
}

