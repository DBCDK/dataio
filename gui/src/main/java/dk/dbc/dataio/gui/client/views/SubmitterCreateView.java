package dk.dbc.dataio.gui.client.views;

import com.google.gwt.user.client.ui.IsWidget;
import dk.dbc.dataio.gui.client.exceptions.FlowStoreProxyError;
import dk.dbc.dataio.gui.client.presenters.SubmitterCreatePresenter;

public interface SubmitterCreateView extends IsWidget, View<SubmitterCreatePresenter> {
    void onFlowStoreProxyFailure(FlowStoreProxyError errorCode, String detail);
    void onSaveSubmitterSuccess();
}
