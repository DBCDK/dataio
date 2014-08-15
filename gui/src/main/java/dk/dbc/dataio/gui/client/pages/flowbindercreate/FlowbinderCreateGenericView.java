package dk.dbc.dataio.gui.client.pages.flowbindercreate;

import com.google.gwt.user.client.ui.IsWidget;
import dk.dbc.dataio.gui.client.views.GenericView;
import java.util.Map;

public interface FlowbinderCreateGenericView extends IsWidget, GenericView<FlowbinderCreateGenericPresenter> {
    void setAvailableFlows(Map<String, String> availableFlows);
    void setAvailableSubmitters(Map<String, String> availableSubmitters);
    void setAvailableSinks(Map<String, String> availableSinks);
}

