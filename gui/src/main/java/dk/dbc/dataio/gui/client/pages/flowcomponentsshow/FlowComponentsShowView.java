package dk.dbc.dataio.gui.client.pages.flowcomponentsshow;

import com.google.gwt.user.client.ui.IsWidget;
import dk.dbc.dataio.commons.types.FlowComponent;
import dk.dbc.dataio.gui.client.views.GenericView;
import java.util.List;

public interface FlowComponentsShowView extends IsWidget, GenericView<FlowComponentsShowPresenter> {
    void setFlowComponents(List<FlowComponent> flowComponents);
}
