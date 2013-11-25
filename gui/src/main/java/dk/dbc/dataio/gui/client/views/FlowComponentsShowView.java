package dk.dbc.dataio.gui.client.views;

import com.google.gwt.user.client.ui.IsWidget;
import dk.dbc.dataio.commons.types.FlowComponent;
import dk.dbc.dataio.gui.client.presenters.FlowComponentsShowPresenter;
import java.util.List;

public interface FlowComponentsShowView extends IsWidget, View<FlowComponentsShowPresenter> {
    void setFlowComponents(List<FlowComponent> flowComponents);
}
