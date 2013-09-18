package dk.dbc.dataio.gui.client.views;

import com.google.gwt.user.client.ui.IsWidget;
import dk.dbc.dataio.gui.client.presenters.FlowbinderCreatePresenter;
import java.util.List;

public interface FlowbinderCreateView extends IsWidget, View<FlowbinderCreatePresenter> {
    public void setAvailableFlow(String key, String flow);
    public String getSelectedFlow();
    public void setAvailableSubmitter(String key, String value);
    public void clearAvailableSubmitters();
    public List<String> getSelectedSubmitters();
}

