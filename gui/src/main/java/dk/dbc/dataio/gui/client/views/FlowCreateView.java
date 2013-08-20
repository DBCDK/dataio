package dk.dbc.dataio.gui.client.views;

import com.google.gwt.user.client.ui.IsWidget;
import dk.dbc.dataio.gui.client.presenters.FlowCreatePresenter;

public interface FlowCreateView extends IsWidget, View<FlowCreatePresenter> {
    void setData(String name, String description);

    public void setAvailableItem(String name, String name0);
}
