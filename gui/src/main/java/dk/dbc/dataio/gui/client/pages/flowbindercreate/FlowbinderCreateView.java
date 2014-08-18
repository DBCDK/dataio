package dk.dbc.dataio.gui.client.pages.flowbindercreate;

import com.google.gwt.user.client.ui.IsWidget;
import dk.dbc.dataio.gui.client.views.GenericView;
import java.util.Map;

public interface FlowbinderCreateView extends IsWidget, GenericView<FlowbinderCreatePresenter> {
    void clearFields();
    void setAvailableFlows(Map<String, String> availableFlows);
    void setAvailableSubmitters(Map<String, String> availableSubmitters);
    void setAvailableSinks(Map<String, String> availableSinks);
}

