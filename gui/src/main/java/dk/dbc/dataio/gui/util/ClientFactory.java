package dk.dbc.dataio.gui.util;

import com.google.gwt.place.shared.PlaceController;
import com.google.web.bindery.event.shared.EventBus;
import dk.dbc.dataio.gui.client.proxies.FlowStoreProxyAsync;
import dk.dbc.dataio.gui.client.proxies.JavaScriptProjectFetcherAsync;
import dk.dbc.dataio.gui.client.proxies.JobStoreProxyAsync;
import dk.dbc.dataio.gui.client.proxies.SinkServiceProxyAsync;
import dk.dbc.dataio.gui.client.views.FlowComponentCreateView;
import dk.dbc.dataio.gui.client.views.FlowComponentsShowView;
import dk.dbc.dataio.gui.client.views.FlowCreateView;
import dk.dbc.dataio.gui.client.views.FlowbinderCreateView;
import dk.dbc.dataio.gui.client.views.FlowsShowView;
import dk.dbc.dataio.gui.client.views.JobsShowView;
import dk.dbc.dataio.gui.client.views.SinkCreateView;
import dk.dbc.dataio.gui.client.views.SinksShowView;
import dk.dbc.dataio.gui.client.views.SubmitterCreateView;
import dk.dbc.dataio.gui.client.views.SubmittersShowView;

/**
 * Provides access to common objects across the application including:
 * - views as singleton objects, which improves performance since views contain DOM calls which are expensive.
 * - shared event bus.
 * - any RPC proxies.
 */
public interface ClientFactory {
    // Event Bus
    EventBus getEventBus();

    // Place Controller
    PlaceController getPlaceController();

    // Proxies
    FlowStoreProxyAsync getFlowStoreProxyAsync();
    JavaScriptProjectFetcherAsync getJavaScriptProjectFetcherAsync();
    SinkServiceProxyAsync getSinkServiceProxyAsync();
    JobStoreProxyAsync getJobStoreProxyAsync();

    // Views
    FlowCreateView getFlowCreateView();
    FlowComponentCreateView getFlowComponentCreateView();
    SubmitterCreateView getSubmitterCreateView();
    FlowbinderCreateView getFlowbinderCreateView();
    SinkCreateView getSinkCreateView();
    FlowComponentsShowView getFlowComponentsShowView();
    FlowsShowView getFlowsShowView();
    SubmittersShowView getSubmittersShowView();
    JobsShowView getJobsShowView();
    SinksShowView getSinksShowView();
}
