package dk.dbc.dataio.gui.util;

import com.google.gwt.place.shared.PlaceController;
import com.google.web.bindery.event.shared.EventBus;
import com.google.web.bindery.event.shared.SimpleEventBus;
import dk.dbc.dataio.gui.client.proxies.FlowStoreProxy;
import dk.dbc.dataio.gui.client.proxies.FlowStoreProxyAsync;
import dk.dbc.dataio.gui.client.views.FlowComponentCreateView;
import dk.dbc.dataio.gui.client.views.FlowComponentCreateViewImpl;
import dk.dbc.dataio.gui.client.views.FlowCreateView;
import dk.dbc.dataio.gui.client.views.FlowCreateViewImpl;
import dk.dbc.dataio.gui.client.views.SubmitterCreateView;
import dk.dbc.dataio.gui.client.views.SubmitterCreateViewImpl;


public class ClientFactoryImpl implements ClientFactory {
    private final EventBus eventBus = new SimpleEventBus();
    private final FlowCreateView flowCreateView = new FlowCreateViewImpl();
    private final FlowComponentCreateView flowComponentCreateView = new FlowComponentCreateViewImpl();
    private final SubmitterCreateView submitterCreateView = new SubmitterCreateViewImpl();
    private final PlaceController placeController = new PlaceController(eventBus);
    private final FlowStoreProxyAsync flowStoreProxyAsync = FlowStoreProxy.Factory.getAsyncInstance();

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    @Override
    public FlowCreateView getFlowCreateView() {
        return flowCreateView;
    }

    @Override
    public FlowComponentCreateView getFlowComponentCreateView() {
        return flowComponentCreateView;
    }

    @Override
    public SubmitterCreateView getSubmitterCreateView() {
        return submitterCreateView;
    }

    @Override
    public PlaceController getPlaceController() {
        return placeController;
    }

    @Override
    public FlowStoreProxyAsync getFlowStoreProxyAsync() {
        return flowStoreProxyAsync;
    }
}
