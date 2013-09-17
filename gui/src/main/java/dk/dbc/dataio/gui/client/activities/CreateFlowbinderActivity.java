package dk.dbc.dataio.gui.client.activities;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import dk.dbc.dataio.gui.client.exceptions.FlowStoreProxyError;
import dk.dbc.dataio.gui.client.exceptions.FlowStoreProxyException;
import dk.dbc.dataio.gui.client.places.FlowbinderCreatePlace;
import dk.dbc.dataio.gui.client.presenters.FlowbinderCreatePresenter;
import dk.dbc.dataio.gui.client.proxies.FlowStoreProxyAsync;
import dk.dbc.dataio.gui.client.views.FlowbinderCreateView;
import dk.dbc.dataio.gui.util.ClientFactory;

/**
 * This class represents the create flowbinder activity encompassing saving
 * of flowbinder data in the flow store via RPC proxy
 */
public class CreateFlowbinderActivity extends AbstractActivity implements FlowbinderCreatePresenter {
    private ClientFactory clientFactory;
    private FlowbinderCreateView flowbinderCreateView;
    private FlowStoreProxyAsync flowStoreProxy;

    public CreateFlowbinderActivity(FlowbinderCreatePlace place, ClientFactory clientFactory) {
        this.clientFactory = clientFactory;
        flowStoreProxy = clientFactory.getFlowStoreProxyAsync();
    }

    @Override
    public void bind() {
        flowbinderCreateView = clientFactory.getFlowbinderCreateView();
        flowbinderCreateView.setPresenter(this);
    }

    @Override
    public void reload() {
		flowbinderCreateView.refresh();
    }

    @Override
    public void saveFlowbinder(String name, String number, String description) {
//        final SubmitterContent submitterContent = new SubmitterContent(Long.valueOf(number), name, description);
//
//        flowStoreProxy.createSubmitter(submitterContent, new AsyncCallback<Void>() {
//            @Override
//            public void onFailure(Throwable e) {
//                submitterCreateView.onFlowStoreProxyFailure(getErrorCode(e), e.getMessage());
//            }
//
//            @Override
//            public void onSuccess(Void aVoid) {
//                submitterCreateView.onSaveSubmitterSuccess();
//            }
//        });
    }

    @Override
    public void start(AcceptsOneWidget containerWidget, EventBus eventBus) {
        bind();
        containerWidget.setWidget(flowbinderCreateView.asWidget());
    }

    private FlowStoreProxyError getErrorCode(Throwable e) {
        FlowStoreProxyError errorCode = null;
        if (e instanceof FlowStoreProxyException) {
            errorCode = ((FlowStoreProxyException) e).getErrorCode();
        }
        return errorCode;
    }

}
