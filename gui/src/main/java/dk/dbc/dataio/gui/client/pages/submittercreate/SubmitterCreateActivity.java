package dk.dbc.dataio.gui.client.pages.submittercreate;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import dk.dbc.dataio.commons.types.Submitter;
import dk.dbc.dataio.commons.types.SubmitterContent;
import dk.dbc.dataio.gui.client.exceptions.FilteredAsyncCallback;
import dk.dbc.dataio.gui.client.exceptions.ProxyError;
import dk.dbc.dataio.gui.client.exceptions.ProxyException;
import dk.dbc.dataio.gui.client.proxies.FlowStoreProxyAsync;
import dk.dbc.dataio.gui.util.ClientFactory;

/**
 * This class represents the create submitter activity encompassing saving
 * of submitter data in the flow store via RPC proxy
 */
public class SubmitterCreateActivity extends AbstractActivity implements SubmitterCreatePresenter {
    private ClientFactory clientFactory;
    private SubmitterCreateView submitterCreateView;
    private FlowStoreProxyAsync flowStoreProxy;

    public SubmitterCreateActivity(ClientFactory clientFactory) {
        this.clientFactory = clientFactory;
        flowStoreProxy = clientFactory.getFlowStoreProxyAsync();
    }

    @Override
    public void bind() {
        submitterCreateView = clientFactory.getSubmitterCreateView();
        submitterCreateView.setPresenter(this);
    }

    @Override
    public void saveSubmitter(String name, String number, String description) {
        final SubmitterContent submitterContent = new SubmitterContent(Long.parseLong(number), name, description);

        flowStoreProxy.createSubmitter(submitterContent, new FilteredAsyncCallback<Submitter>() {
            @Override
            public void onFilteredFailure(Throwable e) {
                submitterCreateView.onFlowStoreProxyFailure(getErrorCode(e), e.getMessage());
            }

            @Override
            public void onSuccess(Submitter submitter) {
                submitterCreateView.onSaveSubmitterSuccess();
            }
        });
    }

    @Override
    public void start(AcceptsOneWidget containerWidget, EventBus eventBus) {
        bind();
        containerWidget.setWidget(submitterCreateView.asWidget());
        submitterCreateView.clearFields();
    }

    private ProxyError getErrorCode(Throwable e) {
        ProxyError errorCode = null;
        if (e instanceof ProxyException) {
            errorCode = ((ProxyException) e).getErrorCode();
        }
        return errorCode;
    }

}
