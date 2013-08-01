package dk.dbc.dataio.gui.client.activities;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import dk.dbc.dataio.gui.client.model.FlowData;
import dk.dbc.dataio.gui.client.places.FlowCreatePlace;
import dk.dbc.dataio.gui.client.presenters.FlowCreatePresenter;
import dk.dbc.dataio.gui.client.proxies.FlowStoreProxyAsync;
import dk.dbc.dataio.gui.client.views.FlowCreateView;
import dk.dbc.dataio.gui.client.views.FlowCreateViewImpl;
import dk.dbc.dataio.gui.util.ClientFactory;

/**
 * This class represents the create flow activity encompassing saving
 * of flow data in the flow store via RPC proxy
 */
public class CreateFlowActivity extends AbstractActivity implements FlowCreatePresenter {
    private ClientFactory clientFactory;
    private FlowCreateView flowCreateView;
    private FlowStoreProxyAsync flowStoreProxy;

    public CreateFlowActivity(FlowCreatePlace place, ClientFactory clientFactory) {
        this.clientFactory = clientFactory;
        flowStoreProxy = clientFactory.getFlowStoreProxyAsync();
    }

    @Override
    public void bind() {
        flowCreateView = clientFactory.getFlowCreateView();
        flowCreateView.setPresenter(this);
    }

    @Override
    public void reload() {
		flowCreateView.refresh();
    }

    @Override
    public void saveFlow(String name, String description) {
        final FlowData flowData = new FlowData();
        flowData.setName(name);
        flowData.setDescription(description);

        flowStoreProxy.createFlow(flowData, new AsyncCallback<Void>() {
            @Override
            public void onFailure(Throwable e) {
                final String errorClassName = e.getClass().getName();
                flowCreateView.displayError(errorClassName + " - " + e.getMessage());
            }

            @Override
            public void onSuccess(Void aVoid) {
                flowCreateView.displaySuccess(FlowCreateViewImpl.SAVE_RESULT_LABEL_SUCCES_MESSAGE);
            }
        });
    }

    @Override
    public void start(AcceptsOneWidget containerWidget, EventBus eventBus) {
        bind();
        containerWidget.setWidget(flowCreateView.asWidget());
    }
}
