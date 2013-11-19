package dk.dbc.dataio.gui.client.activities;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import dk.dbc.dataio.commons.types.FlowComponent;
import dk.dbc.dataio.commons.types.FlowContent;
import dk.dbc.dataio.gui.client.i18n.FlowCreateConstants;
import dk.dbc.dataio.gui.client.places.FlowCreatePlace;
import dk.dbc.dataio.gui.client.presenters.FlowCreatePresenter;
import dk.dbc.dataio.gui.client.proxies.FlowStoreProxyAsync;
import dk.dbc.dataio.gui.client.views.FlowCreateView;
import dk.dbc.dataio.gui.util.ClientFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class represents the create flow activity encompassing saving
 * of flow data in the flow store via RPC proxy
 */
public class CreateFlowActivity extends AbstractActivity implements FlowCreatePresenter {
    private final FlowCreateConstants constants = GWT.create(FlowCreateConstants.class);
    private ClientFactory clientFactory;
    private FlowCreateView flowCreateView;
    private FlowStoreProxyAsync flowStoreProxy;
    private Map<String, FlowComponent> availableFlowComponents = new HashMap<String, FlowComponent>();

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
    public void start(AcceptsOneWidget containerWidget, EventBus eventBus) {
        bind();
        containerWidget.setWidget(flowCreateView.asWidget());
        flowStoreProxy.findAllComponents(new AsyncCallback<List<FlowComponent>>() {
            @Override
            public void onFailure(Throwable e) {
                flowCreateView.onFailure(e.getClass().getName() + " - " + e.getMessage() + " - " + Arrays.toString(e.getStackTrace()));
            }
            @Override
            public void onSuccess(List<FlowComponent> result) {
                Map<String, String> flowComponentsToView = new HashMap<String, String>();
                for (FlowComponent component : result) {
                    String key = Long.toString(component.getId());
                    try {
                        availableFlowComponents.put(key, component);
                        flowComponentsToView.put(key, component.getContent().getName());
                    } catch (Exception e) {
                        flowCreateView.onFailure(e.getClass().getName() + " - " + e.getMessage() + " - " + Arrays.toString(e.getStackTrace()));
                    }
                }
                flowCreateView.setAvailableFlowComponents(flowComponentsToView);
            }

        });
    }
 
    @Override
    public void saveFlow(String name, String description, Collection<String> selectedFlowComponents) {
        final List<FlowComponent> flowComponents = new ArrayList<FlowComponent>();
        for (String flowComponentId: selectedFlowComponents) {
            flowComponents.add(availableFlowComponents.get(flowComponentId));
        }
        final FlowContent flowContent = new FlowContent(name, description, flowComponents);
        flowStoreProxy.createFlow(flowContent, new AsyncCallback<Void>() {
            @Override
            public void onFailure(Throwable e) {
                flowCreateView.onFailure(e.getClass().getName() + " - " + e.getMessage() + " - " + Arrays.toString(e.getStackTrace()));
            }
            @Override
            public void onSuccess(Void aVoid) {
                flowCreateView.onSuccess(constants.status_FlowSuccessfullySaved());
            }
        });
    }
    
}
