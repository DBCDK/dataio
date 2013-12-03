package dk.dbc.dataio.gui.client.activities;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import dk.dbc.dataio.commons.types.FlowComponent;
import dk.dbc.dataio.gui.client.i18n.FlowComponentsShowConstants;
import dk.dbc.dataio.gui.client.places.FlowComponentsShowPlace;
import dk.dbc.dataio.gui.client.presenters.FlowComponentsShowPresenter;
import dk.dbc.dataio.gui.client.proxies.FlowStoreProxyAsync;
import dk.dbc.dataio.gui.client.views.FlowComponentsShowView;
import dk.dbc.dataio.gui.util.ClientFactory;
import java.util.List;


/**
 * This class represents the show flows activity 
 */
public class ShowFlowComponentsActivity extends AbstractActivity implements FlowComponentsShowPresenter {
//    private final FlowComponentsShowConstants constants = GWT.create(FlowComponentsShowConstants.class);
    private ClientFactory clientFactory;
    private FlowComponentsShowView flowComponentsShowView;
    private FlowStoreProxyAsync flowStoreProxy;
    
    public ShowFlowComponentsActivity(FlowComponentsShowPlace place, ClientFactory clientFactory) {
        this.clientFactory = clientFactory;
        flowStoreProxy = clientFactory.getFlowStoreProxyAsync();
    }

    @Override
    public void bind() {
        flowComponentsShowView = clientFactory.getFlowComponentsShowView();
        flowComponentsShowView.setPresenter(this);
    }

    @Override
    public void reload() {
		flowComponentsShowView.refresh();
    }
    
    @Override
    public void start(AcceptsOneWidget containerWidget, EventBus eventBus) {
        bind();
        containerWidget.setWidget(flowComponentsShowView.asWidget());
        fetchFlowComponents();
    }
    
    
    // Local methods
    private void fetchFlowComponents() {
        flowStoreProxy.findAllComponents(new AsyncCallback<List<FlowComponent>>() {
            @Override
            public void onFailure(Throwable e) {
                flowComponentsShowView.onFailure(e.getClass().getName() + " - " + e.getMessage());
            }
            @Override
            public void onSuccess(List<FlowComponent> flowComponents) {
                flowComponentsShowView.setFlowComponents(flowComponents);
            }
        });
    }

}
