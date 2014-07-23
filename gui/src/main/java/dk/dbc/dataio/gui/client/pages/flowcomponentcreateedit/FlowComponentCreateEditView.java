package dk.dbc.dataio.gui.client.pages.flowcomponentcreateedit;

import com.google.gwt.user.client.ui.IsWidget;
import dk.dbc.dataio.commons.types.RevisionInfo;
import dk.dbc.dataio.gui.client.exceptions.JavaScriptProjectFetcherError;
import dk.dbc.dataio.gui.client.views.View;
import java.util.List;

public interface FlowComponentCreateEditView extends IsWidget, View<FlowComponentCreateEditPresenter> {
    void setAvailableRevisions(List<RevisionInfo> availableRevisions);
    void setAvailableScriptNames(List<String> availableScriptNames);
    void setAvailableInvocationMethods(List<String> availableInvocationMethods);
    void fetchRevisionFailed(JavaScriptProjectFetcherError errorCode, String detail);
    void fetchScriptNamesFailed(String string);
    void fetchInvocationMethodsFailed(JavaScriptProjectFetcherError errorCode, String string);
}
