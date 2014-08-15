package dk.dbc.dataio.gui.client.pages.submittercreate;

import com.google.gwt.user.client.ui.IsWidget;
import dk.dbc.dataio.gui.client.exceptions.ProxyError;
import dk.dbc.dataio.gui.client.views.GenericView;

public interface SubmitterCreateGenericView extends IsWidget, GenericView<SubmitterCreateGenericPresenter> {
    void onFlowStoreProxyFailure(ProxyError errorCode, String detail);
    void onSaveSubmitterSuccess();
}
