package dk.dbc.dataio.gui.util;

import com.google.gwt.place.shared.PlaceController;
import com.google.web.bindery.event.shared.EventBus;
import dk.dbc.dataio.gui.client.proxies.FlowStoreProxyAsync;
import dk.dbc.dataio.gui.client.proxies.JavaScriptProjectFetcherAsync;
import dk.dbc.dataio.gui.client.proxies.SinkServiceProxyAsync;
import dk.dbc.dataio.gui.client.views.FlowComponentCreateView;
import dk.dbc.dataio.gui.client.views.FlowComponentsShowView;
import dk.dbc.dataio.gui.client.views.FlowCreateView;
import dk.dbc.dataio.gui.client.views.FlowbinderCreateView;
import dk.dbc.dataio.gui.client.views.SinkCreateView;
import dk.dbc.dataio.gui.client.views.SubmitterCreateView;

/**
 * Provides access to common objects across the application including:
 * - views as singleton objects, which improves performance since views contain DOM calls which are expensive.
 * - shared event bus.
 * - any RPC proxies.
 */
public interface ClientFactory {
    // Event Bus
    public EventBus getEventBus();
    
    // Place Controller
    public PlaceController getPlaceController();

    // Proxies
    public FlowStoreProxyAsync getFlowStoreProxyAsync();
    public JavaScriptProjectFetcherAsync getJavaScriptProjectFetcherAsync();
    public SinkServiceProxyAsync getSinkServiceProxyAsync();
    
    // Views
    public FlowCreateView getFlowCreateView();
    public FlowComponentCreateView getFlowComponentCreateView();
    public SubmitterCreateView getSubmitterCreateView();
    public FlowbinderCreateView getFlowbinderCreateView();
    public SinkCreateView getSinkCreateView();
    public FlowComponentsShowView getFlowComponentsShowView();
}
