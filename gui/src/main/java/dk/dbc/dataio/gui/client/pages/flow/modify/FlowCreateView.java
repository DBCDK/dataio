package dk.dbc.dataio.gui.client.pages.flow.modify;

import com.google.gwt.user.client.ui.IsWidget;
import dk.dbc.dataio.gui.client.views.GenericView;
import java.util.Map;

public interface FlowCreateView extends IsWidget, GenericView<FlowCreatePresenter> {
    void clearFields();
    void setAvailableFlowComponents(Map<String, String> availableFlowComponents);
}
