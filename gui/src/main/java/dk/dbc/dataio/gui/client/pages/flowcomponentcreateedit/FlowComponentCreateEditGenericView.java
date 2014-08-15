package dk.dbc.dataio.gui.client.pages.flowcomponentcreateedit;

import com.google.gwt.user.client.ui.IsWidget;
import dk.dbc.dataio.commons.types.FlowComponent;
import dk.dbc.dataio.commons.types.RevisionInfo;
import dk.dbc.dataio.gui.client.exceptions.JavaScriptProjectFetcherError;
import dk.dbc.dataio.gui.client.views.GenericView;

import java.util.List;

public interface FlowComponentCreateEditGenericView extends IsWidget, GenericView<FlowComponentCreateEditGenericPresenter> {
    void setAvailableRevisions(List<RevisionInfo> availableRevisions, int currentRevision);
    void setAvailableScriptNames(List<String> availableScriptNames, String currentScriptName);
    void setAvailableInvocationMethods(List<String> availableInvocationMethods, String currentInvocationMethod);
    void fetchRevisionFailed(JavaScriptProjectFetcherError errorCode, String detail);
    void fetchScriptNamesFailed(String string);
    void fetchInvocationMethodsFailed(JavaScriptProjectFetcherError errorCode, String string);
    void initializeFields(String header, FlowComponent flowComponent);
    void onSaveFlowComponentSuccess();
}
