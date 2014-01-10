package dk.dbc.dataio.gui.util;

import com.google.gwt.place.shared.PlaceController;
import com.google.web.bindery.event.shared.EventBus;
import com.google.web.bindery.event.shared.SimpleEventBus;
import dk.dbc.dataio.gui.client.proxies.FlowStoreProxy;
import dk.dbc.dataio.gui.client.proxies.FlowStoreProxyAsync;
import dk.dbc.dataio.gui.client.proxies.JavaScriptProjectFetcher;
import dk.dbc.dataio.gui.client.proxies.JavaScriptProjectFetcherAsync;
import dk.dbc.dataio.gui.client.proxies.SinkServiceProxy;
import dk.dbc.dataio.gui.client.proxies.SinkServiceProxyAsync;
import dk.dbc.dataio.gui.client.views.FlowComponentCreateView;
import dk.dbc.dataio.gui.client.views.FlowComponentCreateViewImpl;
import dk.dbc.dataio.gui.client.views.FlowComponentsShowView;
import dk.dbc.dataio.gui.client.views.FlowComponentsShowViewImpl;
import dk.dbc.dataio.gui.client.views.FlowCreateView;
import dk.dbc.dataio.gui.client.views.FlowCreateViewImpl;
import dk.dbc.dataio.gui.client.views.FlowbinderCreateView;
import dk.dbc.dataio.gui.client.views.FlowbinderCreateViewImpl;
import dk.dbc.dataio.gui.client.views.FlowsShowView;
import dk.dbc.dataio.gui.client.views.FlowsShowViewImpl;
import dk.dbc.dataio.gui.client.views.SinkCreateView;
import dk.dbc.dataio.gui.client.views.SinkCreateViewImpl;
import dk.dbc.dataio.gui.client.views.SubmitterCreateView;
import dk.dbc.dataio.gui.client.views.SubmitterCreateViewImpl;
import dk.dbc.dataio.gui.client.views.SubmittersShowView;
import dk.dbc.dataio.gui.client.views.SubmittersShowViewImpl;


public class ClientFactoryImpl implements ClientFactory {
    // Event Bus
    private final EventBus eventBus = new SimpleEventBus();

    // Place Controller
    private final PlaceController placeController = new PlaceController(eventBus);

    // Proxies
    private final FlowStoreProxyAsync flowStoreProxyAsync = FlowStoreProxy.Factory.getAsyncInstance();
    private final JavaScriptProjectFetcherAsync javaScriptProjectFetcher = JavaScriptProjectFetcher.Factory.getAsyncInstance();
    private final SinkServiceProxyAsync sinkServiceProxyAsync = SinkServiceProxy.Factory.getAsyncInstance();

    // Views
    private final FlowCreateView flowCreateView = new FlowCreateViewImpl();
    private final FlowComponentCreateView flowComponentCreateView = new FlowComponentCreateViewImpl();
    private final SubmitterCreateView submitterCreateView = new SubmitterCreateViewImpl();
    private final FlowbinderCreateView flowbinderCreateView = new FlowbinderCreateViewImpl();
    private final SinkCreateView sinkCreateView = new SinkCreateViewImpl();
    private final FlowComponentsShowView flowComponentsShowView = new FlowComponentsShowViewImpl();
    private final FlowsShowView flowsShowView = new FlowsShowViewImpl();
    private final SubmittersShowView submittersShowView = new SubmittersShowViewImpl();


    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    @Override
    public PlaceController getPlaceController() {
        return placeController;
    }

    @Override
    public FlowStoreProxyAsync getFlowStoreProxyAsync() {
        return flowStoreProxyAsync;
    }

    @Override
    public JavaScriptProjectFetcherAsync getJavaScriptProjectFetcherAsync() {
        return javaScriptProjectFetcher;
    }

    @Override
    public SinkServiceProxyAsync getSinkServiceProxyAsync() {
        return sinkServiceProxyAsync;
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
    public FlowbinderCreateView getFlowbinderCreateView() {
        return flowbinderCreateView;
    }

    @Override
    public SinkCreateView getSinkCreateView() {
        return sinkCreateView;
    }

    @Override
    public FlowComponentsShowView getFlowComponentsShowView() {
        return flowComponentsShowView;
    }

    @Override
    public FlowsShowView getFlowsShowView() {
        return flowsShowView;
    }

    @Override
    public SubmittersShowView getSubmittersShowView() {
        return submittersShowView;
    }

}
