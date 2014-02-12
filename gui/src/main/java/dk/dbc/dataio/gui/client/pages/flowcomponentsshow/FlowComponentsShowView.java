package dk.dbc.dataio.gui.client.pages.flowcomponentsshow;

import com.google.gwt.user.client.ui.IsWidget;
import dk.dbc.dataio.commons.types.FlowComponent;
import dk.dbc.dataio.gui.client.views.View;
import java.util.List;

public interface FlowComponentsShowView extends IsWidget, View<FlowComponentsShowPresenter> {
    void setFlowComponents(List<FlowComponent> flowComponents);
}
