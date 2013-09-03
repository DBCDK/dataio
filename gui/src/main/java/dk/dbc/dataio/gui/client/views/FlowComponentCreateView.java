package dk.dbc.dataio.gui.client.views;

import com.google.gwt.user.client.ui.IsWidget;
import dk.dbc.dataio.commons.types.RevisionInfo;
import dk.dbc.dataio.gui.client.presenters.FlowComponentCreatePresenter;
import java.util.List;

public interface FlowComponentCreateView extends IsWidget, View<FlowComponentCreatePresenter> {
    public void setAvailableRevisions(List<RevisionInfo> availableRevisions);
    public void setAvailableScriptNames(List<String> availableScriptNames);
    public void setAvailableInvocationMethods(List<String> availableInvocationMethods);
    public void disableRevisionEntry();
    public void disableScriptNameEntry();
    public void disableInvocationMethodEntry();
}
