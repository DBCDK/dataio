package dk.dbc.dataio.gui.client.activities;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import dk.dbc.dataio.gui.client.places.FlowComponentCreatePlace;
import dk.dbc.dataio.gui.client.presenters.FlowComponentCreatePresenter;
import dk.dbc.dataio.gui.client.views.FlowComponentCreateView;
import dk.dbc.dataio.gui.util.ClientFactory;

/**
 * This class represents the create flow activity encompassing saving
 * of flow data in the flow store via RPC proxy
 */
public class CreateFlowComponentActivity extends AbstractActivity implements FlowComponentCreatePresenter {
    private ClientFactory clientFactory;
    private FlowComponentCreateView flowComponentCreateView;
    //private FlowStoreProxyAsync flowStoreProxy;

    public CreateFlowComponentActivity(FlowComponentCreatePlace place, ClientFactory clientFactory) {
        this.clientFactory = clientFactory;
        //flowStoreProxy = clientFactory.getFlowStoreProxyAsync();
    }

    @Override
    public void bind() {
        flowComponentCreateView = clientFactory.getFlowComponentCreateView();
        flowComponentCreateView.setPresenter(this);
    }

    @Override
    public void reload() {
		flowComponentCreateView.refresh();
    }
    
    @Override
    public void start(AcceptsOneWidget containerWidget, EventBus eventBus) {
        bind();
        containerWidget.setWidget(flowComponentCreateView.asWidget());
    }
}
