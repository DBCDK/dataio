package dk.dbc.dataio.gui.client.activities;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import dk.dbc.dataio.commons.types.SubmitterContent;
import dk.dbc.dataio.gui.client.exceptions.FilteredAsyncCallback;
import dk.dbc.dataio.gui.client.exceptions.FlowStoreProxyError;
import dk.dbc.dataio.gui.client.exceptions.FlowStoreProxyException;
import dk.dbc.dataio.gui.client.presenters.SubmitterCreatePresenter;
import dk.dbc.dataio.gui.client.proxies.FlowStoreProxyAsync;
import dk.dbc.dataio.gui.client.views.SubmitterCreateView;
import dk.dbc.dataio.gui.util.ClientFactory;

/**
 * This class represents the create submitter activity encompassing saving
 * of submitter data in the flow store via RPC proxy
 */
public class CreateSubmitterActivity extends AbstractActivity implements SubmitterCreatePresenter {
    private ClientFactory clientFactory;
    private SubmitterCreateView submitterCreateView;
    private FlowStoreProxyAsync flowStoreProxy;

    public CreateSubmitterActivity(/*SubmitterCreatePlace place,*/ ClientFactory clientFactory) {
        this.clientFactory = clientFactory;
        flowStoreProxy = clientFactory.getFlowStoreProxyAsync();
    }

    @Override
    public void bind() {
        submitterCreateView = clientFactory.getSubmitterCreateView();
        submitterCreateView.setPresenter(this);
    }

    @Override
    public void reload() {
		submitterCreateView.refresh();
    }

    @Override
    public void saveSubmitter(String name, String number, String description) {
        final SubmitterContent submitterContent = new SubmitterContent(Long.parseLong(number), name, description);

        flowStoreProxy.createSubmitter(submitterContent, new FilteredAsyncCallback<Void>() {
            @Override
            public void onFilteredFailure(Throwable e) {
                submitterCreateView.onFlowStoreProxyFailure(getErrorCode(e), e.getMessage());
            }

            @Override
            public void onSuccess(Void aVoid) {
                submitterCreateView.onSaveSubmitterSuccess();
            }
        });
    }

    @Override
    public void start(AcceptsOneWidget containerWidget, EventBus eventBus) {
        bind();
        containerWidget.setWidget(submitterCreateView.asWidget());
    }

    private FlowStoreProxyError getErrorCode(Throwable e) {
        FlowStoreProxyError errorCode = null;
        if (e instanceof FlowStoreProxyException) {
            errorCode = ((FlowStoreProxyException) e).getErrorCode();
        }
        return errorCode;
    }

}
