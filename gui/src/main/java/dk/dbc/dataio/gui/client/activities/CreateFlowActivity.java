package dk.dbc.dataio.gui.client.activities;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import dk.dbc.dataio.engine.FlowComponent;
import dk.dbc.dataio.engine.FlowContent;
import dk.dbc.dataio.gui.client.places.FlowCreatePlace;
import dk.dbc.dataio.gui.client.presenters.FlowCreatePresenter;
import dk.dbc.dataio.gui.client.proxies.FlowStoreProxyAsync;
import dk.dbc.dataio.gui.client.views.FlowCreateView;
import dk.dbc.dataio.gui.client.views.FlowCreateViewImpl;
import dk.dbc.dataio.gui.util.ClientFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class represents the create flow activity encompassing saving
 * of flow data in the flow store via RPC proxy
 */
public class CreateFlowActivity extends AbstractActivity implements FlowCreatePresenter {
    private ClientFactory clientFactory;
    private FlowCreateView flowCreateView;
    private FlowStoreProxyAsync flowStoreProxy;
    private Map<String, FlowComponent> availableComponents = new HashMap<String, FlowComponent>();

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
        final List<FlowComponent> flowComponents = new ArrayList<FlowComponent>();
        for (String component : flowCreateView.getSelectedItems()) {
            flowComponents.add(availableComponents.get(component));
        }
        final FlowContent flowContent = new FlowContent(name, description, flowComponents);
        flowStoreProxy.createFlow(flowContent, new AsyncCallback<Void>() {
            @Override
            public void onFailure(Throwable e) {
                final String errorClassName = e.getClass().getName();
                flowCreateView.displayError(errorClassName + " - " + e.getMessage() + " - " + Arrays.toString(e.getStackTrace()));
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
        flowCreateView.clearAvailableItems();
        flowStoreProxy.findAllComponents(new AsyncCallback<List<FlowComponent>>() {
            @Override
            public void onFailure(Throwable e) {
                flowCreateView.displayError(e.getClass().getName() + " - " + e.getMessage());
            }

            @Override
            public void onSuccess(List<FlowComponent> result) {
                for (FlowComponent component : result) {
                    String key = Long.toString(component.getId()) + "-" + Long.toString(component.getVersion());
                    try {
                        availableComponents.put(key, component);
                        flowCreateView.setAvailableItem(component.getContent().getName(), key);
                    } catch (Exception e) {
                        flowCreateView.displayError(e.getClass().getName() + " - " + e.getMessage());
                    }
                }
            }

        });
    }
}
