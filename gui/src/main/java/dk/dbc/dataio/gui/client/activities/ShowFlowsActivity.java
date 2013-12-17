package dk.dbc.dataio.gui.client.activities;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.gui.client.presenters.FlowsShowPresenter;
import dk.dbc.dataio.gui.client.proxies.FlowStoreProxyAsync;
import dk.dbc.dataio.gui.client.views.FlowsShowView;
import dk.dbc.dataio.gui.util.ClientFactory;
import java.util.List;


/**
 * This class represents the show flows activity
 */
public class ShowFlowsActivity extends AbstractActivity implements FlowsShowPresenter {
    private ClientFactory clientFactory;
    private FlowsShowView flowsShowView;
    private FlowStoreProxyAsync flowStoreProxy;

    public ShowFlowsActivity(/*FlowsShowPlace place,*/ ClientFactory clientFactory) {
        this.clientFactory = clientFactory;
        flowStoreProxy = clientFactory.getFlowStoreProxyAsync();
    }

    @Override
    public void bind() {
        flowsShowView = clientFactory.getFlowsShowView();
        flowsShowView.setPresenter(this);
    }

    @Override
    public void reload() {
		flowsShowView.refresh();
    }

    @Override
    public void start(AcceptsOneWidget containerWidget, EventBus eventBus) {
        bind();
        containerWidget.setWidget(flowsShowView.asWidget());
        fetchFlows();
    }


    // Local methods
    private void fetchFlows() {
        flowStoreProxy.findAllFlows(new AsyncCallback<List<Flow>>() {
            @Override
            public void onFailure(Throwable e) {
                flowsShowView.onFailure(e.getClass().getName() + " - " + e.getMessage());
            }
            @Override
            public void onSuccess(List<Flow> flows) {
                flowsShowView.setFlows(flows);
            }
        });
    }

}
